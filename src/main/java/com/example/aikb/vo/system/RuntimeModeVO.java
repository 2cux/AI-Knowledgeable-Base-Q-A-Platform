package com.example.aikb.vo.system;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统运行模式概览。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeModeVO {

    private List<String> activeProfiles;
    private boolean debugAiTestEnabled;
    private AiCapabilityStatusVO embedding;
    private AiCapabilityStatusVO llm;
}
