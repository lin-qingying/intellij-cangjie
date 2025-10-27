/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 * 命令剖面配置项，用于控制某个命令执行时的默认配置项
 */
@Serializable
data class ProfileConfig(
    /** 编译流程的控制项 */
    val build: BuildProfileConfig? = null,
    /** 测试配置，支持指定编译和运行测试用例时的选项 */
    val test: TestProfileConfig? = null,
    /** 性能测试配置，支持指定编译和运行性能测试用例时的选项 */
    val bench: BenchProfileConfig? = null,
    /** 运行可执行文件时的选项 */
    val run: RunProfileConfig? = null,
    /** 自定义透传给 cjc 的选项 */
    @field:JsonProperty("customized-option")
    val customizedOption: Map<String, String>? = null
)

/**
 * 编译流程的控制项配置
 */
@Serializable
data class BuildProfileConfig(
    /** 是否开启 LTO （Link Time Optimization 链接时优化）优化编译模式，仅 linux 平台支持该功能 */
    val lto: String? = null,
    /** 是否默认开启增量编译 */
    val incremental: Boolean? = null
)

/**
 * 测试配置，支持指定编译和运行测试用例时的选项
 */
@Serializable
data class TestProfileConfig(
    /** 指定执行结果在控制台中是否无颜色显示 */
    @field:JsonProperty("no-color")
    val noColor: Boolean? = null,
    /** 为每个测试用例指定默认的超时时间 */
    @field:JsonProperty("timeout-each")
    val timeoutEach: String? = null,
    /** 指定随机种子的值 */
    @field:JsonProperty("random-seed")
    val randomSeed: Int? = null,
    /** 是否执行性能测试 */
    val bench: Boolean? = null,
    /** 指定测试执行后的报告生成路径 */
    @field:JsonProperty("report-path")
    val reportPath: String? = null,
    /** 指定报告输出格式，当前单元测试报告仅支持 xml 格式 */
    @field:JsonProperty("report-format")
    val reportFormat: String? = null,
    /** 指定显示编译过程详细信息 */
    val verbose: Boolean? = null,
    /** 测试构建配置 */
    val build: TestBuildConfig? = null,
    /** 环境变量配置 */
    val env: Map<String, EnvConfig>? = null
)

/**
 * 性能测试配置，支持指定编译和运行性能测试用例时的选项
 */
@Serializable
data class BenchProfileConfig(
    /** 指定执行结果在控制台中是否无颜色显示 */
    @field:JsonProperty("no-color")
    val noColor: Boolean? = null,
    /** 指定随机种子的值 */
    @field:JsonProperty("random-seed")
    val randomSeed: Int? = null,
    /** 指定测试执行后的报告生成路径 */
    @field:JsonProperty("report-path")
    val reportPath: String? = null,
    /** 与当前性能结果进行比较的现有报告的路径 */
    @field:JsonProperty("baseline-path")
    val baselinePath: String? = null,
    /** 指定报告输出格式，性能测试报告仅支持 csv 和 csv-raw 格式 */
    @field:JsonProperty("report-format")
    val reportFormat: String? = null,
    /** 指定显示编译过程详细信息 */
    val verbose: Boolean? = null,
    /** 测试构建配置 */
    val build: TestBuildConfig? = null,
    /** 环境变量配置 */
    val env: Map<String, EnvConfig>? = null
)

/**
 * 运行配置，支持配置在 run 命令时运行可执行文件时的环境变量配置
 */
@Serializable
data class RunProfileConfig(
    /** 环境变量配置 */
    val env: Map<String, EnvConfig>? = null
)

/**
 * 测试构建配置
 */
@Serializable
data class TestBuildConfig(
    /** 附加编译选项 */
    @field:JsonProperty("compile-option")
    val compileOption: String? = null,
    /** 是否开启 LTO 优化编译模式 */
    val lto: String? = null,
    /** 显式设置 mock 模式 */
    val mock: String? = null
)

/**
 * 环境变量配置
 */
@Serializable
data class EnvConfig(
    /** 环境变量值 */
    val value: String,
    /** 环境变量的拼接方式 */
    @field:JsonProperty("splice-type")
    val spliceType: SpliceType? = null
)

/**
 * 环境变量的拼接方式
 */
@Serializable
enum class SpliceType {
    /** 该配置仅在环境内不存在同名环境变量时生效，若存在同名环境变量则忽略该配置 */
    @JsonProperty("absent")
    ABSENT,
    /** 该配置会替代环境中已有的同名环境变量 */
    @JsonProperty("replace")
    REPLACE,
    /** 该配置会拼接在环境中已有的同名环境变量之前 */
    @JsonProperty("prepend")
    PREPEND,
    /** 该配置会拼接在环境中已有的同名环境变量之后 */
    @JsonProperty("append")
    APPEND
} 