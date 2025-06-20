package cn.cangnova.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * API 通用响应模型
 */
@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String,
    @Contextual
    val data: Any? = null
)

/**
 * 批量删除请求
 */
@Serializable
data class BulkDeleteRequest(
    val ids: List<String>
) 