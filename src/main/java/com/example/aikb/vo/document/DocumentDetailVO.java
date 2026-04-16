package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 文档详情展示信息。
 */
@Data
@Builder
@Schema(description = "文档详情展示信息")
public class DocumentDetailVO {

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long id;

    /** 文档所属知识库 ID。 */
    @Schema(description = "文档所属知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 原始文件名称。 */
    @Schema(description = "原始文件名称", example = "产品手册.pdf")
    private String fileName;

    /** 文件类型。 */
    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    /** 文件大小，单位为字节。 */
    @Schema(description = "文件大小，单位为字节", example = "204800")
    private Long fileSize;

    /** 文件存储路径。 */
    @Schema(description = "文件存储路径", example = "kb/1/产品手册.pdf")
    private String storagePath;

    /** 解析状态。 */
    @Schema(description = "解析状态", example = "UPLOADED")
    private String parseStatus;

    /** 上传用户 ID。 */
    @Schema(description = "上传用户 ID", example = "1")
    private Long createdBy;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
