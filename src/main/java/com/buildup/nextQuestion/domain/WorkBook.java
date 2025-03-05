package com.buildup.nextQuestion.domain;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter@Setter
@NoArgsConstructor
@Table(name = "work_book")
public class WorkBook {

    @Id@GeneratedValue
    @Column(name="work_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "work_book_info_id")
    private WorkBookInfo workBookInfo;

    public WorkBook(Question question, WorkBookInfo workBookInfo) {
        this.question = question;
        this.workBookInfo = workBookInfo;

    }
}
