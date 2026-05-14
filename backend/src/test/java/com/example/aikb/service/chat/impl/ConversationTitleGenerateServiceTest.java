package com.example.aikb.service.chat.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.aikb.entity.Conversation;
import com.example.aikb.mapper.ConversationMapper;
import com.example.aikb.service.chat.ConversationTitleGenerateService;
import com.example.aikb.service.llm.AnswerExtractResult;
import com.example.aikb.service.llm.AnswerExtractor;
import com.example.aikb.service.llm.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationTitleGenerateServiceTest {

    private static final String CONVERSATION_UID = "test-conv-uid";
    private static final String USER_QUESTION = "员工忘记系统密码时该怎么办？";
    private static final String ASSISTANT_ANSWER = "员工可以通过管理员重置密码。";

    private ConversationMapper conversationMapper;
    private LlmService llmService;
    private AnswerExtractor answerExtractor;
    private ConversationTitleGenerateService service;

    @BeforeEach
    void setUp() {
        conversationMapper = mock(ConversationMapper.class);
        llmService = mock(LlmService.class);
        answerExtractor = new AnswerExtractor(new ObjectMapper());
        service = new ConversationTitleGenerateService(conversationMapper, llmService, answerExtractor);
    }

    @Test
    void shouldBuildPromptWithQuestionAndAnswer() {
        String prompt = service.buildTitlePrompt("员工忘记密码", "联系管理员重置即可。");

        assertThat(prompt)
                .contains("员工忘记密码")
                .contains("联系管理员重置即可。")
                .contains("标题：")
                .doesNotContain("{")
                .doesNotContain("[");
    }

    @Test
    void shouldCleanTitleByTrimming() {
        String title = service.cleanTitle("  会话标题  ");
        assertThat(title).isEqualTo("会话标题");
    }

    @Test
    void shouldCleanTitleByRemovingQuotes() {
        assertThat(service.cleanTitle("\"重置密码流程\"")).isEqualTo("重置密码流程");
        assertThat(service.cleanTitle("'员工密码重置'")).isEqualTo("员工密码重置");
        assertThat(service.cleanTitle("「系统密码重置」")).isEqualTo("系统密码重置");
    }

    @Test
    void shouldCleanTitleByRemovingNewlines() {
        String title = service.cleanTitle("员工密码\n重置方法");
        assertThat(title).isEqualTo("员工密码重置方法");
    }

    @Test
    void shouldTruncateTitleToMaxLength() {
        String longTitle = "这是一个非常长的会话标题它肯定会超过三十个字符的限制被截断";
        String title = service.cleanTitle(longTitle);
        assertThat(title).hasSizeLessThanOrEqualTo(30);
    }

    @Test
    void shouldRejectJsonLikeContent() {
        assertThat(service.cleanTitle("{ \"title\": \"test\" }")).isNull();
        assertThat(service.cleanTitle("[ \"title\" ]")).isNull();
    }

    @Test
    void shouldReturnNullForEmptyTitle() {
        assertThat(service.cleanTitle("")).isNull();
        assertThat(service.cleanTitle("  ")).isNull();
        assertThat(service.cleanTitle("\n\n")).isNull();
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(service.cleanTitle(null)).isNull();
    }

    @Test
    void shouldUseLlmGeneratedTitleWhenSuccessful() {
        Conversation conversation = new Conversation();
        conversation.setConversationUid(CONVERSATION_UID);
        conversation.setTitleSource("AUTO");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);
        when(llmService.chat(any())).thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"员工密码重置方法\"}]}");
        when(conversationMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        service.generateTitle(CONVERSATION_UID, USER_QUESTION, ASSISTANT_ANSWER);

        verify(conversationMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void shouldFallbackToFirstQuestionWhenLlmFails() {
        Conversation conversation = new Conversation();
        conversation.setConversationUid(CONVERSATION_UID);
        conversation.setTitleSource("AUTO");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);
        when(llmService.chat(any())).thenThrow(new RuntimeException("LLM unavailable"));
        when(conversationMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        service.generateTitle(CONVERSATION_UID, USER_QUESTION, ASSISTANT_ANSWER);

        verify(conversationMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void shouldNotOverwriteUserRenamedConversation() {
        Conversation conversation = new Conversation();
        conversation.setConversationUid(CONVERSATION_UID);
        conversation.setTitleSource("USER");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);

        service.generateTitle(CONVERSATION_UID, USER_QUESTION, ASSISTANT_ANSWER);

        verify(conversationMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void shouldNotTriggerForNullConversationUid() {
        service.generateTitle(null, USER_QUESTION, ASSISTANT_ANSWER);
        verify(conversationMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldNotTriggerForBlankConversationUid() {
        service.generateTitle("  ", USER_QUESTION, ASSISTANT_ANSWER);
        verify(conversationMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldDoNothingWhenConversationNotFound() {
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.generateTitle(CONVERSATION_UID, USER_QUESTION, ASSISTANT_ANSWER);

        verify(conversationMapper, never()).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void shouldFallbackToXinHuiHuaWhenQuestionIsEmpty() {
        Conversation conversation = new Conversation();
        conversation.setConversationUid(CONVERSATION_UID);
        conversation.setTitleSource("AUTO");
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(conversation);
        when(llmService.chat(any())).thenThrow(new RuntimeException("fail"));

        service.generateTitle(CONVERSATION_UID, "", ASSISTANT_ANSWER);

        verify(conversationMapper).update(any(), any(UpdateWrapper.class));
    }
}
