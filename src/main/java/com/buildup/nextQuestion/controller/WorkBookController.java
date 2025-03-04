package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.workBook.CreateWorkBookRequest;
import com.buildup.nextQuestion.dto.workBook.GetWorkBookInfoResponse;
import com.buildup.nextQuestion.service.WorkBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WorkBookController {

    private final WorkBookService workBookService;


    @PostMapping("public/workBook/create")
    public ResponseEntity<String> createWorkBook(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateWorkBookRequest request
    ){
        try{
            workBookService.createWorkBook(token, request);
            return ResponseEntity.ok("문제집이 성공적으로 생성되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("public/workBook/search")
    public ResponseEntity<?> getWorkBookInfo(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<GetWorkBookInfoResponse> workBookInfos = workBookService.getWorkBookInfo(token);
            return ResponseEntity.ok(workBookInfos);
        } catch (Exception e) {
            // 예외 메시지를 JSON 형태로 반환
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "워크북 정보를 가져오는 중 오류 발생" + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

}
