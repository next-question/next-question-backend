package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.dto.question.MoveQuestionRequest;
import com.buildup.nextQuestion.dto.question.SaveQuestionRequest;
import com.buildup.nextQuestion.dto.question.FindQuestionByMemberResponse;
import com.buildup.nextQuestion.exception.DuplicateResourceException;
import com.buildup.nextQuestion.exception.AccessDeniedException;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {


    private final QuestionInfoRepository questionInfoRepository;
    private final QuestionRepository questionRepository;
    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;
    private final LocalMemberRepository localMemberRepository;
    private final WorkBookRepository workBookRepository;
    private final WorkBookInfoRepository workBookInfoRepository;

    //생성된 문제 리스트 저장
    @Transactional
    public List<String> saveAll(JsonNode jsonNode) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode questionsNode = jsonNode.get("questions");
        List<String> encryptedQeustionIds = new ArrayList<>();
        if (questionsNode != null && questionsNode.isArray()) {

            List<QuestionInfo> questionInfos = objectMapper.readValue(
                    questionsNode.toString(),
                    new TypeReference<List<QuestionInfo>>() {
                    }
            );
            List<QuestionInfo> createdQuestionInfos = questionInfoRepository.saveAll(questionInfos);

            for (QuestionInfo createdQuestionInfo : createdQuestionInfos) {
                String encryptedQuestionId = encryptionService.encryptPrimaryKey(createdQuestionInfo.getId());
                encryptedQeustionIds.add(encryptedQuestionId);

            }
        }

        return encryptedQeustionIds;
    }

    @Transactional
    public void saveQuestion(String token, SaveQuestionRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        WorkBook workBook = workBookRepository.findById(workBookId)
                .orElseThrow(() -> new NoSuchElementException("해당 문제집을 찾을 수 없습니다."));

        // 회원 문제집 저장
        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            QuestionInfo questionInfo = questionInfoRepository.findById(questionInfoId)
                    .orElseThrow(() -> new NoSuchElementException("해당 문제가 존재하지 않습니다."));

            // 문제집에 동일한 문제가 이미 존재하는지 확인
            boolean isDuplicate = workBookInfoRepository.existsByWorkBookIdAndQuestionInfoId(workBook.getId(), questionInfoId);
            if (isDuplicate) {
                throw new DuplicateResourceException("문제집에 이미 동일한 문제가 존재합니다.");
            }

            WorkBookInfo workBookInfo = new WorkBookInfo(questionInfo, workBook);
            Question question = new Question(member, questionInfo);
            questionRepository.save(question);
            workBookInfoRepository.save(workBookInfo);
        }
    }

    @Transactional
    public List<FindQuestionByMemberResponse> searchQuestionByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).orElseThrow(() -> new NoSuchElementException("해당 멤버를 찾을 수 없습니다.")).getMember();

        List<Question> questionInfos = questionRepository.findAllByMemberId(member.getId());

        List<FindQuestionByMemberResponse> response = new ArrayList<>();
        for (Question questionInfo : questionInfos) {
            QuestionInfo question = questionInfo.getQuestionInfo();
            if (!questionInfo.getDel()) {
                FindQuestionByMemberResponse findQuestionByMemberResponse = new FindQuestionByMemberResponse();

                findQuestionByMemberResponse.setEncryptedQuestionId(
                        encryptionService.encryptPrimaryKey(questionInfo.getId())
                );
                findQuestionByMemberResponse.setName(question.getName());
                findQuestionByMemberResponse.setType(question.getType());
                findQuestionByMemberResponse.setAnswer(question.getAnswer());
                findQuestionByMemberResponse.setOpt(question.getOption());
                findQuestionByMemberResponse.setCreateTime(question.getCreateTime());
                findQuestionByMemberResponse.setRecentSolveTime(questionInfo.getRecentSolveTime());

                response.add(findQuestionByMemberResponse);
            }
        }
        return response;
    }

    @Transactional
    public void deleteQuestion(String token, List<String> encryptedQuestionInfoIds) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        if (encryptedQuestionInfoIds == null || encryptedQuestionInfoIds.isEmpty()) {
            throw new NoSuchElementException("삭제할 문제가 없습니다.");
        }

        for (String encryptedQuestionInfoId : encryptedQuestionInfoIds) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);


            Question questionInfo = questionRepository.findById(questionInfoId)
                    .orElseThrow(() -> new NoSuchElementException("해당 문제를 찾을 수 없습니다."));

            // 해당 사용자의 문제인지 검증 (소유자가 아니면 예외 발생)
            if (!questionInfo.getMember().getId().equals(member.getId())) {
                throw new AccessDeniedException("해당 문제를 삭제할 권한이 없습니다.");
            }

            // 삭제 처리
            questionInfo.setDel(true);
        }
    }

    @Transactional
    public void moveQuestion(String token, MoveQuestionRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        // 원본 문제집 조회 및 검증
        Long sourceWorkBookId = encryptionService.decryptPrimaryKey(request.getEncryptedSourceWorkbookId());
        WorkBook sourceWorkBook = workBookRepository.findById(sourceWorkBookId)
                .orElseThrow(() -> new NoSuchElementException("해당 원본 문제집을 찾을 수 없습니다."));
        if (!sourceWorkBook.getMember().equals(member)) {
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }

        // 대상 문제집 조회 및 검증
        Long targetWorkbookId = encryptionService.decryptPrimaryKey(request.getEncryptedTargetWorkbookId());
        WorkBook targetWorkBook = workBookRepository.findById(targetWorkbookId)
                .orElseThrow(() -> new NoSuchElementException("해당 대상 문제집을 찾을 수 없습니다."));
        if (!targetWorkBook.getMember().equals(member)) {
            throw new NoSuchElementException("사용자가 소유한 대상 문제집이 아닙니다.");
        }

        // 문제 이동
        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            Question questionInfo = questionRepository.findById(questionInfoId)
                    .orElseThrow(() -> new NoSuchElementException("해당 문제 정보가 존재하지 않습니다."));

            if (!questionInfo.getMember().equals(member)) {
                throw new AccessDeniedException("사용자가 소유한 문제가 아닙니다.");
            }
            QuestionInfo targetQuestionInfo = questionInfo.getQuestionInfo();

            // 대상 문제집에 동일한 문제가 존재하는지 확인
            boolean isDuplicate = workBookInfoRepository.existsByWorkBookIdAndQuestionInfoId(targetWorkbookId, targetQuestionInfo.getId());
            if (isDuplicate) {
                throw new IllegalStateException("대상 문제집에 이미 동일한 문제가 존재합니다.");
            }

            WorkBookInfo workBookInfo = workBookInfoRepository.findByWorkBookIdAndQuestionInfoId(
                    sourceWorkBookId, targetQuestionInfo.getId()).get();
            workBookInfo.setWorkBook(targetWorkBook);
        }
    }




}

