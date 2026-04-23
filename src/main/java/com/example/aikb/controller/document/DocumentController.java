package com.example.aikb.controller.document;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.dto.document.DocumentEmbeddingRequest;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentProcessRequest;
import com.example.aikb.dto.document.DocumentUploadRequest;
import com.example.aikb.service.embedding.DocumentEmbeddingService;
import com.example.aikb.service.document.DocumentProcessService;
import com.example.aikb.service.document.DocumentService;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.DocumentChunkVO;
import com.example.aikb.vo.document.DocumentDetailVO;
import com.example.aikb.vo.document.DocumentEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingVO;
import com.example.aikb.vo.document.DocumentFileUploadVO;
import com.example.aikb.vo.document.DocumentListVO;
import com.example.aikb.vo.document.DocumentProcessVO;
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
 * 文档接口控制器，负责文档上传、分页查询、详情查询和解析任务创建。
 */
@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传、列表查询、详情查询和解析任务创建接口")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessService documentProcessService;
    private final DocumentEmbeddingService documentEmbeddingService;
    private final TaskRecordService taskRecordService;

    /**
     * 上传文档元数据，并绑定到当前登录用户拥有的指定知识库。
     */
    @Operation(summary = "上传文档元数据", description = "兼容旧接口：仅保存文档元数据，不接收真实文件；真实文件请使用 /api/documents/upload-file")
    @PostMapping("/upload")
    public Result<DocumentDetailVO> upload(@Valid @RequestBody DocumentUploadRequest request) {
        return Result.success(documentService.upload(request), "元数据上传成功");
    }

    /**
     * 上传真实文件并生成文档记录。上传成功只代表文件保存成功，后续解析由解析接口触发。
     */
    @Operation(summary = "上传真实文件", description = "使用 multipart/form-data 上传 txt/md 文件，保存真实文件并生成 UPLOADED 文档记录")
    @PostMapping(value = "/upload-file", consumes = "multipart/form-data")
    public Result<DocumentFileUploadVO> uploadFile(@Valid @ModelAttribute DocumentFileUploadRequest request) {
        return Result.success(documentService.uploadFile(request), "文件上传成功，等待解析");
    }

    /**
     * 分页查询当前登录用户可访问的文档列表，可按知识库 ID 筛选。
     */
    @Operation(summary = "分页查询文档", description = "分页获取当前登录用户可访问的文档列表，支持按知识库筛选")
    @GetMapping
    public Result<PageResult<DocumentListVO>> page(@Valid @ModelAttribute DocumentListQuery query) {
        return Result.success(documentService.page(query));
    }

    /**
     * 查询当前登录用户可访问的指定文档详情。
     */
    @Operation(summary = "查询文档详情", description = "根据文档 ID 查询当前登录用户可访问的文档详情")
    @GetMapping("/{documentId}")
    public Result<DocumentDetailVO> getById(@PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return Result.success(documentService.getById(id));
    }

    /**
     * 为当前登录用户可访问的指定文档创建一条解析任务记录。
     */
    @Operation(summary = "创建文档解析任务", description = "为指定文档创建解析任务记录，MVP 阶段不执行真实解析")
    @PostMapping("/{documentId}/parse")
    public Result<Long> parse(@PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return Result.success(documentService.createParseTask(id), "解析任务已创建");
    }

    /**
     * 对当前登录用户可访问的指定文档执行文本切片，并将切片写入数据库。
     */
    @Operation(summary = "处理文档切片", description = "优先从真实上传的txt/md文件读取内容，按固定长度和重叠长度切片，并重建文档切片数据")
    @PostMapping("/{documentId}/process")
    public Result<DocumentProcessVO> process(
            @PathVariable @Positive(message = "文档ID必须大于0") Long id,
            @Valid @RequestBody(required = false) DocumentProcessRequest request) {
        return Result.success(documentProcessService.process(id, request), "处理成功");
    }

    /**
     * 查询当前登录用户可访问的指定文档切片列表。
     */
    @Operation(summary = "查询文档切片列表", description = "根据文档 ID 查询当前登录用户可访问的文档切片列表")
    @GetMapping("/{documentId}/chunks")
    public Result<List<DocumentChunkVO>> listChunks(@PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return Result.success(documentProcessService.listChunks(id));
    }

    /**
     * 查询当前登录用户可访问的指定文档最近处理任务列表。
     */
    @Operation(summary = "查询文档任务列表", description = "根据文档ID查询当前用户可访问的最近任务记录")
    @GetMapping("/{documentId}/tasks")
    public Result<List<TaskRecordVO>> listTasks(@PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return Result.success(taskRecordService.listByDocument(id));
    }

    /**
     * 对当前登录用户可访问的指定文档执行 chunk 向量化同步。
     */
    @Operation(summary = "执行文档向量化", description = "读取文档下所有 chunk，调用 embedding 服务并保存每个 chunk 的向量化状态")
    @PostMapping("/{documentId}/embed")
    public Result<DocumentEmbeddingVO> embed(
            @PathVariable @Positive(message = "文档ID必须大于0") Long id,
            @Valid @RequestBody(required = false) DocumentEmbeddingRequest request) {
        return Result.success(documentEmbeddingService.embedDocument(id, request), "向量化任务执行完成");
    }

    /**
     * 查询当前登录用户可访问的指定文档向量化状态。
     */
    @Operation(summary = "查询文档向量化状态", description = "汇总查询指定文档下所有 chunk 的向量化同步状态")
    @GetMapping("/{documentId}/embedding-status")
    public Result<DocumentEmbeddingStatusVO> getEmbeddingStatus(
            @PathVariable @Positive(message = "文档ID必须大于0") Long id) {
        return Result.success(documentEmbeddingService.getEmbeddingStatus(id));
    }
}
