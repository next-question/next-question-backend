package com.buildup.nextQuestion.repository;

import com.buildup.nextQuestion.domain.WorkBook;
import com.buildup.nextQuestion.domain.WorkBookInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkBookInfoRepository extends JpaRepository<WorkBookInfo, Long> {


    Optional<WorkBookInfo> findByWorkBookIdAndQuestionInfoId(Long workBookInfoId, Long questionId);

    boolean existsByWorkBookIdAndQuestionInfoId(Long targetWorkbookId, Long id);

    List<WorkBookInfo> findAllByWorkBookId(Long decryptedId);

    WorkBookInfo findByWorkBookInAndQuestionInfoId(List<WorkBook> workBooks, Long questionInfoId);
}
