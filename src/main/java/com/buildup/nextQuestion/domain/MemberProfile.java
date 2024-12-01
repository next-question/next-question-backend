package com.buildup.nextQuestion.domain;

import com.buildup.nextQuestion.domain.enums.Job;
import com.buildup.nextQuestion.domain.enums.Membership;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "member_profile")
public class MemberProfile {

    @Id @GeneratedValue
    @Column(name = "member_profile_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Job job;

    @Enumerated(EnumType.STRING)
    private Membership membership;

    @Column(columnDefinition = "VARCHAR(20)")
    private String phoneNumber;

    @Column(columnDefinition = "TINYINT(1)")
    private boolean gender;

    private int age;

    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;
}
