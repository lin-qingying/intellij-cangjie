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

enum class Severity {
    INFO,
    ERROR,
    WARNING
}

/**
 * 仓颉错误代码
 */
object ErrorCodes {


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

    data class ErrorCode(
        val code: String,

        val message: String = "",

        val url: String = ""
    )
}
