package com.buildup.nextQuestion.service;


import com.buildup.nextQuestion.dto.question.UploadFileByGuestRequest;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberRequest;
import com.buildup.nextQuestion.dto.question.UploadFileByMemberResponse;
import com.buildup.nextQuestion.service.question.DocumentChunker;
import com.buildup.nextQuestion.service.question.QuestionTypeCounts;
import com.buildup.nextQuestion.service.question.QuestionTypeDistributor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionGenerationFacade {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationFacade.class);

    /** 비회원에게 제공하는 맛보기 문제 수. */
    private static final int GUEST_QUESTION_COUNT = 5;

    private final FileService fileService;
    private final GPTService gptService;
    private final QuestionService questionService;
    private final QuestionTypeDistributor typeDistributor;
    private final DocumentChunker documentChunker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 한 번의 요청에 실어 보낼 문서 글자 수 상한.
     *
     * <p>예전에는 PDF 에서 뽑은 텍스트를 통째로 프롬프트에 붙였다. 문서가 길면 모델 입력 한도를
     * 넘겨 요청이 통째로 실패했고, 넘지 않더라도 입력 토큰이 페이지 수에 그대로 비례해 늘었다.
     * 지금은 이 값으로 잘라 나눠 보낸다.
     */
    @Value("${openai.max-input-chars:40000}")
    private int maxInputChars;

    public JsonNode generateQuestionByGuest(UploadFileByGuestRequest request) throws IOException {
        String content = fileService.extractTextFromPDF(request.getFile());

        QuestionTypeCounts counts = typeDistributor.distribute(
                GUEST_QUESTION_COUNT, request.getOx(), request.getMultiple(), request.getBlank());

        JsonNode response = generate(content, counts);

        // 비회원 응답에는 난이도를 내리지 않는다
        if (response.has("questions")) {
            for (JsonNode question : response.get("questions")) {
                ((ObjectNode) question).remove("level");
            }
        }
        return response;
    }

    public List<UploadFileByMemberResponse> generateQuestionByMember(UploadFileByMemberRequest request) throws Exception {
        String content = fileService.extractTextFromPDF(request.getFile());

        QuestionTypeCounts counts = typeDistributor.distribute(
                request.getQuestionCount(), request.getOx(), request.getMultiple(), request.getBlank());

        return questionService.saveAll(generate(content, counts));
    }

    /**
     * 문서를 상한 크기로 잘라 조각마다 문제를 만들고 하나로 합친다.
     *
     * <p>조각이 하나면 예전과 똑같이 요청 한 번으로 끝난다. 조각이 여럿일 때만 나눠 보낸다.
     */
    private JsonNode generate(String content, QuestionTypeCounts counts) {
        List<DocumentChunker.Chunk> chunks = documentChunker.chunk(content, counts, maxInputChars);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("문서에서 문제를 만들 만한 내용을 찾지 못했습니다.");
        }
        if (chunks.size() == 1) {
            DocumentChunker.Chunk only = chunks.get(0);
            return gptService.requestFunctionCalling(
                    only.text(), only.counts().multiple(), only.counts().blank(), only.counts().ox());
        }

        log.info("문서가 길어 {}조각으로 나눠 생성한다 (조각당 최대 {}자)", chunks.size(), maxInputChars);

        ArrayNode merged = objectMapper.createArrayNode();
        for (DocumentChunker.Chunk chunk : chunks) {
            JsonNode partial = gptService.requestFunctionCalling(
                    chunk.text(), chunk.counts().multiple(), chunk.counts().blank(), chunk.counts().ox());
            if (partial.has("questions")) {
                merged.addAll((ArrayNode) partial.get("questions"));
            }
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("questions", merged);
        return result;
    }
}
