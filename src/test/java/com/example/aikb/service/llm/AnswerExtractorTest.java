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
    void shouldExtractPlainText() {
        AnswerExtractResult result = answerExtractor.extract("  plain answer  ");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("plain answer");
    }

    @Test
    void shouldExtractAnswerFieldFromJson() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":\"  json answer  \"}");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("json answer");
    }

    @Test
    void shouldRejectEmptyAnswerField() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":\"   \"}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.EMPTY_ANSWER);
    }

    @Test
    void shouldExtractOpenAiChatCompletionsContent() {
        String rawResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "  chat completion answer  "
                      }
                    }
                  ]
                }
                """;

        AnswerExtractResult result = answerExtractor.extract(rawResponse);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("chat completion answer");
    }

    @Test
    void shouldPreferResponsesOutputText() {
        String rawResponse = """
                {
                  "output_text": "  responses output text answer  ",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "nested answer"
                        }
                      ]
                    }
                  ]
                }
                """;

        AnswerExtractResult result = answerExtractor.extract(rawResponse);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("responses output text answer");
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
                          "text": "  first part  "
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

    @Test
    void shouldReturnEmptyResponseForBlankInput() {
        AnswerExtractResult result = answerExtractor.extract("   ");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.EMPTY_RESPONSE);
    }

    @Test
    void shouldReturnInvalidJsonForBrokenJson() {
        AnswerExtractResult result = answerExtractor.extract("{\"answer\":");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.INVALID_JSON);
    }

    @Test
    void shouldReturnMissingAnswerFieldForEmptyJson() {
        AnswerExtractResult result = answerExtractor.extract("{}");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.MISSING_ANSWER_FIELD);
    }

    @Test
    void shouldReturnUnsupportedStructureForJsonArray() {
        AnswerExtractResult result = answerExtractor.extract("[{\"text\":\"answer\"}]");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(AnswerExtractFailureReason.UNSUPPORTED_STRUCTURE);
    }
}
