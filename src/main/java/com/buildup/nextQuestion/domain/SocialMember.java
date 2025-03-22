package com.buildup.nextQuestion.domain;

import com.buildup.nextQuestion.domain.enums.LoginType;
import com.buildup.nextQuestion.domain.enums.Role;
import com.buildup.nextQuestion.domain.enums.SocialType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "social_member")
public class SocialMember {

    @Id @GeneratedValue
    @Column(name = "social_member_id")
    private Long id;

    @Column(columnDefinition = "VARCHAR(100)")
    private String snsId;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member;

    public SocialMember(String snsId, SocialType socialType, Member member) {
        this.snsId = snsId;
        this.socialType = socialType;
        this.member = member;
    }

    public SocialMember() {
    }
}
