package com.example.shinhanQnA.Controller;

import com.example.shinhanQnA.DTO.GroupIdByMonthDTO;
import com.example.shinhanQnA.DTO.ThreeWeekOpinionGroupDTO;
import com.example.shinhanQnA.entity.ThreeWeekOpinion;
import com.example.shinhanQnA.entity.ThreeWeekOpinionGroup;
import com.example.shinhanQnA.service.AdminService;
import com.example.shinhanQnA.service.JwtTokenProvider;
import com.example.shinhanQnA.service.ThreeWeekOpinionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/three-week-opinions")
@RequiredArgsConstructor
public class ThreeWeekOpinionController {

    private final ThreeWeekOpinionService service;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminService adminService;

    // (1) 토큰에서 이메일 추출 및 검증
    private String validateAndExtractEmail(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 올바르지 않습니다.");
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        return jwtTokenProvider.getEmailFromToken(token);
    }

    // (2) 관리자 권한 검증
    private boolean isAdmin(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        return adminService.isValidAdminToken(token);
    }

    /** 월별 선정 그룹 생성 및 게시글 저장
     *  POST /three-week-opinions/select?year=YYYY&month=MM
     *  유저 토큰 or 관리자 토큰 모두 가능
     */
    @PostMapping("/select")
    public ResponseEntity<?> selectGroupAndSave(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam int year,
            @RequestParam int month) {

        // 토큰 검증(유저/관리자 모두 허용)
        validateAndExtractEmail(authorizationHeader);

        ThreeWeekOpinionGroup group = service.createOrGetGroup(year, month);
        List<ThreeWeekOpinion> opinions = service.saveOpinionsForGroup(group);

        return ResponseEntity.ok(Map.of(
                "groupId", group.getId(),
                "savedCount", opinions.size(),
                "responseStatus", group.getResponseStatus()
        ));
    }

    /** 그룹별 선정 게시글 목록 조회
     *  GET /three-week-opinions?year=YYYY&month=MM&sort=date|likes
     *  유저 또는 관리자 모두 가능
     */
    @GetMapping
    public ResponseEntity<?> getOpinionsByGroup(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "date") String sort) {

        validateAndExtractEmail(authorizationHeader);

        List<ThreeWeekOpinion> opinions = service.getOpinionsByGroup(year, month, sort);
        return ResponseEntity.ok(opinions);
    }

    /** 그룹 단위 응답 상태 변경
     *  PUT /three-week-opinions/group/{groupId}/status
     *  관리자 토큰만 허용
     */
    @PutMapping("/group/{groupId}/status")
    public ResponseEntity<?> updateGroupStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @RequestBody Map<String, String> body) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "상태 값을 입력하세요."));
        }

        ThreeWeekOpinionGroup updatedGroup = service.updateGroupResponseStatus(groupId, newStatus);

        return ResponseEntity.ok(Map.of("status", updatedGroup.getResponseStatus()));
    }

    /** 그룹 상세 정보 DTO 반환
     *  GET /three-week-opinions/group/{groupId}?sort=date|likes
     *  유저, 관리자 모두 가능
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupDetail(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "date") String sort) {

        validateAndExtractEmail(authorizationHeader);

        ThreeWeekOpinionGroupDTO dto = service.getGroupWithOpinionsAsDTO(groupId, sort);
        return ResponseEntity.ok(dto);
    }


    @GetMapping("/groups/ids")
    public ResponseEntity<?> getGroupIdsByYear(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam int year) {

        validateAndExtractEmail(authorizationHeader);

        List<GroupIdByMonthDTO> dtos = service.getGroupIdsByYear(year);
        return ResponseEntity.ok(dtos);
    }

    /** 테스트용 - 수동으로 특정 년/월에 대해 3주 조회 게시글 생성
     *  POST /three-week-opinions/manual-create?year=YYYY&month=MM
     *  관리자 토큰만 허용
     */
    @PostMapping("/manual-create")
    public ResponseEntity<?> manualCreateThreeWeekOpinion(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam int year,
            @RequestParam int month) {

        if (!isAdmin(authorizationHeader)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }

        try {
            ThreeWeekOpinionGroup group = service.createOrGetGroup(year, month);
            List<ThreeWeekOpinion> opinions = service.saveOpinionsForGroup(group);

            return ResponseEntity.ok(Map.of(
                    "message", "3주 조회 게시글이 수동으로 생성되었습니다.",
                    "year", year,
                    "month", month,
                    "groupId", group.getId(),
                    "selectedCount", opinions.size(),
                    "responseStatus", group.getResponseStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

}
