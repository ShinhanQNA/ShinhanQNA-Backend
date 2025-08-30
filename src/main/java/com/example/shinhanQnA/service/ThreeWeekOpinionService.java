package com.example.shinhanQnA.service;

import com.example.shinhanQnA.DTO.GroupIdByMonthDTO;
import com.example.shinhanQnA.entity.Board;
import com.example.shinhanQnA.entity.ThreeWeekOpinion;
import com.example.shinhanQnA.entity.ThreeWeekOpinionGroup;
import com.example.shinhanQnA.repository.BoardRepository;
import com.example.shinhanQnA.repository.ThreeWeekOpinionGroupRepository;
import com.example.shinhanQnA.repository.ThreeWeekOpinionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shinhanQnA.DTO.ThreeWeekOpinionDTO;
import com.example.shinhanQnA.DTO.ThreeWeekOpinionGroupDTO;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThreeWeekOpinionService {

    private final BoardRepository boardRepository;
    private final ThreeWeekOpinionGroupRepository groupRepository;
    private final ThreeWeekOpinionRepository opinionRepository;

    /**
     * 해당 년/월 3주차(1-21일) 게시글 중 좋아요 1개 이상인 게시글 선정 및 저장
     * 그룹이 없으면 생성, 있으면 기존 그룹 사용
     */
    @Transactional
    public ThreeWeekOpinionGroup createOrGetGroup(Integer year, Integer month) {
        return groupRepository.findBySelectedYearAndSelectedMonth(year, month)
                .orElseGet(() -> {
                    ThreeWeekOpinionGroup newGroup = ThreeWeekOpinionGroup.builder()
                            .selectedYear(year)
                            .selectedMonth(month)
                            .responseStatus("응답 대기")
                            .build();
                    return groupRepository.save(newGroup);
                });
    }

    @Transactional
    public List<ThreeWeekOpinion> saveOpinionsForGroup(ThreeWeekOpinionGroup group) {

        LocalDate startDate = LocalDate.of(group.getSelectedYear(), group.getSelectedMonth(), 1);
        LocalDate endDate = LocalDate.of(group.getSelectedYear(), group.getSelectedMonth(), 21);

        List<Board> boards = boardRepository.findAll().stream()
                .filter(board -> {
                    LocalDate boardDate = board.getDate().toLocalDate();
                    return !boardDate.isBefore(startDate) && !boardDate.isAfter(endDate)
                            && board.getLikes() != null && board.getLikes() >= 1;
                })
                .collect(Collectors.toList());

        // 중복 저장 방지
        List<ThreeWeekOpinion> savedOpinions = boards.stream()
                .filter(board -> !opinionRepository.existsByGroupAndBoard_PostId(group, board.getPostId()))
                .map(board -> ThreeWeekOpinion.builder()
                        .group(group)
                        .board(board)
                        .build())
                .map(opinionRepository::save)
                .collect(Collectors.toList());

        return savedOpinions;
    }

    /**
     * 그룹 전체 의견 조회 (정렬: 최신순|공감 순)
     */
    public List<ThreeWeekOpinion> getOpinionsByGroup(Integer year, Integer month, String sortType) {
        ThreeWeekOpinionGroup group = groupRepository.findBySelectedYearAndSelectedMonth(year, month)
                .orElseThrow(() -> new RuntimeException("해당 월의 선정 그룹이 없습니다."));

        Sort sort;
        if ("likes".equalsIgnoreCase(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "board.likes");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "board.date");
        }

        return opinionRepository.findByGroup(group, sort);
    }

    /**
     * 월별 선정 그룹의 응답 상태 변경
     */
    @Transactional
    public ThreeWeekOpinionGroup updateGroupResponseStatus(Long groupId, String newStatus) {
        ThreeWeekOpinionGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("선택하신 그룹이 없습니다."));
        group.setResponseStatus(newStatus);
        return groupRepository.save(group);
    }

    public ThreeWeekOpinionGroupDTO getGroupWithOpinionsAsDTO(Long groupId, String sortType) {
        ThreeWeekOpinionGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("그룹을 찾을 수 없습니다."));

        Sort sort;
        if ("likes".equalsIgnoreCase(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "board.likes");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "board.date");
        }

        List<ThreeWeekOpinion> opinions = opinionRepository.findByGroup(group, sort);

        List<ThreeWeekOpinionDTO> opinionDTOs = opinions.stream()
                .map(opinion -> ThreeWeekOpinionDTO.builder()
                        .id(opinion.getId())
                        .postId(opinion.getBoard().getPostId())
                        .title(opinion.getBoard().getTitle())
                        .content(opinion.getBoard().getContent())
                        .likes(opinion.getBoard().getLikes())
                        .date(opinion.getBoard().getDate())
                        .imagePath(opinion.getBoard().getImagePath())
                        .createdAt(opinion.getCreatedAt())
                        .build())
                .toList();

        return ThreeWeekOpinionGroupDTO.builder()
                .id(group.getId())
                .selectedYear(group.getSelectedYear())
                .selectedMonth(group.getSelectedMonth())
                .responseStatus(group.getResponseStatus())
                .createdAt(group.getCreatedAt())
                .opinions(opinionDTOs)
                .build();
    }

    public ThreeWeekOpinionGroup getGroupByYearMonth(int year, int month) {
        return groupRepository.findBySelectedYearAndSelectedMonth(year, month)
                .orElse(null);
    }

    public List<GroupIdByMonthDTO> getGroupIdsByYear(int year) {
        List<ThreeWeekOpinionGroup> groups = groupRepository.findAllBySelectedYear(year);
        return groups.stream()
                .map(group -> new GroupIdByMonthDTO(
                        group.getSelectedMonth(),
                        group.getId(),
                        group.getResponseStatus()
                ))
                .sorted((a, b) -> a.getSelectedMonth().compareTo(b.getSelectedMonth())) // 월 오름차순 정렬
                .toList();
    }

    @Transactional
    public ThreeWeekOpinionGroup createOrGetFullMonthGroup(Integer year, Integer month) {
        // 그룹이 이미 존재하면 가져오고, 없으면 새로 생성
        ThreeWeekOpinionGroup group = groupRepository.findBySelectedYearAndSelectedMonth(year, month)
                .orElseGet(() -> {
                    ThreeWeekOpinionGroup newGroup = ThreeWeekOpinionGroup.builder()
                            .selectedYear(year)
                            .selectedMonth(month)
                            .responseStatus("응답 대기")
                            .build();
                    return groupRepository.save(newGroup);
                });

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Board> boards = boardRepository.findAll().stream()
                .filter(board -> {
                    LocalDate boardDate = board.getDate().toLocalDate();
                    return !boardDate.isBefore(startDate) && !boardDate.isAfter(endDate)
                            && board.getLikes() != null && board.getLikes() >= 1;
                })
                .toList();

        // 중복 저장 방지
        List<ThreeWeekOpinion> savedOpinions = boards.stream()
                .filter(board -> !opinionRepository.existsByGroupAndBoard_PostId(group, board.getPostId()))
                .map(board -> ThreeWeekOpinion.builder()
                        .group(group)
                        .board(board)
                        .build())
                .map(opinionRepository::save)
                .toList();

        return group;
    }



}

