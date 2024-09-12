package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.diagnostics.Errors.*

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
