package org.cangnova.cangjie.buildsystem.impl.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 * 单包配置信息
 */
@Serializable
data class PackageConfigurationInfo(
    /** 包的输出类型 */
    @JsonProperty("output-type")
    val outputType: OutputType? = null,
    
    /** 包的编译选项 */
    @JsonProperty("compile-option")
    val compileOption: String? = null
)