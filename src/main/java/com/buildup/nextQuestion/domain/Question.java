package com.buildup.nextQuestion.domain;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Getter @Setter
@Table(name = "question")
public class Question {

    @Id @GeneratedValue
    @Column(name = "question_id")
    private Long id;

    @Column(columnDefinition = "VARCHAR(100)")
    private String name; // 문제 이름

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private QuestionType type; //문제 타입

    @Column(name = "opt", columnDefinition = "TEXT")
    private String option;

    @JsonSetter("opt")
    public void setOpt(String opt) {
        // MULTIPLE_CHOICE일 때만 opt 저장
        if (this.type == QuestionType.MULTIPLE_CHOICE) {
            this.option = opt;
        } else {
            this.option = null;
        }
    }


    private String answer; // 정답

    @CreationTimestamp
    private Timestamp createTime; // 생성 시간



}