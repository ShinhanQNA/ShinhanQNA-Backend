package com.example.shinhanQnA.service;

import com.example.shinhanQnA.entity.Answer;
import com.example.shinhanQnA.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;

    // 답변 작성 (관리자 전용)
    @Transactional
    public Answer createAnswer(Answer answer) {
        return answerRepository.save(answer);
    }

    // 답변 수정 (관리자 전용)
    @Transactional
    public Answer updateAnswer(Long id, Answer newAnswerData) {
        Answer answer = answerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
        answer.setTitle(newAnswerData.getTitle());
        answer.setContent(newAnswerData.getContent());
        return answerRepository.save(answer);
    }

    // 답변 삭제 (관리자 전용)
    @Transactional
    public void deleteAnswer(Long id) {
        if (!answerRepository.existsById(id)) {
            throw new RuntimeException("답변을 찾을 수 없습니다.");
        }
        answerRepository.deleteById(id);
    }

    // 답변 전체 조회 (모든 사용자 접근 가능)
    public List<Answer> getAllAnswers() {
        // Answer 엔티티의 createdAt 필드를 기준으로 최신 순 조회
        return answerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // 답변 단일 조회 (모든 사용자 접근 가능)
    public Answer getAnswerById(Long id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("답변을 찾을 수 없습니다."));
    }
}

