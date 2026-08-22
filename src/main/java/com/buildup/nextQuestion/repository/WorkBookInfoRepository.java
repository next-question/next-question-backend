package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import com.buildup.nextQuestion.repository.projection.WorkBookQuestionCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkBookInfoRepository extends JpaRepository<WorkBookInfo, Long> {


    Optional<WorkBookInfo> findByWorkBookIdAndQuestionInfoId(Long workBookInfoId, Long questionId);

    boolean existsByWorkBookIdAndQuestionInfoId(Long targetWorkbookId, Long id);

    List<WorkBookInfo> findAllByWorkBookId(Long decryptedId);

    WorkBookInfo findByWorkBookInAndQuestionInfoId(List<WorkBook> workBooks, Long questionInfoId);

    /**
     * 문제집별로 삭제되지 않은 문제 수를 한 번에 센다.
     *
     * <p>work_book_info 와 question 사이에는 연관관계가 없고 question_info 를 사이에 두고
     * 이어진다. 그래서 두 엔티티를 question_info_id 로 직접 조인한다(암묵적 inner join).
     *
     * <p>문제가 하나도 없는 문제집은 결과에 포함되지 않는다 — 호출부가 0으로 처리한다.
     */
    @Query("""
            select wbi.workBook.id as workBookId, count(q.id) as questionCount
            from WorkBookInfo wbi, Question q
            where q.questionInfo.id = wbi.questionInfo.id
              and q.member.id = :memberId
              and q.del = false
              and wbi.workBook.id in :workBookIds
            group by wbi.workBook.id
            """)
    List<WorkBookQuestionCount> countActiveQuestionsByWorkBookIds(@Param("memberId") Long memberId,
                                                                  @Param("workBookIds") List<Long> workBookIds);
}
