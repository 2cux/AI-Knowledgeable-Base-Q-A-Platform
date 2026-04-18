package com.example.aikb.controller.retrieval;

import com.example.aikb.common.Result;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检索接口控制器。该模块只返回上下文 chunk，不执行 QA 生成。
 */
@Validated
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
@Tag(name = "检索模块", description = "基于知识库检索相关文档切片")
public class RetrievalController {

    private final RetrievalService retrievalService;

    @Operation(summary = "检索相关chunk", description = "根据用户问题在指定知识库中检索最相关的文档切片")
    @PostMapping("/search")
    public Result<RetrievalSearchVO> search(@Valid @RequestBody RetrievalSearchRequest request) {
        return Result.success(retrievalService.search(request));
    }
}
