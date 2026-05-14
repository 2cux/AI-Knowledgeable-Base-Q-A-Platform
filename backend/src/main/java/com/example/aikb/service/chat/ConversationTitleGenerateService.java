package com.example.aikb.service.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aikb.common.LogSanitizer;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.entity.Conversation;
import com.example.aikb.mapper.ConversationMapper;
import com.example.aikb.service.llm.AnswerExtractResult;
import com.example.aikb.service.llm.AnswerExtractor;
import com.example.aikb.service.llm.LlmService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates short conversation titles using the LLM based on the first Q&A round.
 * <p>
 * Title generation never blocks the main ask flow — all exceptions are caught and
 * result in a graceful fallback to the first question truncated to 30 characters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTitleGenerateService {

    private static final int TITLE_MAX_LENGTH = 30;
    private static final int ANSWER_PREVIEW_LENGTH = 800;
    private static final String SOURCE_AUTO = "AUTO";
    private static final String SOURCE_USER = "USER";
    private static final String FALLBACK_SUFFIX = "...";

    private final ConversationMapper conversationMapper;
    private final LlmService llmService;
    private final AnswerExtractor answerExtractor;

    /**
     * Generates a title for the given conversation using the first user question
     * and the first assistant answer. Falls back to the first question if the LLM
     * call fails or produces an invalid title.
     * <p>
     * Only updates conversations whose title_source is NOT {@code USER}, so that
     * user-renamed conversations are never overwritten.
     */
    public void generateTitle(String conversationUid, String firstUserQuestion, String firstAssistantAnswer) {
        if (conversationUid == null || conversationUid.isBlank()) {
            return;
        }

        try {
            Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                    .eq(Conversation::getConversationUid, conversationUid)
                    .eq(Conversation::getDeleted, false)
                    .last("LIMIT 1"));
            if (conversation == null) {
                return;
            }

            // Never overwrite a user-renamed conversation
            if (SOURCE_USER.equals(conversation.getTitleSource())) {
                return;
            }

            String title = generateByLlm(firstUserQuestion, firstAssistantAnswer);

            if (title == null || title.isBlank()) {
                log.warn("Conversation title generation returned empty, conversationUid={}, fallback=true",
                        LogSanitizer.safeMessage(conversationUid));
                fallbackTitle(conversationUid, firstUserQuestion);
                return;
            }

            int updated = conversationMapper.update(null, new UpdateWrapper<Conversation>()
                    .eq("conversation_uid", conversationUid)
                    .eq("deleted", false)
                    .and(w -> w.eq("title_source", SOURCE_AUTO)
                            .or().isNull("title_source"))
                    .set("title", title));
            if (updated > 0) {
                log.info("Conversation title generated, conversationUid={}, titleLength={}, titleGenerated=true",
                        LogSanitizer.safeMessage(conversationUid), title.length());
            }
        } catch (Exception e) {
            log.warn("Conversation title generation failed, conversationUid={}, fallback=true, error={}",
                    LogSanitizer.safeMessage(conversationUid),
                    LogSanitizer.safeMessage(e.getMessage()));
            fallbackTitle(conversationUid, firstUserQuestion);
        }
    }

    /**
     * Calls the LLM to generate a short title based on the first Q&A pair.
     */
    String generateByLlm(String firstUserQuestion, String firstAssistantAnswer) {
        String assistantPreview = preview(firstAssistantAnswer, ANSWER_PREVIEW_LENGTH);
        String prompt = buildTitlePrompt(firstUserQuestion, assistantPreview);

        List<LlmMessageRequest> messages = List.of(
                LlmMessageRequest.builder()
                        .role("system")
                        .content("你是一个会话标题生成助手。")
                        .build(),
                LlmMessageRequest.builder()
                        .role("user")
                        .content(prompt)
                        .build());

        String rawResponse = llmService.chat(messages);
        AnswerExtractResult extractResult = answerExtractor.extract(rawResponse);
        if (!extractResult.isSuccess()) {
            return null;
        }

        return cleanTitle(extractResult.getAnswer());
    }

    /**
     * Builds the title generation prompt.
     */
    public String buildTitlePrompt(String firstUserQuestion, String firstAssistantAnswerPreview) {
        return "请根据下面的用户问题和助手回答，生成一个简短中文标题。\n"
                + "要求：\n"
                + "1. 8 到 20 个中文字符左右。\n"
                + "2. 不要超过 30 个字。\n"
                + "3. 不要加引号。\n"
                + "4. 不要使用\"关于\"\"咨询\"\"问题\"等空泛词，除非必要。\n"
                + "5. 准确概括本轮问答主题。\n"
                + "6. 只输出标题，不要解释。\n"
                + "\n"
                + "用户问题：\n"
                + firstUserQuestion
                + "\n\n"
                + "助手回答：\n"
                + firstAssistantAnswerPreview
                + "\n\n"
                + "标题：";
    }

    /**
     * Cleans the LLM-generated title: trims, removes quotes and line breaks,
     * validates length and content.
     */
    public String cleanTitle(String raw) {
        if (raw == null) {
            return null;
        }

        String cleaned = raw.trim()
                .replaceAll("^[\"'「」]+|[\"'「」]+$", "")
                .replaceAll("\\r?\\n", "")
                .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        // Reject obviously invalid output (JSON, multi-line remnants, overly long)
        if (cleaned.contains("{") || cleaned.contains("[") || cleaned.length() > 100) {
            return null;
        }

        // Truncate to max length
        if (cleaned.length() > TITLE_MAX_LENGTH) {
            cleaned = cleaned.substring(0, TITLE_MAX_LENGTH);
        }

        return cleaned;
    }

    /**
     * Falls back to the first question truncated to TITLE_MAX_LENGTH characters.
     */
    void fallbackTitle(String conversationUid, String firstUserQuestion) {
        if (conversationUid == null || conversationUid.isBlank()) {
            return;
        }

        String fallback = firstUserQuestion == null || firstUserQuestion.isBlank()
                ? "新会话"
                : shorten(firstUserQuestion, TITLE_MAX_LENGTH);

        try {
            int updated = conversationMapper.update(null, new UpdateWrapper<Conversation>()
                    .eq("conversation_uid", conversationUid)
                    .eq("deleted", false)
                    .and(w -> w.eq("title_source", SOURCE_AUTO)
                            .or().isNull("title_source"))
                    .set("title", fallback));
            if (updated > 0) {
                log.info("Conversation title fallback set, conversationUid={}, fallback=true",
                        LogSanitizer.safeMessage(conversationUid));
            }
        } catch (Exception e) {
            log.warn("Conversation title fallback failed, conversationUid={}, error={}",
                    LogSanitizer.safeMessage(conversationUid),
                    LogSanitizer.safeMessage(e.getMessage()));
        }
    }

    private String preview(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + FALLBACK_SUFFIX;
    }
}
