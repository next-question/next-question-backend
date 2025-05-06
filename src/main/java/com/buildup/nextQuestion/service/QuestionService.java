package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.dto.question.MoveQuestionRequest;
import com.buildup.nextQuestion.dto.question.SaveQuestionRequest;
import com.buildup.nextQuestion.dto.question.FindQuestionByMemberResponse;
import com.buildup.nextQuestion.dto.solving.FindQuestionsByTypeResponse;
import com.buildup.nextQuestion.exception.DuplicateResourceException;
import com.buildup.nextQuestion.exception.AccessDeniedException;
import com.buildup.nextQuestion.dto.question.*;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {


    private final QuestionInfoRepository questionInfoRepository;
    private final QuestionRepository questionRepository;
    private final EncryptionService encryptionService;
    private final JwtUtility jwtUtility;

    private final WorkBookRepository workBookRepository;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final MemberFinder memberFinder;

    //생성된 문제 리스트 저장
    @Transactional
    public List<UploadFileByMemberResponse> saveAll(JsonNode jsonNode) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JsonNode questionsNode = jsonNode.get("questions");
        List<UploadFileByMemberResponse> response = new ArrayList<>();
        if (questionsNode != null && questionsNode.isArray()) {

            List<QuestionInfo> questionInfos = objectMapper.readValue(
                    questionsNode.toString(),
                    new TypeReference<List<QuestionInfo>>() {
                    }
            );
            List<QuestionInfo> createdQuestionInfos = questionInfoRepository.saveAll(questionInfos);

            for (QuestionInfo createdQuestionInfo : createdQuestionInfos) {
                UploadFileByMemberResponse questionInfo = new UploadFileByMemberResponse();
                questionInfo.setEncryptedQuestionInfoId(
                        encryptionService.encryptPrimaryKey(createdQuestionInfo.getId()));
                questionInfo.setName(createdQuestionInfo.getName());
                questionInfo.setType(createdQuestionInfo.getType());
                questionInfo.setAnswer(createdQuestionInfo.getAnswer());
                questionInfo.setOpt(createdQuestionInfo.getOption());

                response.add(questionInfo);
            }
        }

        return response;
    }

    @Transactional
    public void saveQuestion(String token, SaveQuestionRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        WorkBook workBook = workBookRepository.findById(workBookId)
                .orElseThrow(() -> new EntityNotFoundException("해당 문제집을 찾을 수 없습니다."));

        // 타입별 개수 초기화
        int multipleChoiceCount = 0;
        int fillInTheBlankCount = 0;
        int oxCount = 0;

        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            QuestionInfo questionInfo = questionInfoRepository.findById(questionInfoId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문제가 존재하지 않습니다."));

            // 중복 확인
            boolean isDuplicate = workBookInfoRepository.existsByWorkBookIdAndQuestionInfoId(workBook.getId(), questionInfoId);
            if (isDuplicate) {
                throw new DuplicateResourceException("문제집에 이미 동일한 문제가 존재합니다.");
            }

            // 문제 저장
            WorkBookInfo workBookInfo = new WorkBookInfo(questionInfo, workBook);
            Question question = new Question(member, questionInfo);
            questionRepository.save(question);
            workBookInfoRepository.save(workBookInfo);

            // 타입별 개수 카운트
            switch (questionInfo.getType()) {
                case MULTIPLE_CHOICE -> multipleChoiceCount++;
                case FILL_IN_THE_BLANK -> fillInTheBlankCount++;
                case OX -> oxCount++;
            }
        }

        // WorkBook에 카운트 값 저장
        workBook.setMultipleChoice(workBook.getMultipleChoice()+ multipleChoiceCount);
        workBook.setFillInTheBlank(workBook.getFillInTheBlank()+ fillInTheBlankCount);
        workBook.setOx(workBook.getOx()+ oxCount);
        workBookRepository.save(workBook);
    }


    @Transactional
    public List<FindQuestionByMemberResponse> findQuestionByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

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

    static FindQuestionsByTypeResponse classifyQuestionType (Member member, WorkBook workBook) throws Exception {
        int multipleChoice = 0;
        int fillInTheBlank = 0;
        int ox = 0;


            multipleChoice += workBook.getMultipleChoice();
            fillInTheBlank += workBook.getFillInTheBlank();
            ox += workBook.getOx();


        FindQuestionsByTypeResponse response = new FindQuestionsByTypeResponse();
        response.setMultipleChoice(multipleChoice);
        response.setFillInTheBlank(fillInTheBlank);
        response.setOx(ox);

        return response;
    }

    @Transactional
    public FindQuestionsByTypeResponse deleteQuestion(String token, DeleteQuestionRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = memberFinder.findMember(userId);

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());
        WorkBook workBook = workBookRepository.findById(workBookId)
                .orElseThrow(() -> new EntityNotFoundException("해당 문제집을 찾을 수 없습니다."));

        if (!workBook.getMember().getId().equals(member.getId())) {
            throw new SecurityException("접근 권한이 없는 문제집입니다.");
        }
        List<String> encryptedQuestionIds = request.getEncryptedQuestionIds();

        if (encryptedQuestionIds == null || encryptedQuestionIds.isEmpty()) {
            throw new EntityNotFoundException("삭제할 문제가 없습니다.");
        }

        for (String encryptedQuestionId : encryptedQuestionIds) {
            Long questionId = encryptionService.decryptPrimaryKey(encryptedQuestionId);


            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문제를 찾을 수 없습니다."));

            QuestionType type = question.getQuestionInfo().getType();
            // 해당 사용자의 문제인지 검증 (소유자가 아니면 예외 발생)
            if (!question.getMember().getId().equals(member.getId())) {
                throw new AccessDeniedException("해당 문제를 삭제할 권한이 없습니다.");
            }

            // 삭제 처리
            question.setDel(true);
            if (type.equals(QuestionType.MULTIPLE_CHOICE)) {
                workBook.setMultipleChoice(workBook.getMultipleChoice() - 1);
            }
            else if (type.equals(QuestionType.FILL_IN_THE_BLANK)) {
                workBook.setFillInTheBlank(workBook.getFillInTheBlank() - 1);
            }
            else if (type.equals(QuestionType.OX)) {
                workBook.setOx(workBook.getOx() - 1);
            }
        }
        FindQuestionsByTypeResponse response = classifyQuestionType(member, workBook);

        return response;

    }

    @Transactional
    public List<FindQuestionsByTypeResponse> moveQuestion(String token, MoveQuestionRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

        // 원본 문제집 조회 및 검증
        Long sourceWorkBookId = encryptionService.decryptPrimaryKey(request.getEncryptedSourceWorkbookId());
        WorkBook sourceWorkBook = workBookRepository.findById(sourceWorkBookId)
                .orElseThrow(() -> new EntityNotFoundException("해당 원본 문제집을 찾을 수 없습니다."));
        if (!sourceWorkBook.getMember().equals(member)) {
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }

        // 대상 문제집 조회 및 검증
        Long targetWorkbookId = encryptionService.decryptPrimaryKey(request.getEncryptedTargetWorkbookId());
        WorkBook targetWorkBook = workBookRepository.findById(targetWorkbookId)
                .orElseThrow(() -> new EntityNotFoundException("해당 대상 문제집을 찾을 수 없습니다."));
        if (!targetWorkBook.getMember().equals(member)) {
            throw new EntityNotFoundException("사용자가 소유한 대상 문제집이 아닙니다.");
        }

        // 문제 이동
        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            Question targetQuestion = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문제 정보가 존재하지 않습니다."));

            if (!targetQuestion.getMember().equals(member)) {
                throw new AccessDeniedException("사용자가 소유한 문제가 아닙니다.");
            }
            QuestionInfo targetQuestionInfo = targetQuestion.getQuestionInfo();
            QuestionType type = targetQuestionInfo.getType();
            // 대상 문제집에 동일한 문제가 존재하는지 확인
            boolean isDuplicate = workBookInfoRepository.existsByWorkBookIdAndQuestionInfoId(targetWorkbookId, targetQuestionInfo.getId());
            if (isDuplicate) {
                throw new DuplicateResourceException("대상 문제집에 이미 동일한 문제가 존재합니다.");
            }

            WorkBookInfo workBookInfo = workBookInfoRepository.findByWorkBookIdAndQuestionInfoId(
                    sourceWorkBookId, targetQuestionInfo.getId()).get();
            workBookInfo.setWorkBook(targetWorkBook);

            if (type.equals(QuestionType.MULTIPLE_CHOICE)) {
                sourceWorkBook.setMultipleChoice(sourceWorkBook.getMultipleChoice() - 1);
                targetWorkBook.setMultipleChoice(targetWorkBook.getMultipleChoice() + 1);
            }
            else if (type.equals(QuestionType.FILL_IN_THE_BLANK)) {
                sourceWorkBook.setFillInTheBlank(sourceWorkBook.getFillInTheBlank() - 1);
                targetWorkBook.setFillInTheBlank(targetWorkBook.getFillInTheBlank() + 1);
            }
            else if (type.equals(QuestionType.OX)) {
                sourceWorkBook.setOx(sourceWorkBook.getOx() - 1);
                targetWorkBook.setOx(targetWorkBook.getOx() + 1);
            }

        }
        List<FindQuestionsByTypeResponse> responses = new ArrayList<>();
        responses.add(classifyQuestionType(member, sourceWorkBook));
        responses.add(classifyQuestionType(member, targetWorkBook));

        return responses;
    }










}

