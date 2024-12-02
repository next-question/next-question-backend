package com.buildup.nextQuestion.Service;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.buildup.nextQuestion.dto.QuestionUpdateRequest;
import com.buildup.nextQuestion.Repository.QuestionRepository;
import com.buildup.nextQuestion.domain.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service

public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    //생성된 문제 리스트 저장
    public void saveAll(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        JsonNode questionsNode = jsonNode.get("questions");

        List<Question> questions = new ArrayList<>();

        for (JsonNode questionNode : questionsNode) {
            Question question = new Question();
            question.setName(questionNode.get("name").asText());
            String typeFrom = questionNode.get("type").asText();
            QuestionType type = QuestionType.valueOf(typeFrom);
            question.setType(type);
            question.setAnswer(questionNode.get("answer").asText());
            Timestamp currentTimestamp = Timestamp.from(Instant.now());
            question.setCreateTime(currentTimestamp);

            if (questionNode.get("type").asText().equals("MULTIPLE_CHOICE")) {
                question.setOption(questionNode.get("opt").asText());
            }

            questions.add(question);
        }

        questionRepository.saveAll(questions);
    }

    //문제 제공(json형식 반환)
    public List<Question> findAllQuestionByWorkBook(Long workbook_id){
        return questionRepository.findAllByWorkBookId(workbook_id);
    }

    //문제 정보 갱신(update)
    public void questionUpdate(List<QuestionUpdateRequest> updatedQuestions) {
        for(QuestionUpdateRequest request : updatedQuestions) {
            Long questionId = request.getQuestionId();

            Question existingQuestion = questionRepository.findById(questionId).get(); //id로 Question객체 가져옴
            existingQuestion.setWrong(request.getWrong()); //객체 정답여부 update
            existingQuestion.setRecentSolveTime(request.getRecentSolveTime()); //객체 최근 시간 update

            questionRepository.save(existingQuestion); //객체 저장
        }
    }


}

