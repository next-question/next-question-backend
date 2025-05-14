package com.buildup.nextQuestion.mapper;

import com.buildup.nextQuestion.domain.Question;
import com.buildup.nextQuestion.domain.QuestionInfo;
import com.buildup.nextQuestion.dto.question.MemberQuestionInfoDto;
import com.buildup.nextQuestion.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionMapper {

    private final EncryptionService encryptionService;

    public List<MemberQuestionInfoDto> memberQuestionInfoMapper(List<Question> questions) {
        return questions.stream()
                .map(question -> {
                    QuestionInfo info = question.getQuestionInfo();

                    if (info == null) {
                        throw new IllegalStateException("QuestionInfo is null for Question ID: " + question.getId());
                    }

                    try {
                        return new MemberQuestionInfoDto(
                                encryptionService.encryptPrimaryKey(question.getId()),          // encryptedQuestionId
                                info.getName(),                                                 // name
                                info.getType(),                                                 // type
                                info.getAnswer(),                                               // answer
                                info.getOption(),                                               // opt
                                question.getCreateTime()                                     // createTime (Timestamp or LocalDateTime)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Encryption failed for Question ID: " + question.getId(), e);
                    }
                })
                .toList();
    }
}
