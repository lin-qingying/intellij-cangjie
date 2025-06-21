package cn.cangnova.model

import kotlinx.serialization.Serializable

/**
 * 刷新令牌请求模型
 */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
) 