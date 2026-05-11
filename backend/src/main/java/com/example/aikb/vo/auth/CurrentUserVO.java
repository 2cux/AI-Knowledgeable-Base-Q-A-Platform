package com.example.aikb.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户基础信息响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前登录用户基础信息")
public class CurrentUserVO {

    /** 当前登录用户 ID。 */
    @Schema(description = "当前登录用户 ID", example = "1")
    private Long userId;

    /** 用户名。 */
    @Schema(description = "用户名", example = "alice")
    private String username;

    /** 用户昵称。 */
    @Schema(description = "用户昵称", example = "Alice")
    private String nickname;

    /** 用户角色。 */
    @Schema(description = "用户角色", example = "USER")
    private String role;

    /** 用户状态，1 表示启用，0 表示禁用。 */
    @Schema(description = "用户状态，1 表示启用，0 表示禁用", example = "1")
    private Integer status;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
