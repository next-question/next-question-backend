package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.dto.question.*;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.*;

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
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        WorkBook workBook = workBookRepository.findById(workBookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문제집을 찾을 수 없습니다."));

        // 회원 문제집 저장
        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            QuestionInfo questionInfo = questionInfoRepository.findById(questionInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 문제가 존재하지 않습니다."));

            // 문제집에 동일한 문제가 이미 존재하는지 확인
            boolean isDuplicate = workBookInfoRepository.existsByWorkBookIdAndQuestionInfoId(workBook.getId(), questionInfoId);
            if (isDuplicate) {
                throw new IllegalStateException("문제집에 이미 동일한 문제가 존재합니다.");
            }

            WorkBookInfo workBookInfo = new WorkBookInfo(questionInfo, workBook);
            Question question = new Question(member, questionInfo);
            questionRepository.save(question);
            workBookInfoRepository.save(workBookInfo);
        }
    }

    @Transactional
    public List<FindQuestionByMemberResponse> findQuestionByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);
        Member member = localMemberRepository.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getMember();

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
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        if (encryptedQuestionInfoIds == null || encryptedQuestionInfoIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 문제가 없습니다.");
        }

        for (String encryptedQuestionInfoId : encryptedQuestionInfoIds) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);


            Question questionInfo = questionRepository.findById(questionInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 문제를 찾을 수 없습니다."));

            // 해당 사용자의 문제인지 검증 (소유자가 아니면 예외 발생)
            if (!questionInfo.getMember().getId().equals(member.getId())) {
                throw new IllegalAccessException("해당 문제를 삭제할 권한이 없습니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        // 원본 문제집 조회 및 검증
        Long sourceWorkBookId = encryptionService.decryptPrimaryKey(request.getEncryptedSourceWorkbookId());
        WorkBook sourceWorkBook = workBookRepository.findById(sourceWorkBookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 원본 문제집을 찾을 수 없습니다."));
        if (!sourceWorkBook.getMember().equals(member)) {
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }

        // 대상 문제집 조회 및 검증
        Long targetWorkbookId = encryptionService.decryptPrimaryKey(request.getEncryptedTargetWorkbookId());
        WorkBook targetWorkBook = workBookRepository.findById(targetWorkbookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대상 문제집을 찾을 수 없습니다."));
        if (!targetWorkBook.getMember().equals(member)) {
            throw new AccessDeniedException("사용자가 소유한 대상 문제집이 아닙니다.");
        }

        // 문제 이동
        for (String encryptedQuestionInfoId : request.getEncryptedQuestionInfoIds()) {
            Long questionInfoId = encryptionService.decryptPrimaryKey(encryptedQuestionInfoId);
            Question questionInfo = questionRepository.findById(questionInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 문제 정보가 존재하지 않습니다."));

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

    public List<FindQuestionsByNormalExamResponse> findQuestionsByNormalExam(String token, FindQuestionByNormalExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        if (!workBookRepository.existsByIdAndMemberId(workBookId, member.getId())){
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }
        List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBookId);
        if (workBookInfos.size() < request.getOptions().getCount()){
            throw new IllegalArgumentException("문제집의 문제 수가 선택한 수보다 적습니다.");
        }
        List<FindQuestionsByNormalExamResponse> response = new ArrayList<>();
        if (request.getOptions().isRandom()){
            Collections.shuffle(workBookInfos);
            List<WorkBookInfo> selectedWorkBookInfos = workBookInfos.subList(0, request.getOptions().getCount());
            for (WorkBookInfo selectedWorkBookInfo : selectedWorkBookInfos) {
                QuestionInfo questionInfo = selectedWorkBookInfo.getQuestionInfo();
                Question question = questionRepository.findByMemberIdAndQuestionInfoIdAndDelFalse(
                        member.getId(), questionInfo.getId()).get();

                FindQuestionsByNormalExamResponse selectedQuestion = new FindQuestionsByNormalExamResponse();
                selectedQuestion.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
                selectedQuestion.setName(questionInfo.getName());
                selectedQuestion.setType(questionInfo.getType());
                selectedQuestion.setAnswer(questionInfo.getAnswer());
                selectedQuestion.setOpt(questionInfo.getOption());
                selectedQuestion.setWrong(question.getWrong());
                selectedQuestion.setRecentSolveTime(question.getRecentSolveTime());

                response.add(selectedQuestion);
            }
        }
        else {

            // 문제 유형별 분류
            List<QuestionInfo> oxList = new ArrayList<>();
            List<QuestionInfo> multipleList = new ArrayList<>();
            List<QuestionInfo> blankList = new ArrayList<>();

            for (WorkBookInfo workBookInfo : workBookInfos) {
                QuestionInfo questionInfo = workBookInfo.getQuestionInfo();
                if (questionInfo.getType().equals(QuestionType.OX)) {
                    oxList.add(questionInfo);
                } else if (questionInfo.getType().equals(QuestionType.MULTIPLE_CHOICE)) {
                    multipleList.add(questionInfo);
                } else if (questionInfo.getType().equals(QuestionType.FILL_IN_THE_BLANK)) {
                    blankList.add(questionInfo);
                }
            }

            // 사용자 선택 옵션 확인
            int totalCount = request.getOptions().getCount();
            Map<QuestionType, List<QuestionInfo>> selectedTypes = new HashMap<>();

            if (request.getOptions().isOx() && !oxList.isEmpty()) selectedTypes.put(QuestionType.OX, oxList);
            if (request.getOptions().isMultiple() && !multipleList.isEmpty())
                selectedTypes.put(QuestionType.MULTIPLE_CHOICE, multipleList);
            if (request.getOptions().isBlank() && !blankList.isEmpty())
                selectedTypes.put(QuestionType.FILL_IN_THE_BLANK, blankList);

            int selectedTypeCount = selectedTypes.size();
            if (selectedTypeCount == 0) {
                throw new IllegalArgumentException("선택한 문제 유형의 문제가 존재하지 않습니다.");
            }

            // 균등 분배할 개수
            int perTypeCount = totalCount / selectedTypeCount;
            int remainder = totalCount % selectedTypeCount;

            Map<QuestionType, Integer> questionDistribution = new HashMap<>();
            for (QuestionType type : selectedTypes.keySet()) {
                questionDistribution.put(type, perTypeCount + (remainder-- > 0 ? 1 : 0));
            }

            // 부족한 유형을 모두 체크하여 리스트로 저장
            List<String> missingTypes = new ArrayList<>();
            for (Map.Entry<QuestionType, Integer> entry : questionDistribution.entrySet()) {
                QuestionType type = entry.getKey();
                List<QuestionInfo> availableQuestions = selectedTypes.get(type);
                int requiredCount = entry.getValue();

                if (availableQuestions.size() < requiredCount) {
                    missingTypes.add(type + " 유형의 문제가 부족합니다. (필요: " + requiredCount + "개, 존재: " + availableQuestions.size() + "개)");
                }
            }

            // 부족한 유형이 하나라도 있다면 에러 메시지 반환
            if (!missingTypes.isEmpty()) {
                throw new IllegalArgumentException("요청한 문제를 출제할 수 없습니다.\n" + String.join("\n", missingTypes));
            }

            // 문제 선택
            List<QuestionInfo> finalSelected = new ArrayList<>();
            for (Map.Entry<QuestionType, Integer> entry : questionDistribution.entrySet()) {
                QuestionType type = entry.getKey();
                List<QuestionInfo> availableQuestions = selectedTypes.get(type);
                finalSelected.addAll(selectRandomQuestions(availableQuestions, entry.getValue()));
            }

            // 응답 생성
            for (QuestionInfo questionInfo : finalSelected) {
                Question question = questionRepository.findByMemberIdAndQuestionInfoIdAndDelFalse(
                        member.getId(), questionInfo.getId()).orElseThrow(() -> new IllegalArgumentException("해당 문제를 찾을 수 없습니다."));

                FindQuestionsByNormalExamResponse selectedQuestion = new FindQuestionsByNormalExamResponse();
                selectedQuestion.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
                selectedQuestion.setName(questionInfo.getName());
                selectedQuestion.setType(questionInfo.getType());
                selectedQuestion.setAnswer(questionInfo.getAnswer());
                selectedQuestion.setOpt(questionInfo.getOption());
                selectedQuestion.setWrong(question.getWrong());
                selectedQuestion.setRecentSolveTime(question.getRecentSolveTime());

                response.add(selectedQuestion);
            }
        }

        return response;
    }

    private List<QuestionInfo> selectRandomQuestions(List<QuestionInfo> questionList, int count) {
        Collections.shuffle(questionList);
        return questionList.subList(0, Math.min(count, questionList.size()));
    }
}

