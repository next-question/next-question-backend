package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileByMemberRequest {
    private MultipartFile file;
    private Integer questionCount;
    private Boolean multiple;
    private Boolean ox;
    private Boolean blank;
}
