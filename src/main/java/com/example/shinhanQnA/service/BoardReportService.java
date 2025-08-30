package com.example.shinhanQnA.service;

import com.example.shinhanQnA.entity.BoardReport;
import com.example.shinhanQnA.repository.BoardReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardReportService {

    private final BoardReportRepository boardReportRepository;

    @Transactional
    public BoardReport reportBoard(Integer postId, String reporterEmail, String reportReason) {
        // 동일 게시글에 대해 동일 신고자가 이미 신고했는지 확인
        boolean exists = boardReportRepository.existsByPostIdAndReporterEmail(postId, reporterEmail);
        if (exists) {
            throw new RuntimeException("이미 해당 게시글을 신고하셨습니다.");
        }

        BoardReport report = BoardReport.builder()
                .postId(postId)
                .reporterEmail(reporterEmail)
                .reportReason(reportReason)
                .reportDate(LocalDateTime.now())
                .resolved(false)
                .build();
        return boardReportRepository.save(report);
    }

    public List<BoardReport> getAllReports() {
        return boardReportRepository.findAll();
    }

    @Transactional
    public BoardReport updateResolvedStatus(Long reportId, boolean resolved) {
        BoardReport report = boardReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("해당 신고 내역을 찾을 수 없습니다."));
        report.setResolved(resolved);
        return boardReportRepository.save(report);
    }
}
