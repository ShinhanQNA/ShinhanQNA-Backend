package com.example.shinhanQnA.service;

import com.example.shinhanQnA.entity.Notice;
import com.example.shinhanQnA.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 공지사항 작성 (관리자 전용)
    @Transactional
    public Notice createNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    // 공지사항 수정 (관리자 전용)
    @Transactional
    public Notice updateNotice(Long id, Notice newNoticeData) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
        notice.setTitle(newNoticeData.getTitle());
        notice.setContent(newNoticeData.getContent());
        return noticeRepository.save(notice);
    }

    // 공지사항 삭제 (관리자 전용)
    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new RuntimeException("공지사항을 찾을 수 없습니다.");
        }
        noticeRepository.deleteById(id);
    }

    // 공지사항 전체 조회 (모든 사용자 가능)
    public List<Notice> getAllNotices() {
        // createdAt 필드를 기준으로 최신순 내림차순 정렬하여 조회
        return noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // 공지사항 단일 조회 (모든 사용자 가능)
    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다."));
    }
}
