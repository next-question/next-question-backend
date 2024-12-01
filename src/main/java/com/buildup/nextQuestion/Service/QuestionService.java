package com.buildup.nextQuestion.Service;

import com.buildup.nextQuestion.dto.QuestionUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import com.buildup.nextQuestion.Repository.QuestionRepository;
import com.buildup.nextQuestion.domain.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service

public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    //생성된 문제 저장(문제 db에 저장)
    public void save(Question question) {
        questionRepository.save(question);
    }

    //문제 제공(json형식 반환)
    public List<Question> findAllQuestionByWorkBook(Long workbook_id){
        return questionRepository.findAllByWorkBookId(workbook_id);
    }

    //문제 정보 갱신(받아온거 sql update)
    public void questionUpdate(QuestionUpdateRequest updatedQuestion) {
        Long questionId = updatedQuestion.getQuestionId();

        Question existingQuestion = questionRepository.findById(questionId).get(); //id로 Question객체 가져옴

        existingQuestion.setWrong(updatedQuestion.getWrong()); //객체 정답여부 update

        existingQuestion.setRecentSolveTime(updatedQuestion.getRecentSolveTime()); //객체 최근 시간 update

        questionRepository.save(existingQuestion); //객체 저장
    }
}