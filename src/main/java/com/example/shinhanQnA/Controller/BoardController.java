package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.BoardResponseDTO;
import com.example.shinhanQnA.entity.Board;
import com.example.shinhanQnA.entity.BoardReport;
import com.example.shinhanQnA.service.BoardReportService;
import com.example.shinhanQnA.service.BoardService;
import com.example.shinhanQnA.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BoardReportService boardReportService;

    private String validateAndExtractEmail(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 올바르지 않습니다. Bearer 스킴 필요");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        return jwtTokenProvider.getEmailFromToken(token);
    }

    // 게시글 작성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> writeBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            String email = validateAndExtractEmail(authorizationHeader);
            Board savedBoard = boardService.writeBoard(title, content, imageFile, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 게시글 수정
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            String email = validateAndExtractEmail(authorizationHeader);
            Board updatedBoard = boardService.updateBoard(postId, title, content, imageFile, email);
            return ResponseEntity.ok(updatedBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 단일 조회 (DTO 반환, 작성자 이메일+이미지 경로 포함)
    @GetMapping("/{postId}")
    public ResponseEntity<?> getBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId) {

        try {
            validateAndExtractEmail(authorizationHeader);
            return boardService.findBoardResponseById(postId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 전체 조회 (이미지 제외, 작성자 상태 + 신고 횟수 포함)
    @GetMapping
    public ResponseEntity<?> getAllBoards(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            validateAndExtractEmail(authorizationHeader);
            return ResponseEntity.ok(boardService.findAllBoardsWithUserWarning());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }
    // 상태별 조회
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getBoardsByStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String status) {

        try {
            validateAndExtractEmail(authorizationHeader);
            return ResponseEntity.ok(boardService.findBoardsByStatus(status));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deleteBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId) {

        try {
            validateAndExtractEmail(authorizationHeader);
            boardService.deleteBoard(postId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 페이징 & 정렬 조회
    @GetMapping("/search")
    public ResponseEntity<?> getBoardsWithPagingAndSort(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "day") String sort) {

        try {
            validateAndExtractEmail(authorizationHeader);

            if (size <= 0) {
                size = 10;
            }

            List<Board> boards = boardService.findBoardsWithPagingAndSort(size, sort);
            return ResponseEntity.ok(boards);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "토큰 인증 실패", "message", e.getMessage()));
        }
    }

    // 공감 증가 API (POST /boards/{postId}/like)
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likeBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId) {

        try {
            String userEmail = validateAndExtractEmail(authorizationHeader);
            int likes = boardService.likeBoard(postId, userEmail);
            return ResponseEntity.ok(Map.of("likes", likes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "공감 처리 실패", "message", e.getMessage()));
        }
    }

    // 공감 취소 API (POST /boards/{postId}/unlike)
    @PostMapping("/{postId}/unlike")
    public ResponseEntity<?> unlikeBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId) {

        try {
            String userEmail = validateAndExtractEmail(authorizationHeader);
            int likes = boardService.unlikeBoard(postId, userEmail);
            return ResponseEntity.ok(Map.of("likes", likes));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "공감 취소 실패", "message", e.getMessage()));
        }
    }

    // 상태 변경 API (PUT /boards/{postId}/status)
    @PutMapping("/{postId}/status")
    public ResponseEntity<?> changeStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId,
            @RequestBody Map<String, String> payload) {

        try {
            validateAndExtractEmail(authorizationHeader);

            String newStatus = payload.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "잘못된 요청", "message", "상태값을 입력해주세요."));
            }

            Board board = boardService.changeStatus(postId, newStatus);
            return ResponseEntity.ok(board);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "상태 변경 실패", "message", e.getMessage()));
        }
    }

    // 게시글 신고 API
    @PostMapping("/{postId}/report")
    public ResponseEntity<?> reportBoard(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer postId,
            @RequestBody Map<String, String> payload) {

        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Authorization 헤더가 올바르지 않습니다."));
            }
            String token = authorizationHeader.substring(7);
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 토큰입니다."));
            }
            String reporterEmail = jwtTokenProvider.getEmailFromToken(token);
            String reportReason = payload.get("reportReason");

            BoardReport report = boardReportService.reportBoard(postId, reporterEmail, reportReason);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "신고 실패", "message", e.getMessage()));
        }
    }
}
