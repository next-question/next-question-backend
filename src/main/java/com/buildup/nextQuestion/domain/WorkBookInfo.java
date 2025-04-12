package com.buildup.nextQuestion.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter@Setter
@NoArgsConstructor
@Table(name = "work_book_info")
public class WorkBookInfo {

    @Id@GeneratedValue
    @Column(name="work_book_info_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_info_id")
    private QuestionInfo questionInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_book_id")
    private WorkBook workBook;

    public WorkBookInfo(QuestionInfo questionInfo, WorkBook workBook) {
        this.questionInfo = questionInfo;
        this.workBook = workBook;

    }
}
