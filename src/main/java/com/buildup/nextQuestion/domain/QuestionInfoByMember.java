package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class QuestionInfoByMember {

    @Id @GeneratedValue
    private Long id;

    private Boolean wrong; // 오답 여부

    private Boolean del; // 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private Timestamp recentSolveTime; // 최근 학습 시간

    private LocalTime createTime; // 생성 시간

    public QuestionInfoByMember(Member member, Question question) {
        this.wrong = false;
        this.del = false;
        this.member = member;
        this.question = question;
        this.recentSolveTime = null;
        this.createTime = LocalTime.now();

    }
}
