package com.buildup.nextQuestion.dto.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileByGuestRequest {
    private MultipartFile file;
    private Boolean multiple;
    private Boolean ox;
    private Boolean blank;
}
