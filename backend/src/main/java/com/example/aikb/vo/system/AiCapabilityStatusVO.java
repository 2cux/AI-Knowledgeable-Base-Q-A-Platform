package com.example.aikb.vo.system;

import com.example.aikb.service.system.AiRuntimeMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单项 AI 能力运行状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCapabilityStatusVO {

    private AiRuntimeMode mode;
    private String provider;
    private String model;
    private boolean enabled;
    private boolean baseUrlConfigured;
    private String baseUrlHost;
    private String baseUrlPath;
    private boolean apiKeyConfigured;
}
