package com.example.shinhanQnA.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ThreeWeekOpinionSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ThreeWeekOpinionSchedulerService.class);

    private final ThreeWeekOpinionService threeWeekOpinionService;

    /**
     * 매월 21일 자정(00:00)에 자동으로 3주 조회 게시글 생성
     * cron 표현식: "0 0 0 21 * ?" (초 분 시 일 월 요일)
     * - 초: 0
     * - 분: 0
     * - 시: 0 (자정)
     * - 일: 21 (매월 21일)
     * - 월: * (모든 월)
     * - 요일: ? (상관없음)
     */
    @Scheduled(cron = "0 0 0 21 * ?")
    public void autoCreateThreeWeekOpinion() {
        try {
            LocalDate today = LocalDate.now();
            int year = today.getYear();
            int month = today.getMonthValue();

            logger.info("3주 조회 게시글 자동 생성 시작: {}년 {}월", year, month);

            // 해당 월의 3주 조회 그룹이 이미 존재하는지 확인
            var existingGroup = threeWeekOpinionService.getGroupByYearMonth(year, month);
            if (existingGroup != null) {
                logger.info("{}년 {}월 3주 조회 그룹이 이미 존재합니다. 생성을 건너뜁니다.", year, month);
                return;
            }

            // 3주 조회 그룹 생성 또는 조회
            var group = threeWeekOpinionService.createOrGetGroup(year, month);

            // 해당 그룹에 3주간의 게시글 중 좋아요 1개 이상인 게시글들을 저장
            var savedOpinions = threeWeekOpinionService.saveOpinionsForGroup(group);

            logger.info("3주 조회 게시글 자동 생성 완료: {}년 {}월, 총 {}개 게시글 선정",
                       year, month, savedOpinions.size());

        } catch (Exception e) {
            logger.error("3주 조회 게시글 자동 생성 중 오류 발생", e);
        }
    }

    /**
     * 테스트용 메서드 - 수동으로 특정 년/월에 대해 3주 조회 게시글 생성
     * 실제 운영에서는 사용하지 않고, 개발/테스트 시에만 사용
     */
    public void manualCreateThreeWeekOpinion(int year, int month) {
        try {
            logger.info("3주 조회 게시글 수동 생성 시작: {}년 {}월", year, month);

            var group = threeWeekOpinionService.createOrGetGroup(year, month);
            var savedOpinions = threeWeekOpinionService.saveOpinionsForGroup(group);

            logger.info("3주 조회 게시글 수동 생성 완료: {}년 {}월, 총 {}개 게시글 선정",
                       year, month, savedOpinions.size());

        } catch (Exception e) {
            logger.error("3주 조회 게시글 수동 생성 중 오류 발생", e);
            throw new RuntimeException("3주 조회 게시글 생성에 실패했습니다: " + e.getMessage());
        }
    }
}
