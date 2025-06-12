package cn.cangnova.model

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

/**
 * 管理员用户模型
 */
@Serializable
data class AdminUser(
    /**
     * MongoDB文档ID
     */
    @BsonId
    val _id: String = ObjectId().toString(),
    
    /**
     * 用户名
     */
    val username: String,
    
    /**
     * 密码哈希
     */
    val passwordHash: String,
    
    /**
     * 显示名称
     */
    val displayName: String,
    
    /**
     * 角色
     */
    val role: String,
    
    /**
     * 电子邮件
     */
    val email: String = "",
    
    /**
     * 创建时间
     */
    val createdAt: Long = Instant.now().toEpochMilli(),
    
    /**
     * 最后登录时间
     */
    var lastLoginAt: Long? = null,
    
    /**
     * 是否启用
     */
    var enabled: Boolean = true
)

/**
 * 创建用户请求
 */
@Serializable
data class CreateUserRequest(
    val username: String,
    val password: String,
    val displayName: String,
    val role: String,
    val email: String = ""
)

/**
 * 更新用户请求
 */
@Serializable
data class UpdateUserRequest(
    val displayName: String? = null,
    val role: String? = null,
    val email: String? = null,
    val password: String? = null,
    val enabled: Boolean? = null
)

/**
 * 用户登录请求
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 用户登录响应
 */
@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: AdminUserDto? = null
)

/**
 * 管理员用户DTO，用于API响应
 */
@Serializable
data class AdminUserDto(
    val username: String,
    val displayName: String,
    val role: String,
    val email: String,
    val createdAt: Long,
    val lastLoginAt: Long?,
    val enabled: Boolean
) {
    companion object {
        fun fromAdminUser(user: AdminUser): AdminUserDto {
            return AdminUserDto(
                username = user.username,
                displayName = user.displayName,
                role = user.role,
                email = user.email,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt,
                enabled = user.enabled
            )
        }
    }
} 