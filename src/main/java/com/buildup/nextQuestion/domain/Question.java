package com.buildup.nextQuestion.domain;

import com.buildup.nextQuestion.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private QuestionType type; //문제 타입

    @Column(name = "opt", columnDefinition = "TEXT")
    private String option;

    private String answer; // 정답
    private Timestamp createTime; // 생성 시간
    private Timestamp recentSolveTime; // 최근 학습 시간

    @Column(columnDefinition = "TINYINT(1)")
    private Boolean wrong; // 오답 여부

    @Column(name = "del", columnDefinition = "TINYINT(1)")
    private Boolean delete; // 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "work_book_id") //문제집에서 id를 fk로 가져옴
    private WorkBook workBook;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member; // 회원 ID
}