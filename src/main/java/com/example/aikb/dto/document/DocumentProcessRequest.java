package com.example.aikb.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档处理请求参数，用于指定切片配置或直接传入纯文本内容。
 */
@Data
@Schema(description = "文档处理请求参数")
public class DocumentProcessRequest {

    /** 切片长度，按字符数计算。 */
    @Schema(description = "切片长度，按字符数计算", example = "500")
    @Min(value = 100, message = "chunkSize不能小于100")
    @Max(value = 5000, message = "chunkSize不能大于5000")
    private Integer chunkSize = 500;

    /** 相邻切片重叠长度，按字符数计算。 */
    @Schema(description = "相邻切片重叠长度，按字符数计算", example = "50")
    @Min(value = 0, message = "overlap不能小于0")
    @Max(value = 1000, message = "overlap不能大于1000")
    private Integer overlap = 50;

    /** 调试兼容入口；有真实 storagePath 时服务端优先读取本地 txt/md 文件。 */
    @Schema(description = "调试兼容文本；有真实storagePath时服务端优先读取本地txt/md文件")
    @Size(max = 200000, message = "文本内容不能超过200000个字符")
    private String textContent;
}
