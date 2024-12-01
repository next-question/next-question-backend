package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter @Setter
@Table(name = "work_book")
public class WorkBook {

    @Id @GeneratedValue
    @Column(name = "work_book_id")
    private Long id;

    private Timestamp recentSolveDate; // 문제집 최근 학습일

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member;
}