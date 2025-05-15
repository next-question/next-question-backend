package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Statistics")
public class Statistics {
    @Id
    @GeneratedValue
    @Column(name = "Statistics_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private int Streak;

    private int maxStreak;

}
