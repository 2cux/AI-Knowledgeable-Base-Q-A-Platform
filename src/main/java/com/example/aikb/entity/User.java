package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统用户实体，保存账号、认证和基础权限信息。
 */
@Data
@TableName("user")
@Schema(description = "系统用户实体")
public class User {

    /** 用户主键 ID。 */
    @Schema(description = "用户主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，作为登录账号使用。 */
    @Schema(description = "用户名", example = "alice")
    private String username;

    /** 加密后的登录密码。 */
    @Schema(description = "加密后的登录密码")
    @TableField("password_hash")
    private String passwordHash;

    /** 用户邮箱。 */
    @Schema(description = "用户邮箱", example = "alice@example.com")
    private String email;

    /** 用户昵称。 */
    @Schema(description = "用户昵称", example = "Alice")
    private String nickname;

    /** 用户角色，用于区分权限范围。 */
    @Schema(description = "用户角色", example = "USER")
    private String role;

    /** 用户状态，用于控制账号是否可用。 */
    @Schema(description = "用户状态", example = "1")
    private Integer status;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
