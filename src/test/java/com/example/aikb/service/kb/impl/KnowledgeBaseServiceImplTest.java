package com.example.aikb.service.kb.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.aikb.dto.kb.KnowledgeBaseUpdateRequest;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.LoginUser;
import com.example.aikb.vo.kb.KnowledgeBaseVO;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long KNOWLEDGE_BASE_ID = 11L;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @BeforeEach
    void setUp() {
        initMybatisPlusTableInfo();
        knowledgeBaseService = new KnowledgeBaseServiceImpl(knowledgeBaseMapper);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(USER_ID, "tester"), null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateOnlyChangesOwnActiveKnowledgeBaseSafeFields() {
        KnowledgeBase updated = knowledgeBase("New KB", "New description");
        when(knowledgeBaseMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(knowledgeBaseMapper.selectOne(any())).thenReturn(updated);

        KnowledgeBaseVO response = knowledgeBaseService.update(KNOWLEDGE_BASE_ID, request("  New KB  ",
                "  New description  "));

        assertThat(response.getId()).isEqualTo(KNOWLEDGE_BASE_ID);
        assertThat(response.getName()).isEqualTo("New KB");
        assertThat(response.getDescription()).isEqualTo("New description");
        assertThat(response.getStatus()).isEqualTo(1);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaUpdateWrapper<KnowledgeBase>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
        verify(knowledgeBaseMapper).update(isNull(), wrapperCaptor.capture());
        String sqlSet = wrapperCaptor.getValue().getSqlSet();
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSet).contains("name", "description", "updated_at");
        assertThat(sqlSet).doesNotContain("owner_id", "created_at", "status", "deleted", "id");
        assertThat(sqlSegment).contains("id", "owner_id", "status", "deleted");
    }

    @Test
    void updateThrowsBusinessExceptionWhenKnowledgeBaseNotFoundOrNotOwned() {
        when(knowledgeBaseMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> knowledgeBaseService.update(KNOWLEDGE_BASE_ID, request("New KB", "desc")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("知识库不存在");

        verify(knowledgeBaseMapper, never()).selectOne(any());
    }

    @Test
    void deleteSoftDeletesOwnActiveKnowledgeBaseOnly() {
        when(knowledgeBaseMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        knowledgeBaseService.delete(KNOWLEDGE_BASE_ID);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<LambdaUpdateWrapper<KnowledgeBase>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
        verify(knowledgeBaseMapper).update(isNull(), wrapperCaptor.capture());
        String sqlSet = wrapperCaptor.getValue().getSqlSet();
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSet).contains("deleted", "updated_at");
        assertThat(sqlSet).doesNotContain("name", "description", "owner_id", "created_at");
        assertThat(sqlSegment).contains("id", "owner_id", "status", "deleted");
    }

    @Test
    void deleteThrowsBusinessExceptionWhenKnowledgeBaseNotFoundNotOwnedOrDeleted() {
        when(knowledgeBaseMapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> knowledgeBaseService.delete(KNOWLEDGE_BASE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("知识库不存在");
    }

    @Test
    void updateRequiresLogin() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> knowledgeBaseService.update(KNOWLEDGE_BASE_ID, request("New KB", "desc")))
                .isInstanceOf(BusinessException.class);

        verify(knowledgeBaseMapper, never()).update(any(), any(Wrapper.class));
    }

    private KnowledgeBaseUpdateRequest request(String name, String description) {
        KnowledgeBaseUpdateRequest request = new KnowledgeBaseUpdateRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private void initMybatisPlusTableInfo() {
        if (TableInfoHelper.getTableInfo(KnowledgeBase.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    KnowledgeBase.class);
        }
    }

    private KnowledgeBase knowledgeBase(String name, String description) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(KNOWLEDGE_BASE_ID);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        knowledgeBase.setOwnerId(USER_ID);
        knowledgeBase.setStatus(1);
        knowledgeBase.setDeleted(0);
        knowledgeBase.setCreatedAt(LocalDateTime.now().minusDays(1));
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        return knowledgeBase;
    }
}
