package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.domain.*;
import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.dto.question.NormalExamOption;
import com.buildup.nextQuestion.dto.solving.FindQuestionsByNormalExamResponse;
import com.buildup.nextQuestion.dto.solving.*;
import com.buildup.nextQuestion.repository.*;
import com.buildup.nextQuestion.support.MemberFinder;
import com.buildup.nextQuestion.utility.JwtUtility;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolvingService {

    private final HistoryRepository historyRepository;
    private final HistoryInfoRepository historyInfoRepository;
    private final JwtUtility jwtUtility;
    private final MemberFinder memberFinder;
    private final EncryptionService encryptionService;
    private final WorkBookRepository workBookRepository;
    private final WorkBookInfoRepository workBookInfoRepository;
    private final QuestionRepository questionRepository;
    private final AttendanceRepository attendanceRepository;


    @Transactional
    public List<FindQuestionsByNormalExamResponse> findQuestionsByNormalExam(String token, FindQuestionsByNormalExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = memberFinder.findMember(userId);

        // 전체 문제 리스트 생성
        List<Question> requestedQuestions = new ArrayList<>();

        for (String encryptedId : request.getEncryptedWorkBookIds()) {
            Long workBookId = encryptionService.decryptPrimaryKey(encryptedId);

            if (!workBookRepository.existsByIdAndMemberId(workBookId, member.getId())) {
                throw new AccessDeniedException("사용자가 소유한 문제집이 아닙니다.");
            }

            List<WorkBookInfo> workBookInfos = workBookInfoRepository.findAllByWorkBookId(workBookId);

            for (WorkBookInfo workBookInfo : workBookInfos) {
                Long questionInfoId = workBookInfo.getQuestionInfo().getId();
                Optional<Question> questionOpt = questionRepository.findByMemberIdAndQuestionInfoId(member.getId(), questionInfoId);

                questionOpt.ifPresent(q -> {
                    if (!q.getDel()) requestedQuestions.add(q);
                });
            }
        }

        if (requestedQuestions.size() < request.getOptions().getCount()) {
            throw new IllegalArgumentException("선택된 문제집의 문제 수가 요청 수보다 적습니다.");
        }

        // 출제 방식에 따라 분기
        if (request.getOptions().isRandom()) {
            return buildRandomExamResponse(requestedQuestions, request.getOptions().getCount());
        } else {
            return buildTypeBasedExamResponse(requestedQuestions, request.getOptions(), member);
        }
    }

    @SneakyThrows
    private List<FindQuestionsByNormalExamResponse> buildRandomExamResponse(List<Question> questions, int count) {
        Collections.shuffle(questions);
        List<Question> selected = questions.subList(0, count);

        return selected.stream().map(this::mapToResponse).toList();
    }

    private FindQuestionsByNormalExamResponse mapToResponse(Question question) {
        try {
            QuestionInfo info = question.getQuestionInfo();
            FindQuestionsByNormalExamResponse res = new FindQuestionsByNormalExamResponse();
            res.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(question.getId()));
            res.setName(info.getName());
            res.setType(info.getType());
            res.setAnswer(info.getAnswer());
            res.setOpt(info.getOption());
            res.setWrong(question.getWrong());
            res.setRecentSolveTime(question.getRecentSolveTime());
            return res;
        } catch (Exception e) {
            throw new RuntimeException("암호화 중 오류 발생", e);
        }
    }




    private List<FindQuestionsByNormalExamResponse> buildTypeBasedExamResponse(List<Question> questions, NormalExamOption options, Member member) {
        // 문제 유형별 분류
        Map<QuestionType, List<Question>> typeMap = new HashMap<>();
        typeMap.put(QuestionType.OX, new ArrayList<>());
        typeMap.put(QuestionType.MULTIPLE_CHOICE, new ArrayList<>());
        typeMap.put(QuestionType.FILL_IN_THE_BLANK, new ArrayList<>());

        for (Question question : questions) {
            typeMap.get(question.getQuestionInfo().getType()).add(question);
        }

        // 사용자 선택 유형 필터
        Map<QuestionType, List<Question>> selectedTypes = new LinkedHashMap<>();
        if (options.isOx() && !typeMap.get(QuestionType.OX).isEmpty()) {
            selectedTypes.put(QuestionType.OX, typeMap.get(QuestionType.OX));
        }
        if (options.isMultiple() && !typeMap.get(QuestionType.MULTIPLE_CHOICE).isEmpty()) {
            selectedTypes.put(QuestionType.MULTIPLE_CHOICE, typeMap.get(QuestionType.MULTIPLE_CHOICE));
        }
        if (options.isBlank() && !typeMap.get(QuestionType.FILL_IN_THE_BLANK).isEmpty()) {
            selectedTypes.put(QuestionType.FILL_IN_THE_BLANK, typeMap.get(QuestionType.FILL_IN_THE_BLANK));
        }

        int totalCount = options.getCount();
        int typeCount = selectedTypes.size();

        if (typeCount == 0) {
            throw new IllegalArgumentException("선택한 문제 유형의 문제가 존재하지 않습니다.");
        }

        // 균등 분배
        int perType = totalCount / typeCount;
        int remainder = totalCount % typeCount;

        Map<QuestionType, Integer> distribution = new HashMap<>();
        for (QuestionType type : selectedTypes.keySet()) {
            distribution.put(type, perType + (remainder-- > 0 ? 1 : 0));
        }

        // 선택된 문제들
        List<Question> selectedQuestions = new ArrayList<>();
        List<String> shortageMessages = new ArrayList<>();

        for (Map.Entry<QuestionType, Integer> entry : distribution.entrySet()) {
            List<Question> pool = selectedTypes.get(entry.getKey());
            int needed = entry.getValue();

            if (pool.size() < needed) {
                shortageMessages.add(entry.getKey() + " 유형의 문제가 부족합니다. (필요: " + needed + "개, 존재: " + pool.size() + "개)");
            } else {
                Collections.shuffle(pool);
                selectedQuestions.addAll(pool.subList(0, needed));
            }
        }

        if (!shortageMessages.isEmpty()) {
            throw new IllegalArgumentException("요청한 문제를 출제할 수 없습니다.\n" + String.join("\n", shortageMessages));
        }

        return selectedQuestions.stream().map(this::mapToResponse).toList();
    }




    public List<FindQuestionsByMockExamResponse> findQuestionsByMockExam(String token, FindQuestionsByMockExamRequest request) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

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

        Member member = memberFinder.findMember(userId);

        // 기록 저장
        History history = new History();
        history.setMember(member);
        history.setSolvedDate(new Timestamp(System.currentTimeMillis()));
        history.setType(request.getType());

        History savedHistory = historyRepository.save(history);

        for (WorkBookInfoDTO workBookInfoDTO : request.getWorkBookInfoDTOS()) {
            WorkBook workBook = workBookRepository.findById(encryptionService.decryptPrimaryKey(workBookInfoDTO.getEncryptedWorkBookId())).get();
            workBook.setRecentSolveDate(new Timestamp(System.currentTimeMillis()));

            List<ExamInfoDTO> infos = workBookInfoDTO.getInfo();
            for (ExamInfoDTO info : infos) {
                Long questionId = encryptionService.decryptPrimaryKey(info.getEncryptedQuestionId());
                Question question = questionRepository.findById(questionId)
                        .orElseThrow(() -> new EntityNotFoundException("해당 문제를 찾을 수 없습니다."));

                HistoryInfo historyInfo = new HistoryInfo();
                historyInfo.setHistory(savedHistory);
                historyInfo.setQuestion(question);
                historyInfo.setWrong(info.getWrong());

                historyInfoRepository.save(historyInfo);
            }
        }


    }

    @Transactional
    public List<FindHistoryByMemberResponse> findHistoryByMember(String token) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = memberFinder.findMember(userId);

        List<FindHistoryByMemberResponse> responses = new ArrayList<>();

        List<History> histories = historyRepository.findAllByMemberId(member.getId());

        for (History history : histories) {
            FindHistoryByMemberResponse response = new FindHistoryByMemberResponse();
            List<HistoryInfo> historyInfos = historyInfoRepository.findAllByHistoryId(history.getId());

            List<QuestionInfoDTO> requestedQuestionInfos = new ArrayList<>();
            for (HistoryInfo historyInfo : historyInfos) {
                QuestionInfoDTO requestedQuestionInfo = new QuestionInfoDTO();

                QuestionInfo questionInfo = historyInfo.getQuestion().getQuestionInfo();
                requestedQuestionInfo.setName(questionInfo.getName());
                requestedQuestionInfo.setType(questionInfo.getType());
                requestedQuestionInfo.setAnswer(questionInfo.getAnswer());
                requestedQuestionInfo.setOpt(questionInfo.getOption());
                requestedQuestionInfo.setWrong(historyInfo.getWrong());

                requestedQuestionInfos.add(requestedQuestionInfo);
            }
            response.setEncryptedHistoryId(encryptionService.encryptPrimaryKey(history.getId()));
            response.setSolvedType(history.getType());
            response.setSolvedDate(history.getSolvedDate());
            response.setQuestionInfos(requestedQuestionInfos);
            responses.add(response);
        }

        return responses;

    }


    @Transactional
    public List<GetOrAssignTodayQuestionsResponse> getOrAssignTodayQuestions(String token) throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate oneMonthAgo = today.minusMonths(1);

        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

        // 이미 오늘 문제를 받았는지 확인
        List<Question> todayQuestions = questionRepository.findByMemberAndAssignedDate(member, today);
        if (!todayQuestions.isEmpty()) {
            return todayQuestions.stream().map(q -> {
                GetOrAssignTodayQuestionsResponse res = new GetOrAssignTodayQuestionsResponse();
                try {
                    res.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(q.getId()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                res.setName(q.getQuestionInfo().getName());
                res.setType(q.getQuestionInfo().getType());
                res.setAnswer(q.getQuestionInfo().getAnswer());
                res.setOpt(q.getQuestionInfo().getOption());
                res.setWrong(q.getWrong());
                res.setRecentSolveTime(q.getRecentSolveTime());
                return res;
            }).toList();
        }

        // 문제 목록 조회 (삭제되지 않은 것 중 최근 1달 이내 풀이한 문제)
        List<Question> recentQuestions = questionRepository.findAllByMemberId(member.getId()).stream()
                .filter(q -> !q.getDel())
                .filter(q -> q.getRecentSolveTime() == null
                        || !q.getRecentSolveTime().toLocalDateTime().toLocalDate().isBefore(oneMonthAgo))
                .toList();

        if (recentQuestions.size() < 3) {
            throw new IllegalStateException("최근 한 달 이내 학습한 문제가 3개 미만입니다.");
        }

        // 중복 제거 (QuestionInfo 기준 최신 문제)
        Map<Long, Question> latestByInfoId = new HashMap<>();
        for (Question q : recentQuestions) {
            Long infoId = q.getQuestionInfo().getId();
            if (!latestByInfoId.containsKey(infoId) || isOlder(q, latestByInfoId.get(infoId))) {
                latestByInfoId.put(infoId, q);
            }
        }

        List<Question> uniqueQuestions = new ArrayList<>(latestByInfoId.values());

        // 오답 우선 정렬
        List<Question> wrongQuestions = uniqueQuestions.stream()
                .filter(Question::getWrong)
                .collect(Collectors.toList());

        List<Question> selected = new ArrayList<>(wrongQuestions.stream()
                .limit(3)
                .toList());

        // 부족한 수는 랜덤으로 보충
        if (selected.size() < 3) {
            Set<Long> selectedIds = selected.stream().map(q -> q.getId()).collect(Collectors.toSet());
            List<Question> remaining = uniqueQuestions.stream()
                    .filter(q -> !selectedIds.contains(q.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining);
            int remainingToPick = 3 - selected.size();
            selected.addAll(remaining.stream().limit(remainingToPick).toList());
        }

        if (selected.size() < 3) {
            throw new IllegalStateException("선정된 문제가 3개 미만입니다.");
        }

        // 결과 응답 구성
        List<GetOrAssignTodayQuestionsResponse> responses = new ArrayList<>();
        for (Question q : selected) {
            GetOrAssignTodayQuestionsResponse res = new GetOrAssignTodayQuestionsResponse();
            res.setEncryptedQuestionId(encryptionService.encryptPrimaryKey(q.getId()));
            res.setName(q.getQuestionInfo().getName());
            res.setType(q.getQuestionInfo().getType());
            res.setAnswer(q.getQuestionInfo().getAnswer());
            res.setOpt(q.getQuestionInfo().getOption());
            res.setWrong(q.getWrong());
            res.setRecentSolveTime(q.getRecentSolveTime());

            q.setAssignedDate(today);
            responses.add(res);
        }

        return responses;
    }


    @Transactional
    public void recordAttendance(String token, List<RecordAttendanceRequest> requests) throws Exception {
        LocalDate today = LocalDate.now();

        String userId = jwtUtility.getUserIdFromToken(token);

        // 사용자 조회
        Member member = memberFinder.findMember(userId);

        // 이미 출석 기록이 있는지 확인
        boolean alreadyChecked = attendanceRepository.existsByMemberAndDate(member, today);
        if (alreadyChecked){
            throw new IllegalArgumentException("이미 출석처리 되었습니다.");
        }

        if (requests.size() != 3){
            throw new IllegalArgumentException("출석 인증이 처리되지 않았습니다.");
        }
        // 문제 풀이 처리
        for (RecordAttendanceRequest request : requests) {
            Long questionId = encryptionService.decryptPrimaryKey(request.getEncryptedQuestionId());
            Question question = questionRepository.findById(questionId).get();
            question.setWrong(request.isWrong());
            question.setRecentSolveTime(new Timestamp(System.currentTimeMillis()));
        }

        // 출석 기록 저장
        Attendance attendance = new Attendance();
        attendance.setMember(member);
        attendance.setDate(today);
        attendance.setCheckedTime(new Timestamp(System.currentTimeMillis()));
        attendanceRepository.save(attendance);
    }
    @Transactional(readOnly = true)
    public FindQuestionsByTypeResponse findQuestionsByTypeResponse(String token, List<String> encryptedWorkBookIds) throws Exception {
        String userId = jwtUtility.getUserIdFromToken(token);

        Member member = memberFinder.findMember(userId);

        int multipleChoice = 0;
        int fillInTheBlank = 0;
        int ox = 0;

        for (String encryptedId : encryptedWorkBookIds) {
            Long workBookId = encryptionService.decryptPrimaryKey(encryptedId);
            WorkBook workBook = workBookRepository.findById(workBookId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 문제집을 찾을 수 없습니다."));

            if (!workBook.getMember().getId().equals(member.getId())) {
                throw new SecurityException("접근 권한이 없는 문제집입니다.");
            }

            multipleChoice += workBook.getMultipleChoice();
            fillInTheBlank += workBook.getFillInTheBlank();
            ox += workBook.getOx();
        }

        FindQuestionsByTypeResponse response = new FindQuestionsByTypeResponse();
        response.setMultipleChoice(multipleChoice);
        response.setFillInTheBlank(fillInTheBlank);
        response.setOx(ox);

        return response;
    }

    private boolean isOlder(Question a, Question b) {
        Timestamp aTime = a.getRecentSolveTime();
        Timestamp bTime = b.getRecentSolveTime();
        if (aTime == null) return true;
        if (bTime == null) return false;
        return aTime.before(bTime);
    }

}
