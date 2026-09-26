package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.exception.GlobalExceptionHandler;
import com.buildup.nextQuestion.exception.PdfExtractionBusyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PDF 텍스트 추출.
 *
 * <p>예전에는 PDF 를 힙에 통째로 올리고 동시 실행 수에도 제한이 없어, 큰 PDF 가 몰리면 서버가
 * OutOfMemoryError 로 죽었다. 지금은 임시 파일 버퍼로 읽고 동시에 도는 수를 제한한다.
 * 여기서 확인하는 것은 세 가지다 — 텍스트는 예전처럼 나오는가, 자리가 없으면 기다리다 거절하는가,
 * 실패한 요청이 자리를 돌려주는가. 마지막이 빠지면 깨진 PDF 몇 건에 추출이 영영 막힌다.
 */
class FileServiceTest {

    @Test
    @DisplayName("임시 파일 버퍼로 읽어도 텍스트는 그대로 나온다")
    void extractsText() throws Exception {
        FileService fileService = new FileService(1, 1000);

        String text = fileService.extractTextFromPDF(pdf("operating system memory"));

        assertThat(text).contains("operating system memory");
    }

    @Test
    @DisplayName("자리가 모두 차 있으면 기다리다가 거절한다")
    void rejectsWhenAllPermitsAreBusy() throws Exception {
        FileService fileService = new FileService(1, 100);
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        byte[] bytes = pdfBytes("slow");

        // 첫 요청은 파일을 읽는 도중에 멈춰 자리를 쥐고 있다
        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
            try {
                return fileService.extractTextFromPDF(blockingFile(bytes, reading, release));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();

        long started = System.nanoTime();
        assertThatThrownBy(() -> fileService.extractTextFromPDF(pdf("second")))
                .isInstanceOf(PdfExtractionBusyException.class);
        long waitedMillis = (System.nanoTime() - started) / 1_000_000;
        assertThat(waitedMillis).as("정해진 시간만큼만 기다린다").isBetween(80L, 2000L);

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).contains("slow");
    }

    @Test
    @DisplayName("깨진 PDF 로 실패해도 자리를 돌려준다")
    void releasesPermitOnFailure() throws Exception {
        FileService fileService = new FileService(1, 100);
        MockMultipartFile broken = new MockMultipartFile("file", "broken.pdf", "application/pdf", "not a pdf".getBytes());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> fileService.extractTextFromPDF(broken)).isInstanceOf(IOException.class);
        }

        assertThat(fileService.extractTextFromPDF(pdf("after failures"))).contains("after failures");
    }

    @Test
    @DisplayName("거절은 503 과 재시도 시점으로 내려간다")
    void busyMapsTo503() {
        ResponseEntity<Map<String, Object>> response = new GlobalExceptionHandler()
                .handlePdfExtractionBusy(new PdfExtractionBusyException("busy"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("5");
        assertThat(response.getBody()).containsEntry("status", 503);
    }

    private static MockMultipartFile pdf(String text) throws IOException {
        return new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfBytes(text));
    }

    private static byte[] pdfBytes(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    /** 읽기 시작하면 풀어줄 때까지 멈추는 업로드 파일. 추출 자리를 쥐고 있는 요청을 만든다. */
    private static MockMultipartFile blockingFile(byte[] bytes, CountDownLatch reading, CountDownLatch release) {
        return new MockMultipartFile("file", "slow.pdf", "application/pdf", bytes) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes) {
                    private boolean first = true;

                    @Override
                    public synchronized int read(byte[] b, int off, int len) {
                        if (first) {
                            first = false;
                            reading.countDown();
                            try {
                                release.await(5, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        return super.read(b, off, len);
                    }
                };
            }
        };
    }
}
