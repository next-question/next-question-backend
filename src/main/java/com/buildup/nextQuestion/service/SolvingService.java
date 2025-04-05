package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.dto.solving.FindQuestionsByNormalExamResponse;
import com.buildup.nextQuestion.dto.solving.*;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.utility.JwtUtility;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolvingService {

    private final HistoryRepository historyRepository;
    private final HistoryInfoRepository historyInfoRepository;
    private final JwtUtility jwtUtility;
    private final LocalMemberRepository localMemberRepository;
    private final EncryptionService encryptionService;
    private final WorkBookRepository workBookRepository;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final QuestionRepository questionRepository;


    @Transactional
    public List<FindQuestionsByNormalExamResponse> findQuestionsByNormalExam(String token, FindQuestionsByNormalExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        if (!workBookRepository.existsByIdAndMemberId(workBookId, member.getId())){
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }
        List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBookId);
        List<Question> requestedQuestions = new ArrayList<>();
        for (WorkBookInfo workBookInfo : workBookInfos) {
            Long questionInfoId = workBookInfo.getQuestionInfo().getId();
            Question question = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId).get();
            if (question.getDel())
                continue;
            requestedQuestions.add(question);
        }

        if (requestedQuestions.size() < request.getOptions().getCount()){
            throw new IllegalArgumentException("문제집의 문제 수가 선택한 수보다 적습니다.");
        }
        List<FindQuestionsByNormalExamResponse> response = new ArrayList<>();
        if (request.getOptions().isRandom()){

            Collections.shuffle(requestedQuestions);
            List<Question> selectedQuestions = requestedQuestions.subList(0, request.getOptions().getCount());
            for (Question question : selectedQuestions) {
                QuestionInfo questionInfo = question.getQuestionInfo();

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

            for (Question question : requestedQuestions) {
                QuestionInfo questionInfo = question.getQuestionInfo();
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

    public List<FindQuestionsByMockExamResponse> findQuestionsByMockExam(String token, FindQuestionsByMockExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        Long workBookId = encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId());

        if (!workBookRepository.existsByIdAndMemberId(workBookId, member.getId())){
            throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
        }
        List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBookId);
        List<Question> requestedQuestions = new ArrayList<>();
        for (WorkBookInfo workBookInfo : workBookInfos) {
            Long questionInfoId = workBookInfo.getQuestionInfo().getId();
            Question question = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId).get();
            if (question.getDel())
                continue;
            requestedQuestions.add(question);
        }

        if (requestedQuestions.size() < request.getOptions().getCount()){
            throw new IllegalArgumentException("문제집의 문제 수가 선택한 수보다 적습니다.");
        }
        List<FindQuestionsByMockExamResponse> response = new ArrayList<>();
        Collections.shuffle(requestedQuestions);
        List<Question> selectedQuestions = requestedQuestions.subList(0, request.getOptions().getCount());
        for (Question question : selectedQuestions) {
            QuestionInfo questionInfo = question.getQuestionInfo();

            FindQuestionsByMockExamResponse selectedQuestion = new FindQuestionsByMockExamResponse();
            selectedQuestion.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
            selectedQuestion.setName(questionInfo.getName());
            selectedQuestion.setType(questionInfo.getType());
            selectedQuestion.setAnswer(questionInfo.getAnswer());
            selectedQuestion.setOpt(questionInfo.getOption());
            selectedQuestion.setWrong(question.getWrong());
            selectedQuestion.setRecentSolveTime(question.getRecentSolveTime());

            response.add(selectedQuestion);
        }
        return response;
    }

    private List<QuestionInfo> selectRandomQuestions(List<QuestionInfo> questionList, int count) {
        Collections.shuffle(questionList);
        return questionList.subList(0, Math.min(count, questionList.size()));
    }

    @Transactional
    public void saveHistoryByExam(String token, SaveHistoryByExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        // 기록 저장
        History history = new History();
        history.setMember(member);
        history.setSolvedDate(new Timestamp(System.currentTimeMillis()));
        history.setType(request.getType());

        History savedHistory = historyRepository.save(history);

        WorkBook workBook = workBookRepository.findById(encryptionService.decryptPrimaryKey(request.getEncryptedWorkBookId())).get();
        workBook.setRecentSolveDate(new Timestamp(System.currentTimeMillis()));

        List<ExamInfoDTO> infos = request.getInfo();
        for (ExamInfoDTO info : infos) {
            Long questionId = encryptionService.decryptPrimaryKey(info.getEncryptedQuestionId());
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문제를 찾을 수 없습니다."));

            QuestionInfo questionInfo = question.getQuestionInfo();

            HistoryInfo historyInfo = new HistoryInfo();
            historyInfo.setHistory(savedHistory);
            historyInfo.setQuestion(question);
            historyInfo.setWrong(info.getWrong());

            historyInfoRepository.save(historyInfo);
        }
    }

    @Transactional
    public List<FindHistoryByMemberResponse> findHistoryByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();

        List<FindHistoryByMemberResponse> response = new ArrayList<>();

        List<History> histories = historyRepository.findAllByMemberId(member.getId());
        for (History history : histories) {
            FindHistoryByMemberResponse requestedHistory = new FindHistoryByMemberResponse();
            requestedHistory.setEncryptedHistoryId(encryptionService.encryptPrimaryKey(history.getId()));
            requestedHistory.setSolvedDate(history.getSolvedDate());
            requestedHistory.setSolvedType(history.getType());

            response.add(requestedHistory);
        }
        return response;
    }

    @Transactional
    public List<FindHistoryInfoByHistoryResponse> findHistoryInfoByHistory(FindHistoryInfoByHistoryRequest request) throws Exception {
        Long historyId = encryptionService.decryptPrimaryKey(request.getEncryptedHistoryId());
        List<HistoryInfo> historyInfos = historyInfoRepository.findAllByHistoryId(historyId);

        List<FindHistoryInfoByHistoryResponse> responses = new ArrayList<>();
        for (HistoryInfo historyInfo : historyInfos) {
            FindHistoryInfoByHistoryResponse response = new FindHistoryInfoByHistoryResponse();

            QuestionInfo questionInfo = historyInfo.getQuestion().getQuestionInfo();
            response.setName(questionInfo.getName());
            response.setType(questionInfo.getType());
            response.setAnswer(questionInfo.getAnswer());
            response.setOpt(questionInfo.getOption());
            response.setWrong(historyInfo.getWrong());

            responses.add(response);
        }
        return responses;
    }

    @Transactional
    public List<GetOrAssignTodayQuestionsResponse> getOrAssignTodayQuestions(String token) throws Exception {
        LocalDate today = LocalDate.now();

        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = localMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 멤버를 찾을 수 없습니다."))
                .getMember();


        List<GetOrAssignTodayQuestionsResponse> responses = new ArrayList<>();

        // 이미 오늘 제공된 문제가 있다면 그대로 반환
        List<Question> todayQuestions = questionRepository.findByMemberAndAssignedDate(member, today);
        if (!todayQuestions.isEmpty()) {
            for (Question q : todayQuestions) {
                GetOrAssignTodayQuestionsResponse response = new GetOrAssignTodayQuestionsResponse();
                response.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(q.getId()));
                response.setName(q.getQuestionInfo().getName());
                response.setType(q.getQuestionInfo().getType());
                response.setAnswer(q.getQuestionInfo().getAnswer());
                response.setOpt(q.getQuestionInfo().getOption());
                response.setWrong(q.getWrong());
                response.setRecentSolveTime(q.getRecentSolveTime());

                q.setAssignedDate(today);
                responses.add(response);
            }


            return responses;
        }

        // 사용자가 보유한 문제 중 삭제되지 않은 것만 필터링
        List<Question> ownedQuestions = questionRepository.findAllByMemberId(member.getId()).stream()
                .filter(q -> !q.getDel())
                .toList();

        if (ownedQuestions.isEmpty()) {
            throw new IllegalStateException("사용자가 학습 중인 문제가 없습니다.");
        }

        // QuestionInfo 단위로 중복 제거 (같은 문제 여러 개 가지고 있을 수 있으니까)
        Map<Long, Question> latestByInfoId = new HashMap<>();
        for (Question q : ownedQuestions) {
            Long infoId = q.getQuestionInfo().getId();
            if (!latestByInfoId.containsKey(infoId) || isOlder(q, latestByInfoId.get(infoId))) {
                latestByInfoId.put(infoId, q);
            }
        }

        List<Question> uniqueQuestions = new ArrayList<>(latestByInfoId.values());

        // 최근 학습 시간 기준 오름차순 정렬 (recentSolveTime이 null이면 가장 먼저)
        uniqueQuestions.sort(Comparator.comparing(
                Question::getRecentSolveTime,
                Comparator.nullsFirst(Comparator.naturalOrder())
        ));

        // 최대 3개 선택
        List<Question> selected = uniqueQuestions.stream()
                .limit(3)
                .toList();



        // assignedDate 설정 (오늘 제공된 문제로 표시)
        for (Question q : selected) {
            GetOrAssignTodayQuestionsResponse response = new GetOrAssignTodayQuestionsResponse();
            response.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(q.getId()));
            response.setName(q.getQuestionInfo().getName());
            response.setType(q.getQuestionInfo().getType());
            response.setAnswer(q.getQuestionInfo().getAnswer());
            response.setOpt(q.getQuestionInfo().getOption());
            response.setWrong(q.getWrong());
            response.setRecentSolveTime(q.getRecentSolveTime());

            q.setAssignedDate(today);
            responses.add(response);
        }

        return responses;
    }

    private boolean isOlder(Question a, Question b) {
        Timestamp aTime = a.getRecentSolveTime();
        Timestamp bTime = b.getRecentSolveTime();
        if (aTime == null) return true;
        if (bTime == null) return false;
        return aTime.before(bTime);
    }

}
