package com.buildup.nextQuestion.exception;

/**
 * PDF 텍스트 추출 자리가 정해진 시간 안에 나지 않았을 때. 서버 잘못이 아니라 지금 붐빈다는 뜻이라
 * 응답은 503 이다.
 */
public class PdfExtractionBusyException extends RuntimeException {

    public PdfExtractionBusyException(String message) {
        super(message);
    }
}
