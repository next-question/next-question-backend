package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Question {

    @Id @GeneratedValue
    @Column(name = "question_id")
    private Long id;

    private Boolean wrong; // 오답 여부

    private Boolean del; // 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "question_info_id", nullable = false)
    private QuestionInfo questionInfo;

    private Timestamp recentSolveTime; // 최근 학습 시간

    private LocalTime createTime; // 생성 시간

    public Question(Member member, QuestionInfo questionInfo) {
        this.wrong = false;
        this.del = false;
        this.member = member;
        this.questionInfo = questionInfo;
        this.recentSolveTime = null;
        this.createTime = LocalTime.now();

    }
}
