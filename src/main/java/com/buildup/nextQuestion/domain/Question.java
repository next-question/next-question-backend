package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Question {

    @Id @GeneratedValue
    @Column(name = "question_id")
    private Long id;

    private Boolean wrong; // 오답 여부

    private Boolean del; // 삭제 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_info_id", nullable = false)
    private QuestionInfo questionInfo;

    private Timestamp recentSolveTime; // 최근 학습 시간

    private LocalTime createTime; // 생성 시간

    private LocalDate assignedDate; // 일일 문제 제공 날짜

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoryInfo> historyInfos;

    public Question(Member member, QuestionInfo questionInfo) {
        this.wrong = false;
        this.del = false;
        this.member = member;
        this.questionInfo = questionInfo;
        this.recentSolveTime = null;
        this.createTime = LocalTime.now();
        this.assignedDate = null;

    }
}
