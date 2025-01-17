package com.linqingying.cangjie.cjpm.project.model.toml

import kotlinx.serialization.Serializable

/**
 * 外部依赖配置类
 */
@Serializable
data class FfiConfig(
    /** C语言库依赖配置 */
    val c: Map<String, CLibConfig> = mapOf()
)

/**
 * C语言库配置
 */
@Serializable
data class CLibConfig(
    /** 库文件路径 */
    val path: String? = null
)

