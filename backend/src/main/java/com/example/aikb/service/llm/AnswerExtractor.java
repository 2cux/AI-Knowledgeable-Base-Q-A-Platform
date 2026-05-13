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

    private static final String FIELD_ANSWER = "answer";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_CHOICES = "choices";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_OUTPUT_TEXT = "output_text";
    private static final String FIELD_TEXT = "text";

    private final ObjectMapper objectMapper;

    public AnswerExtractResult extract(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.EMPTY_CONTENT);
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
            return AnswerExtractResult.failure(AnswerExtractFailureReason.EMPTY_CONTENT);
        }
        if (root.isTextual()) {
            return answerResult(root.asText());
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

        AnswerExtractResult dataAnswerResult = extractDataAnswerField(root);
        if (dataAnswerResult != null) {
            return dataAnswerResult;
        }

        AnswerExtractResult contentResult = extractContent(root);
        if (contentResult != null) {
            return contentResult;
        }

        AnswerExtractResult chatCompletionResult = extractChatCompletionContent(root);
        if (chatCompletionResult != null) {
            return chatCompletionResult;
        }

        AnswerExtractResult outputTextResult = extractTextField(root, FIELD_OUTPUT_TEXT);
        if (outputTextResult != null) {
            return outputTextResult;
        }

        AnswerExtractResult textResult = extractTextField(root, FIELD_TEXT);
        if (textResult != null) {
            return textResult;
        }

        AnswerExtractResult responsesOutputResult = extractResponsesOutput(root);
        if (responsesOutputResult != null) {
            return responsesOutputResult;
        }

        return AnswerExtractResult.failure(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
    }

    private AnswerExtractResult extractAnswerField(JsonNode root) {
        if (!root.has(FIELD_ANSWER)) {
            return null;
        }
        return answerResult(root.path(FIELD_ANSWER).asText(null));
    }

    private AnswerExtractResult extractDataAnswerField(JsonNode root) {
        JsonNode data = root.path(FIELD_DATA);
        if (!data.isObject() || !data.has(FIELD_ANSWER)) {
            return null;
        }
        return answerResult(data.path(FIELD_ANSWER).asText(null));
    }

    private AnswerExtractResult extractTextField(JsonNode root, String fieldName) {
        if (!root.has(fieldName)) {
            return null;
        }
        return answerResult(root.path(fieldName).asText(null));
    }

    private AnswerExtractResult extractContent(JsonNode root) {
        if (!root.has(FIELD_CONTENT)) {
            return null;
        }
        String text = textFromContent(root.path(FIELD_CONTENT));
        if (!StringUtils.hasText(text)) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.MISSING_CONTENT);
        }
        return answerResult(text);
    }

    private AnswerExtractResult extractChatCompletionContent(JsonNode root) {
        JsonNode choices = root.path(FIELD_CHOICES);
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }

        JsonNode content = choices.path(0).path(FIELD_MESSAGE).path(FIELD_CONTENT);
        if (content.isMissingNode()) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
        }
        return answerResult(textFromContent(content));
    }

    private AnswerExtractResult extractResponsesOutput(JsonNode root) {
        JsonNode output = root.path(FIELD_OUTPUT);
        if (!output.isArray() || output.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode outputItem : output) {
            appendContentText(outputItem.path(FIELD_CONTENT), text);
            appendText(outputItem.path(FIELD_TEXT), text);
        }
        return answerResult(text.toString());
    }

    private void appendContentText(JsonNode content, StringBuilder text) {
        if (content.isMissingNode() || content.isNull()) {
            return;
        }
        if (content.isArray()) {
            for (JsonNode contentItem : content) {
                appendText(contentItem.path(FIELD_TEXT), text);
                appendText(contentItem.path(FIELD_CONTENT), text);
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
                appendText(item.path(FIELD_TEXT), text);
                appendText(item.path(FIELD_CONTENT), text);
            }
            return text.toString();
        }
        return null;
    }

    private AnswerExtractResult answerResult(String answer) {
        if (!StringUtils.hasText(answer)) {
            return AnswerExtractResult.failure(AnswerExtractFailureReason.EMPTY_CONTENT);
        }
        return AnswerExtractResult.success(answer.trim());
    }

    private boolean looksLikeJson(String value) {
        return value.startsWith("{") || value.startsWith("[") || value.startsWith("\"")
                || "null".equals(value);
    }

}
