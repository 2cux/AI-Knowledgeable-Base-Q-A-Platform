package com.example.aikb.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Extracts the final answer text from raw LLM output.
 */
@Component
@RequiredArgsConstructor
public class AnswerExtractor {

    private final ObjectMapper objectMapper;

    public AnswerExtractResult extract(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.EMPTY_RESPONSE);
        }

        String trimmedResponse = rawResponse.trim();
        if (!looksLikeJson(trimmedResponse)) {
            return AnswerExtractResult.success(trimmedResponse);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(trimmedResponse);
        } catch (JsonProcessingException ex) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.INVALID_JSON);
        }

        if (root == null || root.isNull() || root.isMissingNode()) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.EMPTY_RESPONSE);
        }
        if (root.isTextual()) {
            return answerResult(root.asText(), AnswerExtractFailureReason.EMPTY_ANSWER);
        }
        if (!root.isObject()) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.UNSUPPORTED_STRUCTURE);
        }
        if (root.isEmpty()) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
        }

        AnswerExtractResult answerFieldResult = extractAnswerField(root);
        if (answerFieldResult != null) {
            return answerFieldResult;
        }

        AnswerExtractResult outputTextResult = extractTextField(root, "output_text");
        if (outputTextResult != null) {
            return outputTextResult;
        }

        AnswerExtractResult chatCompletionResult = extractChatCompletionContent(root);
        if (chatCompletionResult != null) {
            return chatCompletionResult;
        }

        AnswerExtractResult responsesOutputResult = extractResponsesOutput(root);
        if (responsesOutputResult != null) {
            return responsesOutputResult;
        }

        return AnswerExtractResult.failure(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
    }

    private AnswerExtractResult extractAnswerField(JsonNode root) {
        if (!root.has("answer")) {
            return null;
        }
        return answerResult(root.path("answer").asText(null), AnswerExtractFailureReason.EMPTY_ANSWER);
    }

    private AnswerExtractResult extractTextField(JsonNode root, String fieldName) {
        if (!root.has(fieldName)) {
            return null;
        }
        return answerResult(root.path(fieldName).asText(null), AnswerExtractFailureReason.EMPTY_ANSWER);
    }

    private AnswerExtractResult extractChatCompletionContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }

        JsonNode content = choices.path(0).path("message").path("content");
        if (content.isMissingNode()) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.UNSUPPORTED_STRUCTURE);
        }
        return answerResult(textFromContent(content), AnswerExtractFailureReason.EMPTY_ANSWER);
    }

    private AnswerExtractResult extractResponsesOutput(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray() || output.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode outputItem : output) {
            appendContentText(outputItem.path("content"), text);
            appendText(outputItem.path("text"), text);
        }
        return answerResult(text.toString(), AnswerExtractFailureReason.EMPTY_ANSWER);
    }

    private void appendContentText(JsonNode content, StringBuilder text) {
        if (content.isMissingNode() || content.isNull()) {
            return;
        }
        if (content.isArray()) {
            for (JsonNode contentItem : content) {
                appendText(contentItem.path("text"), text);
                appendText(contentItem.path("content"), text);
            }
            return;
        }
        appendText(content, text);
    }

    private void appendText(JsonNode node, StringBuilder text) {
        String value = textFromContent(node);
        if (StringUtils.hasText(value)) {
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(value.trim());
        }
    }

    private String textFromContent(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return null;
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                appendText(item.path("text"), text);
                appendText(item.path("content"), text);
            }
            return text.toString();
        }
        return null;
    }

    private AnswerExtractResult answerResult(String answer, AnswerExtractFailureReason emptyReason) {
        if (!StringUtils.hasText(answer)) {
            return AnswerExtractResult.failure(emptyReason);
        }
        return AnswerExtractResult.success(answer.trim());
    }

    private boolean looksLikeJson(String value) {
        return value.startsWith("{") || value.startsWith("[") || value.startsWith("\"")
                || "null".equals(value);
    }
}
