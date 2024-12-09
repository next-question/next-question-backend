package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter @Setter
public class QuestionInfoByMember {

    @Id @GeneratedValue
    private Long id;

    private Timestamp recentSolveTime; // 최근 학습 시간

    @Column(columnDefinition = "TINYINT(1)")
    private Boolean wrong; // 오답 여부

    @Column(name = "del", columnDefinition = "TINYINT(1)")
    private Boolean delete; // 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "question_id")
    private Question question;
}
