package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "local_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id"),
        @UniqueConstraint(columnNames = "email")
})
public class LocalMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "local_member_id")
    private Long id;

    @Column(nullable = false, length = 15)
    private String userId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 40)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private LocalDate createDate;

    public LocalMember(String userId, String password, String email, Member member) {
        this.userId = userId;
        this.password = password;
        this.email = email;
        this.member = member;
        this.createDate = LocalDate.now();
    }
}
