package com.buildup.nextQuestion.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "multiple_choice")
public class MultipleChoice {

    @Id @GeneratedValue
    @Column(name = "multiple_choice_id")
    private Long id;

    @Column(name = "`option`", columnDefinition = "TEXT")
    private String option;

    @OneToOne
    @JoinColumn(name = "question_id") //Question_id를 fk로 받아옴
    private Question question;
}