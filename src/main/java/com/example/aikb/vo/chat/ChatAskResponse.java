package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "问答响应结果")
public class ChatAskResponse {

    @Schema(description = "生成的答案")
    private String answer;

    @Schema(description = "检索是否命中切片", example = "true")
    private Boolean matched;

    @Schema(description = "实际命中的切片数量", example = "3")
    private Integer retrievedChunkCount;

    @Schema(description = "答案引用来源")
    private List<CitationVO> citations;
}
