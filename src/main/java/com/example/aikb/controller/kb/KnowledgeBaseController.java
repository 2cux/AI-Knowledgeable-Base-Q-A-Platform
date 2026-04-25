package com.example.aikb.controller.kb;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.dto.kb.KnowledgeBaseCreateRequest;
import com.example.aikb.dto.kb.KnowledgeBasePageRequest;
import com.example.aikb.service.document.DocumentService;
import com.example.aikb.service.kb.KnowledgeBaseService;
import com.example.aikb.vo.document.DocumentListVO;
import com.example.aikb.vo.kb.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库控制器。
 */
@Validated
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库基础查询接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;

    @Operation(summary = "创建知识库")
    @PostMapping
    public Result<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return Result.success(knowledgeBaseService.create(request), "Created successfully");
    }

    @Operation(summary = "分页查询知识库")
    @GetMapping
    public Result<PageResult<KnowledgeBaseVO>> page(@Valid @ModelAttribute KnowledgeBasePageRequest request) {
        return Result.success(knowledgeBaseService.page(request));
    }

    @Operation(summary = "查询知识库详情")
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getById(
            @PathVariable @Positive(message = "Knowledge base id must be greater than 0") Long id) {
        return Result.success(knowledgeBaseService.getById(id));
    }

    @Operation(summary = "查询知识库下文档列表")
    @GetMapping("/{id}/documents")
    public Result<List<DocumentListVO>> listDocuments(
            @PathVariable @Positive(message = "Knowledge base id must be greater than 0") Long id) {
        return Result.success(documentService.listByKnowledgeBase(id));
    }
}
