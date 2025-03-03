package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter @Setter
@Table(name = "work_book_info")
public class WorkBookInfo {

    @Id @GeneratedValue
    @Column(name = "work_book_info_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member;

    private String name;

    private Timestamp recentSolveDate; // 문제집 최근 학습일


}