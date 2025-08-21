package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.BoardResponseDTO;
import com.example.shinhanQnA.entity.Board;
import com.example.shinhanQnA.entity.LikeEntity;
import com.example.shinhanQnA.entity.User;
import com.example.shinhanQnA.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.shinhanQnA.entity.userwarning;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final LikeRepository likeRepository;
    private final S3Uploader s3Uploader; // 이미지 업로드용
    private final BoardReportRepository boardReportRepository;
    private final UserWarningRepository userWarningRepository;

    // 게시글 작성
    public Board writeBoard(String title, String content, MultipartFile imageFile, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 사용자 이메일입니다."));

        String userYear = user.getYear();

        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imagePath = s3Uploader.upload(imageFile, "board-images");
        }

        Board board = Board.builder()
                .title(title)
                .content(content)
                .imagePath(imagePath)
                .email(user.getEmail())
                .year(userYear)
                .likes(0)
                .status("응답 대기")
                .date(LocalDateTime.now())
                .build();

        return boardRepository.save(board);
    }

    // 전체 게시글 조회 (작성자 상태 + 신고 회수 포함, 이미지 제외)
    public List<BoardResponseDTO> findAllBoardsWithUserStatus() {
        List<Board> boards = boardRepository.findAllByOrderByDateDesc();

        return boards.stream()
                .map(board -> {
                    User user = userRepository.findByEmail(board.getEmail()).orElse(null);
                    Long reportCount = boardReportRepository.countByPostId(board.getPostId());
                    return BoardResponseDTO.builder()
                            .postId(board.getPostId())
                            .title(board.getTitle())
                            .content(board.getContent())
                            .likes(board.getLikes())
                            .date(board.getDate())
                            .status(board.getStatus())
                            .reportCount(reportCount)
                            .build();
                })
                .toList();
    }

    // 게시글 수정
    @Transactional
    public Board updateBoard(Integer postId, String title, String content, MultipartFile imageFile, String email) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!board.getEmail().equals(email)) {
            throw new RuntimeException("게시글 수정 권한이 없습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 사용자 이메일입니다."));
        String userYear = user.getYear();

        board.setTitle(title);
        board.setContent(content);
        board.setYear(userYear);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imagePath = s3Uploader.upload(imageFile, "board-images");
            board.setImagePath(imagePath);
        }

        return board;
    }

    public Optional<Board> findBoardById(Integer postId) {
        return boardRepository.findById(postId);
    }

    public List<Board> findAllBoards() {
        return boardRepository.findAll();
    }

    public List<Board> findBoardsByStatus(String status) {
        return boardRepository.findByStatus(status);
    }

    // 페이징 및 정렬된 게시글 목록 조회
    public List<Board> findBoardsWithPagingAndSort(int size, String sortType) {
        Sort sort;

        switch (sortType.toLowerCase()) {
            case "year":
                sort = Sort.by(Sort.Direction.ASC, "year");
                break;
            case "day":
                sort = Sort.by(Sort.Direction.DESC, "date");
                break;
            case "like":
                sort = Sort.by(Sort.Direction.DESC, "likes");
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "date");
                break;
        }

        Pageable pageable = PageRequest.of(0, size, sort);
        Page<Board> boardPage = boardRepository.findAll(pageable);
        return boardPage.getContent();
    }

    @Transactional
    public Board updateBoard(Integer postId, Board updated) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        board.setTitle(updated.getTitle());
        board.setContent(updated.getContent());
        board.setImagePath(updated.getImagePath());
        if (updated.getStatus() != null) {
            board.setStatus(updated.getStatus());
        }
        return board;
    }

    public void deleteBoard(Integer postId) {
        boardRepository.deleteById(postId);
    }

    // 공감 증가 (한 계정당 1회만)
    @Transactional
    public int likeBoard(Integer postId, String userEmail) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (likeRepository.findByPostIdAndUserEmail(postId, userEmail).isPresent()) {
            throw new RuntimeException("이미 공감한 게시글입니다.");
        }

        LikeEntity likeEntity = LikeEntity.builder()
                .postId(postId)
                .userEmail(userEmail)
                .build();
        likeRepository.save(likeEntity);

        board.setLikes(board.getLikes() + 1);
        return board.getLikes();
    }

    // 공감 취소
    @Transactional
    public int unlikeBoard(Integer postId, String userEmail) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        LikeEntity likeEntity = likeRepository.findByPostIdAndUserEmail(postId, userEmail)
                .orElseThrow(() -> new RuntimeException("공감하지 않은 게시글입니다."));

        likeRepository.delete(likeEntity);

        int likes = board.getLikes();
        board.setLikes(Math.max(0, likes - 1));
        return board.getLikes();
    }

    // 상태 변경
    @Transactional
    public Board changeStatus(Integer postId, String status) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        board.setStatus(status);
        return board;
    }

    public List<BoardResponseDTO> findAllBoardsWithUserWarning() {
        List<Board> boards = boardRepository.findAllByOrderByDateDesc();

        return boards.stream()
                .map(board -> {
                    // 1. 게시글별 신고 카운트
                    Long reportCount = boardReportRepository.countByPostId(board.getPostId());
                    // 2. 게시글 작성자의 최신 userwarning 상태 찾기
                    String writerEmail = board.getEmail();
                    String warningStatus = userWarningRepository.findTopByEmailOrderByWarningDateDesc(writerEmail)
                            .map(userWarning -> userWarning.getStatus())
                            .orElse("없음"); // warning 존재 X

                    return BoardResponseDTO.builder()
                            .postId(board.getPostId())
                            .title(board.getTitle())
                            .content(board.getContent())
                            .likes(board.getLikes())
                            .date(board.getDate())
                            .status(board.getStatus())
                            .reportCount(reportCount)
                            .warningStatus(warningStatus)
                            .build();
                })
                .toList();
    }

    // 단일 게시글 DTO (작성자 상태, 신고수, 이메일, 이미지 포함)
    public Optional<BoardResponseDTO> findBoardResponseById(Integer postId) {
        return boardRepository.findById(postId)
                .map(board -> {
                    Long reportCount = boardReportRepository.countByPostId(board.getPostId());
                    String warningStatus = userWarningRepository
                            .findTopByEmailOrderByWarningDateDesc(board.getEmail())
                            .map(userwarning::getStatus)
                            .orElse("없음");
                    return BoardResponseDTO.builder()
                            .postId(board.getPostId())
                            .title(board.getTitle())
                            .content(board.getContent())
                            .likes(board.getLikes())
                            .date(board.getDate())
                            .status(board.getStatus())
                            .reportCount(reportCount)
                            .warningStatus(warningStatus)
                            .writerEmail(board.getEmail())
                            .imagePath(board.getImagePath())
                            .build();
                });
    }

}
