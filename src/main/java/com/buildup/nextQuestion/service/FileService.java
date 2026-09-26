package com.buildup.nextQuestion.service;

import com.buildup.nextQuestion.exception.PdfExtractionBusyException;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 업로드한 PDF 에서 텍스트를 뽑는다. 요청 스레드에서 돈다.
 *
 * <p>고친 점은 두 가지다.
 *
 * <p><b>PDF 를 힙에 통째로 올리지 않는다.</b> 예전에는 {@code PDDocument.load(InputStream)} 이라
 * PDFBox 가 문서 전체를 힙 버퍼에 복사하고 추출이 끝날 때까지 들고 있었다. 17.7MB 짜리 PDF 로
 * 힙 512MB 에서 재 보니 동시 10건에 old 영역이 442MB 까지 찼고, 동시 20건에서 OutOfMemoryError 로
 * 서버가 죽었다. 버퍼를 임시 파일로 두면 같은 20건이 old 영역 최대 70MB 안에서 모두 끝났다.
 * {@code PDDocument.load(File)} 도 재 봤는데, 파일을 읽으면서도 스트림 내용을 힙 버퍼로 옮겨서
 * old 영역이 511MB 까지 찼다. 그래서 임시 파일 버퍼를 명시한다. 임시 파일은 {@code java.io.tmpdir}
 * 에 생기므로, 그 경로가 메모리 기반(tmpfs)인 서버라면 이 개선이 힙 밖 메모리로 옮겨갈 뿐이다.
 *
 * <p><b>동시에 추출하는 수를 제한한다.</b> 추출은 CPU 를 쓰는 일이라 코어 수보다 많이 돌려도 빨라지지
 * 않고, 문서 한 건마다 수백 MB 의 짧은 할당을 만든다. 코어 2개에서 20건을 한꺼번에 넣었을 때 제한이
 * 없으면 응답 중간값이 11.7초, 2개로 제한하면 7.8초였다. 자리가 날 때까지 기다리되, 정해진 시간을
 * 넘기면 503 으로 알린다. 톰캣 스레드를 끝없이 붙잡아 두지 않기 위해서다.
 */
@Service
public class FileService {

    private final Semaphore extractionPermits;
    private final long waitMillis;

    public FileService(
            @Value("${pdf.extraction.max-concurrent:0}") int maxConcurrent,
            @Value("${pdf.extraction.wait-ms:10000}") long waitMillis
    ) {
        int permits = maxConcurrent > 0 ? maxConcurrent : Runtime.getRuntime().availableProcessors();
        // 먼저 온 요청이 먼저 들어가게 한다. 공정하지 않으면 늦게 온 요청이 앞질러 누군가는 계속 밀린다
        this.extractionPermits = new Semaphore(permits, true);
        this.waitMillis = waitMillis;
    }

    public String extractTextFromPDF(MultipartFile file) throws IOException {
        acquirePermit();
        try (InputStream in = file.getInputStream();
             PDDocument document = PDDocument.load(in, MemoryUsageSetting.setupTempFileOnly())) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            return pdfTextStripper.getText(document);
        } finally {
            extractionPermits.release();
        }
    }

    private void acquirePermit() {
        try {
            if (!extractionPermits.tryAcquire(waitMillis, TimeUnit.MILLISECONDS)) {
                throw new PdfExtractionBusyException("지금은 PDF 처리 요청이 밀려 있습니다. 잠시 후 다시 시도해 주세요.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PdfExtractionBusyException("PDF 처리 대기 중 요청이 중단됐습니다.");
        }
    }

    public void validateFile(MultipartFile file){
        if (file == null) {
            throw new IllegalArgumentException("PDF file is required.");
        }

        // 파일 형식이 PDF가 아닌 경우
        if (!file.getOriginalFilename().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }
    }

}
