package com.example.aikb.controller.kb;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.dto.kb.KnowledgeBaseCreateRequest;
import com.example.aikb.dto.kb.KnowledgeBasePageRequest;
import com.example.aikb.service.kb.KnowledgeBaseService;
import com.example.aikb.vo.kb.KnowledgeBaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
 * 知识库接口控制器，负责知识库创建、分页查询和详情查询。
 */
@Validated
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库创建、列表查询和详情查询接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建当前用户的知识库。
     */
    @Operation(summary = "创建知识库", description = "为当前登录用户创建一个新的知识库")
    @PostMapping
    public Result<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return Result.success(knowledgeBaseService.create(request), "创建成功");
    }

    /**
     * 分页查询当前用户的知识库列表。
     */
    @Operation(summary = "分页查询知识库", description = "分页获取当前登录用户拥有的知识库列表")
    @GetMapping
    public Result<PageResult<KnowledgeBaseVO>> page(@Valid @ModelAttribute KnowledgeBasePageRequest request) {
        return Result.success(knowledgeBaseService.page(request));
    }

    /**
     * 查询当前用户指定知识库详情。
     */
    @Operation(summary = "查询知识库详情", description = "根据知识库 ID 查询当前登录用户可访问的知识库详情")
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getById(@PathVariable @Positive(message = "知识库ID必须大于0") Long id) {
        return Result.success(knowledgeBaseService.getById(id));
    }
}
