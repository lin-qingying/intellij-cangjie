/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.diagnostics.Errors.*

/**
 * 诊断严重性枚举
 * 
 * 定义了诊断消息的严重程度级别
 */
enum class Severity {
    /**
     * 信息级别
     * 
     * 表示非错误的提示信息
     */
    INFO,
    
    /**
     * 错误级别
     * 
     * 表示阻止程序正常编译或运行的问题
     */
    ERROR,
    
    /**
     * 警告级别
     * 
     * 表示潜在问题，但不会阻止程序编译或运行
     */
    WARNING
}

/**
 * 仓颉错误代码
 * 
 * 定义了错误代码与诊断工厂的映射关系
 */
object ErrorCodes {
    /**
     * 错误代码映射表
     * 
     * 将诊断工厂与对应的错误代码关联
     */
    val map: MutableMap<DiagnosticFactory<*>, ErrorCode> = mutableMapOf()

    init {
        map[NO_MULTILINE_NEWLINE] = ErrorCode("460", "多行字符串不是以换行符开头")

        map[EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE] = ErrorCode("176", "期待类型成员或初始化")

        map[FUNCTION_EXPECTED] = ErrorCode("147")
        map[INVISIBLE_MEMBER] = ErrorCode("166")
        map[ARRAY_LITERAL_TYPE_INFERENCE_FAILED] = ErrorCode("112")
        map[NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER] = ErrorCode("693")
        map[TYPE_MISMATCH_MULTIPLE_SUPERTYPES] = ErrorCode("113")

    }

    /**
     * 错误代码数据类
     * 
     * 包含错误代码、错误消息和相关URL
     *
     * @property code 错误代码字符串
     * @property message 错误消息描述
     * @property url 相关文档URL
     */
    data class ErrorCode(
        val code: String,
        val message: String = "",
        val url: String = ""
    )
}
