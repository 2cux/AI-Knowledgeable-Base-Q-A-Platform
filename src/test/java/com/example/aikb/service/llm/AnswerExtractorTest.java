package com.example.aikb.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnswerExtractorTest {

    private AnswerExtractor answerExtractor;

    @BeforeEach
    void setUp() {
        answerExtractor = new AnswerExtractor(new ObjectMapper());
    }

    @Test
    void shouldExtractAnswerField() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":\"xxx\"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractDataAnswerField() {
        AnswerExtractResult result = answerExtractor.extract("{\"data\":{\"answer\":\"xxx\"}}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractTopLevelContentText() {
        AnswerExtractResult result = answerExtractor.extract("{\"content\":[{\"type\":\"text\",\"text\":\"xxx\"}]}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractChatCompletionMessageContent() {
        AnswerExtractResult result = answerExtractor.extract("{\"choices\":[{\"message\":{\"content\":\"xxx\"}}]}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractOutputTextField() {
        AnswerExtractResult result = answerExtractor.extract("{\"output_text\":\"xxx\"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractTextField() {
        AnswerExtractResult result = answerExtractor.extract("{\"text\":\"xxx\"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldExtractPlainText() {
        AnswerExtractResult result = answerExtractor.extract("  xxx  ");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("xxx");
    }

    @Test
    void shouldReturnEmptyContentForBlankInput() {
        AnswerExtractResult result = answerExtractor.extract("   ");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.EMPTY_CONTENT);
    }

    @Test
    void shouldReturnMissingAnswerFieldForEmptyJsonObject() {
        AnswerExtractResult result = answerExtractor.extract("{}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
    }

    @Test
    void shouldReturnInvalidJsonForMalformedJsonText() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":\"xxx\"");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.INVALID_JSON);
    }

    @Test
    void shouldReturnInvalidJsonForMalformedNonEmptyJsonLikeText() {
        AnswerExtractResult result = answerExtractor.extract("{answer:xxx}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.INVALID_JSON);
    }

    @Test
    void shouldReturnEmptyContentForEmptyAnswerField() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":\"   \"}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.EMPTY_CONTENT);
    }

    @Test
    void shouldExtractResponsesOutputArrayText() {
        String rawResponse = """
                {
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "first part"
                        },
                        {
                          "type": "output_text",
                          "text": "second part"
                        }
                      ]
                    }
                  ]
                }
                """;

        AnswerExtractResult result = answerExtractor.extract(rawResponse);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("first part" + System.lineSeparator() + "second part");
    }
}
