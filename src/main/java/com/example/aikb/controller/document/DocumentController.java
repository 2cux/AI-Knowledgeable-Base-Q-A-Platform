package com.example.aikb.controller.document;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.dto.document.DocumentEmbeddingRequest;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentProcessRequest;
import com.example.aikb.dto.document.DocumentUploadRequest;
import com.example.aikb.service.document.DocumentProcessService;
import com.example.aikb.service.document.DocumentService;
import com.example.aikb.service.embedding.DocumentEmbeddingService;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.DocumentChunkVO;
import com.example.aikb.vo.document.DocumentDetailVO;
import com.example.aikb.vo.document.DocumentEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingVO;
import com.example.aikb.vo.document.DocumentFileUploadVO;
import com.example.aikb.vo.document.DocumentListVO;
import com.example.aikb.vo.document.DocumentProcessVO;
import com.example.aikb.vo.document.DocumentStatusVO;
import com.example.aikb.vo.task.TaskRecordVO;
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
 * 文档控制器。
 */
@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传、查询与状态查看接口")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessService documentProcessService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final TaskRecordService taskRecordService;

    @Operation(summary = "上传文档元数据")
    @PostMapping("/upload")
    public Result<DocumentDetailVO> upload(@Valid @RequestBody DocumentUploadRequest request) {
        return Result.success(documentService.upload(request), "Metadata uploaded successfully");
    }

    @Operation(summary = "上传真实文件")
    @PostMapping(value = "/upload-file", consumes = "multipart/form-data")
    public Result<DocumentFileUploadVO> uploadFile(@Valid @ModelAttribute DocumentFileUploadRequest request) {
        return Result.success(documentService.uploadFile(request), "File uploaded successfully");
    }

    @Operation(summary = "分页查询文档")
    @GetMapping
    public Result<PageResult<DocumentListVO>> page(@Valid @ModelAttribute DocumentListQuery query) {
        return Result.success(documentService.page(query));
    }

    @Operation(summary = "查询文档详情")
    @GetMapping("/{documentId}")
    public Result<DocumentDetailVO> getById(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(documentService.getById(documentId));
    }

    @Operation(summary = "查询文档状态概览")
    @GetMapping("/{documentId}/status")
    public Result<DocumentStatusVO> getStatus(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(documentService.getStatus(documentId));
    }

    @Operation(summary = "创建解析任务")
    @PostMapping("/{documentId}/parse")
    public Result<Long> parse(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(documentService.createParseTask(documentId), "Parse task created successfully");
    }

    @Operation(summary = "执行文档处理")
    @PostMapping("/{documentId}/process")
    public Result<DocumentProcessVO> process(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId,
            @Valid @RequestBody(required = false) DocumentProcessRequest request) {
        return Result.success(documentProcessService.process(documentId, request), "Document processed successfully");
    }

    @Operation(summary = "查询文档切片列表")
    @GetMapping("/{documentId}/chunks")
    public Result<List<DocumentChunkVO>> listChunks(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(documentProcessService.listChunks(documentId));
    }

    @Operation(summary = "查询文档任务列表")
    @GetMapping("/{documentId}/tasks")
    public Result<List<TaskRecordVO>> listTasks(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(taskRecordService.listByDocument(documentId));
    }

    @Operation(summary = "执行文档向量化")
    @PostMapping("/{documentId}/embed")
    public Result<DocumentEmbeddingVO> embed(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId,
            @Valid @RequestBody(required = false) DocumentEmbeddingRequest request) {
        return Result.success(documentEmbeddingService.embedDocument(documentId, request), "Embedding completed");
    }

    @Operation(summary = "查询向量化状态")
    @GetMapping("/{documentId}/embedding-status")
    public Result<DocumentEmbeddingStatusVO> getEmbeddingStatus(
            @PathVariable("documentId") @Positive(message = "Document id must be greater than 0") Long documentId) {
        return Result.success(documentEmbeddingService.getEmbeddingStatus(documentId));
    }
}
