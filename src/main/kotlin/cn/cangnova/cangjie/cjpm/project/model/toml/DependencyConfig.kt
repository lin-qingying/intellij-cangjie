package cn.cangnova.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 * 依赖配置类，支持本地路径依赖和远程 git 依赖
 */
@Serializable
data class DependencyConfig(
    /** 本地依赖路径 */
    val path: String? = null,
    
    /** git 仓库地址，必须包含 git 支持的任何格式的有效 url */
    val git: String? = null,
    
    /** git 分支名 */
    val branch: String? = null,
    
    /** git 标签名 */
    val tag: String? = null,
    
    /** git commit ID */
    val commitId: String? = null,
    
    /** 依赖版本号，用于检查依赖项是否具有正确的版本 */
    val version: String? = null,
    
    /** 指定编译产物类型，可以与源码依赖自身的编译产物类型不一致 */
    @JsonProperty("output-type")
    val outputType: OutputType? = null
)