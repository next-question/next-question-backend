package com.buildup.nextQuestion.performance;

import com.buildup.nextQuestion.domain.enums.Role;
import com.buildup.nextQuestion.utility.JwtUtility;
import com.sun.net.httpserver.HttpServer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문제 생성이 몰릴 때 <b>다른 요청이 얼마나 기다리는지</b> 잰다.
 *
 * <p>예전에는 업로드부터 PDF 추출, GPT 호출, 저장까지가 한 HTTP 요청 안에서 동기로 돌았다.
 * GPT 응답을 기다리는 내내 톰캣 요청 스레드가 묶여 있었고, 생성이 몇 건만 몰려도 그 뒤에 온
 * 평범한 조회 요청까지 큐에서 기다렸다. 지금은 생성을 백그라운드 작업으로 떼어내고 전용
 * 스레드 풀에 맡긴다.
 *
 * <p><b>재는 것은 두 가지다.</b>
 * <ul>
 *   <li>생성 요청 하나가 응답을 받기까지 걸린 시간. 사용자가 화면 앞에서 기다리는 시간이다.</li>
 *   <li>생성이 몰린 동안 작업 상태 조회 API 의 응답 시간. 생성을 맡긴 사용자가 진행 상황을
 *       보려고 실제로 부르는 경로다.</li>
 * </ul>
 *
 * <p><b>측정 조건.</b> OpenAI 자리에는 {@value #GPT_DELAY_MS}ms 뒤에 정상 응답을 주는 로컬
 * 스텁을 둔다. 실제 생성은 이보다 훨씬 오래 걸리므로 아래 수치는 하한이다. 톰캣 스레드는
 * {@value #TOMCAT_THREADS}개로 줄여 포화 지점을 앞당겼다. 운영 기본값은 200 이라, 같은 일이
 * 벌어지는 지점이 옮겨갈 뿐 구조는 같다.
 *
 * <pre>
 * ./gradlew test --tests "*GenerationConcurrencyCostTest"
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GenerationConcurrencyCostTest {

    private static final Logger log = LoggerFactory.getLogger(GenerationConcurrencyCostTest.class);

    /**
     * GPT 한 번 부르는 데 걸리는 시간. 실제로는 이보다 훨씬 길다.
     * {@code -Dbench.gptDelayMs=10000} 으로 바꿔 돌리면 이전 값은 따라 늘고 현재 값은
     * 그대로인지 확인할 수 있다.
     */
    private static final int GPT_DELAY_MS =
            Integer.getInteger("bench.gptDelayMs", 3000);
    /** 톰캣 요청 스레드 수. 운영 기본값 200 을 줄여 포화 지점을 앞당긴다. */
    private static final int TOMCAT_THREADS = 10;
    /** 동시에 생성을 요청하는 사람 수. 스레드 수보다 많아야 큐가 생긴다. */
    private static final int CONCURRENT = 16;

    private static final int PING_SAMPLES = 12;
    private static final long PING_INTERVAL_MS = 150;

    private static final String SYNC_PATH = "/member/questions/upload";
    private static final String ASYNC_PATH = "/member/questions/upload/async";
    /** 생성을 맡긴 사용자가 진행 상황을 보려고 부르는 경로. 메모리 조회라 원래는 즉시 끝난다. */
    private static final String STATUS_PATH = "/member/questions/upload/async/none";

    private static final String BOUNDARY = "----GenerationConcurrencyBoundary";
    private static final String GPT_BODY =
            "{\"choices\":[{\"message\":{\"function_call\":{\"arguments\":\"{\\\"questions\\\":[]}\"}}}]}";

    private static HttpServer gptStub;
    private static byte[] pdf;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtility jwtUtility;

    private String token;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        startGptStub();
        registry.add("openai.api.url", () -> "http://127.0.0.1:" + gptStub.getAddress().getPort() + "/v1/chat");
        // 테스트 기본값은 2초라 스텁 지연에 걸려 재시도만 돈다. 운영값은 120초다.
        registry.add("openai.timeout.read-ms", () -> "30000");
        registry.add("server.tomcat.threads.max", () -> String.valueOf(TOMCAT_THREADS));
        registry.add("server.tomcat.threads.min-spare", () -> String.valueOf(TOMCAT_THREADS));
    }

    static void startGptStub() throws IOException {
        if (gptStub != null) return;
        gptStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        gptStub.createContext("/", exchange -> {
            drain(exchange.getRequestBody());
            try {
                Thread.sleep(GPT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = GPT_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        gptStub.setExecutor(Executors.newCachedThreadPool());
        gptStub.start();
    }

    @BeforeAll
    static void buildPdf() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream out = new PDPageContentStream(doc, page)) {
                out.beginText();
                out.setFont(PDType1Font.HELVETICA, 12);
                out.newLineAtOffset(50, 700);
                out.showText("benchmark document for question generation");
                out.endText();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            doc.save(bytes);
            pdf = bytes.toByteArray();
        }
    }

    @AfterAll
    static void stopGptStub() {
        if (gptStub != null) gptStub.stop(0);
    }

    @Test
    @DisplayName("생성이 몰릴 때 사용자가 기다리는 시간")
    void waitingTimeUnderGenerationBurst() throws Exception {
        token = "Bearer " + jwtUtility.generateToken("bench", Role.MEMBER);

        long syncOne = timeUpload(SYNC_PATH);
        long asyncOne = timeUpload(ASYNC_PATH);

        long[] idle = measureStatusLatency(null);
        long[] duringSync = measureStatusLatency(SYNC_PATH);
        long[] duringAsync = measureStatusLatency(ASYNC_PATH);

        log.info(report(syncOne, asyncOne, idle, duringSync, duringAsync));

        // 동기 경로는 GPT 응답을 요청 스레드에서 기다린다.
        assertThat(syncOne).isGreaterThan(GPT_DELAY_MS * 1_000_000L);
        // 비동기 경로는 PDF 추출과 검증만 하고 돌려준다.
        assertThat(asyncOne).isLessThan(syncOne / 5);
        // 생성이 몰려도 조회는 밀리지 않는다.
        assertThat(percentile(duringAsync, 95)).isLessThan(percentile(duringSync, 50));
    }

    // --- 측정 ---------------------------------------------------------------

    private long timeUpload(String path) throws Exception {
        HttpClient client = newClient();
        long start = System.nanoTime();
        HttpResponse<String> response = client.send(uploadRequest(path), HttpResponse.BodyHandlers.ofString());
        long elapsed = System.nanoTime() - start;
        assertThat(response.statusCode()).isIn(200, 202);
        return elapsed;
    }

    /** 작업 상태 조회의 응답 시간을 표본 추출한다. path 가 주어지면 그 경로로 부하를 걸어 둔다. */
    private long[] measureStatusLatency(String burstPath) throws Exception {
        // 앞 단계에서 서버가 잡고 있던 생성이 다 빠질 때까지 기다린다. 클라이언트를 끊어도
        // 서버는 진행 중인 요청을 마저 처리하므로, 바로 다음 측정을 시작하면 섞인다.
        settle();
        Burst burst = burstPath == null ? null : startBurst(burstPath);
        try {
            if (burst != null) {
                Thread.sleep(700);
            }
            HttpClient client = newClient();
            // 첫 표본이 커넥션 수립과 클래스 로딩을 같이 재지 않도록 한 번 버린다.
            client.send(statusRequest(), HttpResponse.BodyHandlers.discarding());
            long[] samples = new long[PING_SAMPLES];
            for (int i = 0; i < PING_SAMPLES; i++) {
                long start = System.nanoTime();
                client.send(statusRequest(), HttpResponse.BodyHandlers.discarding());
                samples[i] = System.nanoTime() - start;
                Thread.sleep(PING_INTERVAL_MS);
            }
            return samples;
        } finally {
            if (burst != null) burst.stop();
        }
    }

    private HttpRequest statusRequest() {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + STATUS_PATH))
                .header("Authorization", token)
                .GET().build();
    }

    /** 서버에 남은 생성이 끝날 때까지 기다린다. */
    private void settle() throws InterruptedException {
        Thread.sleep(GPT_DELAY_MS + 2000L);
    }

    private Burst startBurst(String path) {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT);
        AtomicBoolean keepGoing = new AtomicBoolean(true);
        CountDownLatch started = new CountDownLatch(CONCURRENT);

        for (int i = 0; i < CONCURRENT; i++) {
            pool.submit(() -> {
                started.countDown();
                HttpClient client = newClient();
                while (keepGoing.get()) {
                    // 두 경로에 같은 속도로 건다. 그냥 반복하면 즉시 돌아오는 비동기 쪽에만
                    // 훨씬 많은 요청이 들어가 비교가 성립하지 않는다. 사람 한 명이 생성 한 건을
                    // 맡기고 결과를 기다리는 주기를 모사한다.
                    long start = System.nanoTime();
                    try {
                        client.send(uploadRequest(path), HttpResponse.BodyHandlers.discarding());
                    } catch (Exception e) {
                        return;
                    }
                    long spentMs = (System.nanoTime() - start) / 1_000_000L;
                    long waitMs = GPT_DELAY_MS - spentMs;
                    if (waitMs > 0) {
                        try {
                            Thread.sleep(waitMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            });
        }
        try {
            started.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return () -> {
            keepGoing.set(false);
            pool.shutdownNow();
            pool.awaitTermination(60, TimeUnit.SECONDS);
        };
    }

    // --- 요청 만들기 ---------------------------------------------------------

    private HttpClient newClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    private HttpRequest uploadRequest(String path) {
        byte[] body = multipartBody();
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", token)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private byte[] multipartBody() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"bench.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(pdf);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            for (String[] field : new String[][]{
                    {"questionCount", "5"}, {"multiple", "true"}, {"ox", "true"}, {"blank", "true"}}) {
                out.write(("--" + BOUNDARY + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + field[0] + "\"\r\n\r\n"
                        + field[1] + "\r\n").getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    // --- 리포트 -------------------------------------------------------------

    private String report(long syncOne, long asyncOne, long[] idle, long[] sync, long[] async) {
        String bar = "──────────────────────────────────────────────────────────────";
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(bar);
        lines.add(" 생성이 몰릴 때 사용자가 기다리는 시간");
        lines.add(bar);
        lines.add(String.format("  %-30s %14s %14s", "", "동기(이전)", "작업 분리(현재)"));
        lines.add(String.format("  %-30s %14s %14s", "생성 요청 응답까지", ms(syncOne), ms(asyncOne)));
        lines.add(String.format("  %-30s %14s %14s", "작업 상태 조회 p50",
                ms(percentile(sync, 50)), ms(percentile(async, 50))));
        lines.add(String.format("  %-30s %14s %14s", "작업 상태 조회 p95",
                ms(percentile(sync, 95)), ms(percentile(async, 95))));
        lines.add(bar);
        lines.add(String.format("  생성 없을 때 조회 p50 %s, p95 %s",
                ms(percentile(idle, 50)), ms(percentile(idle, 95))));
        lines.add(String.format("  GPT 스텁 지연 %dms, 생성 %d건을 %dms 주기로, 톰캣 스레드 %d개, 생성 전용 풀 기본값.",
                GPT_DELAY_MS, CONCURRENT, GPT_DELAY_MS, TOMCAT_THREADS));
        lines.add("  실제 생성은 스텁보다 오래 걸리므로 위 수치는 하한이다.");
        lines.add(bar);
        return String.join(System.lineSeparator(), lines);
    }

    private static String ms(long nanos) {
        return String.format("%.1f ms", nanos / 1_000_000.0);
    }

    private static long percentile(long[] values, int p) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    private static long drain(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) total += read;
        return total;
    }

    @FunctionalInterface
    private interface Burst {
        void stop() throws InterruptedException;
    }
}
