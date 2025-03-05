package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.dto.question.SaveQuestionRequest;
import com.buildup.nextQuestion.dto.question.SearchQuestionByMemberResponse;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.dto.question.QuestionUpdateRequest;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {


    private final QuestionRepository questionRepository;
    private final QuestionInfoByMemberRepository questionInfoByMemberRepository;
    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final LocalMemberRepository localMemberRepository;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final WorkBookRepository workBookRepository;

    //생성된 문제 리스트 저장
    @Transactional
    public List<String> saveAll(JsonNode jsonNode) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode questionsNode = jsonNode.get("questions");
        List<String> encryptedQeustionIds = new ArrayList<>();
        if (questionsNode != null && questionsNode.isArray()) {

            List<Question> questions = objectMapper.readValue(
                    questionsNode.toString(),
                    new TypeReference<List<Question>>() {
                    }
            );
            List<Question> createdQuestions = questionRepository.saveAll(questions);

            for (Question createdQuestion : createdQuestions) {
                String encryptedQuestionId = encryptionService.encryptPrimaryKey(createdQuestion.getId());
                encryptedQeustionIds.add(encryptedQuestionId);

            }
        }

        return encryptedQeustionIds;
    }

    @Transactional
    public void saveQuestion (String token, SaveQuestionRequest saveQuestionRequest) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getMember();


        WorkBookInfo workBookInfo = workBookInfoRepository.findById(
                encryptionService.decryptPrimaryKey(
                        saveQuestionRequest.getEncryptedWorkBookInfoId()))
                .get();

        // 회원 문제집 저장
        for (String encryptedQeustionId : saveQuestionRequest.getEncryptedQuestionIds()) {
            Question question = questionRepository.findById(
                    encryptionService.decryptPrimaryKey(encryptedQeustionId))
                    .get();

            WorkBook workBook = new WorkBook(question, workBookInfo);
            QuestionInfoByMember questionInfoByMember = new QuestionInfoByMember(member,question);
            questionInfoByMemberRepository.save(questionInfoByMember);
            workBookRepository.save(workBook);
        }
    }

    @Transactional
    public List<SearchQuestionByMemberResponse> searchQuestionByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getMember();

        List<QuestionInfoByMember> questionInfos = questionInfoByMemberRepository.findAllByMemberId(member.getId());

        List<SearchQuestionByMemberResponse> response = new ArrayList<>();
        for (QuestionInfoByMember questionInfo : questionInfos) {
            Question question = questionInfo.getQuestion();
            if (!questionInfo.getDel()) {
                SearchQuestionByMemberResponse searchQuestionByMemberResponse = new SearchQuestionByMemberResponse();

                searchQuestionByMemberResponse.setEncryptedQuestionInfoId(
                        encryptionService.encryptPrimaryKey(questionInfo.getId())
                );
                searchQuestionByMemberResponse.setName(question.getName());
                searchQuestionByMemberResponse.setType(question.getType());
                searchQuestionByMemberResponse.setAnswer(question.getAnswer());
                searchQuestionByMemberResponse.setOpt(question.getOption());
                searchQuestionByMemberResponse.setCreateTime(question.getCreateTime());
                searchQuestionByMemberResponse.setRecentSolveTime(questionInfo.getRecentSolveTime());

                response.add(searchQuestionByMemberResponse);
            }
        }
        return response;
    }

    @Transactional
    public void deleteQuestion(String token, List<String> encryptedQuestionInfoIds) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        if (encryptedQuestionInfoIds == null || encryptedQuestionInfoIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 문제가 없습니다.");
        }

        for (String encryptedQuestionInfoId : encryptedQuestionInfoIds) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);


            QuestionInfoByMember questionInfo = questionInfoByMemberRepository.findById(questionInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 문제를 찾을 수 없습니다."));

            // 해당 사용자의 문제인지 검증 (소유자가 아니면 예외 발생)
            if (!questionInfo.getMember().getId().equals(member.getId())) {
                throw new IllegalAccessException("해당 문제를 삭제할 권한이 없습니다.");
            }

            // 삭제 처리
            questionInfo.setDel(true);
        }
    }

}

