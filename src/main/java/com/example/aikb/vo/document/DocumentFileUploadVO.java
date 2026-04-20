package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 真实文件上传响应信息，表示文件已经保存并登记为文档，但尚未解析。
 */
@Data
@Builder
@Schema(description = "真实文件上传响应信息")
public class DocumentFileUploadVO {

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long id;

    /** 文档所属知识库 ID。 */
    @Schema(description = "文档所属知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 原始文件名称。 */
    @Schema(description = "原始文件名称", example = "产品FAQ.md")
    private String fileName;

    /** 文件类型。 */
    @Schema(description = "文件类型", example = "md")
    private String fileType;

    /** 文件大小，单位为字节。 */
    @Schema(description = "文件大小，单位为字节", example = "2048")
    private Long fileSize;

    /** 后端生成的真实存储路径。 */
    @Schema(description = "后端生成的真实存储路径", example = "uploads/1/1/20260420/uuid.md")
    private String storagePath;

    /** 解析状态，上传成功后固定为 UPLOADED。 */
    @Schema(description = "解析状态", example = "UPLOADED")
    private String parseStatus;

    /** 上传用户 ID。 */
    @Schema(description = "上传用户 ID", example = "1")
    private Long createdBy;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
