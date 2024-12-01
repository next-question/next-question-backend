package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "local_member")
public class LocalMember {

    @Id @GeneratedValue
    @Column(name = "local_member_id")
    private Long id;

    @Column(columnDefinition = "VARCHAR(15)")
    private String user_id;

    @Column(columnDefinition = "VARCHAR(20)")
    private String pwd;

    @Column(columnDefinition = "VARCHAR(40)")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    private Member member;

}
