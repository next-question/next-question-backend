package com.buildup.nextQuestion.controller;

import com.buildup.nextQuestion.dto.wrongNote.FindQuestionsByWrongNoteRequest;
import com.buildup.nextQuestion.dto.wrongNote.FindQuestionsByWrongNoteResponse;
import com.buildup.nextQuestion.dto.wrongNote.FindWrongNoteRequest;
import com.buildup.nextQuestion.dto.wrongNote.FindWrongNoteResponse;
import com.buildup.nextQuestion.service.WrongNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;
    @PostMapping("/solving/wrong/search")
    public ResponseEntity<?> searchWrongNote(
            @RequestHeader("Authorization") String token,
            @RequestBody FindWrongNoteRequest request
    ) throws Exception {
        FindWrongNoteResponse response = wrongNoteService.findWrongNote(token, request);
        return ResponseEntity.ok(response);
    }
}
