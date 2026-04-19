package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 问答记录分页响应。
 */
@Data
@Builder
@Schema(description = "问答记录分页响应")
public class ChatRecordPageResponse {

    @Schema(description = "当前页数据")
    private List<ChatRecordListItemVO> list;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Long pageNum;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize;
}
