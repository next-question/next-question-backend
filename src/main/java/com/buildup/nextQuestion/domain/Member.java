package com.buildup.nextQuestion.domain;

import com.buildup.nextQuestion.domain.enums.LoginType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "member")
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @Column(columnDefinition = "VARCHAR(10)")
    private String nickname;

    @Enumerated(EnumType.STRING)
    private LoginType logintype;

    public Member(String nickname, LoginType logintype) {
        this.nickname = nickname;
        this.logintype = logintype;
    }


}
