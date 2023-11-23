package com.huawei.cangjie.idea.debugger.dap

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
@Serializable
class DapInitializeRequest {
//    {"command":"initialize","arguments":{"clientID":"vscode","clientName":"Visual Studio Code","adapterID":"cangjieDebug","pathFormat":"path","linesStartAt1":true,"columnsStartAt1":true,"supportsVariableType":true,"supportsVariablePaging":true,"supportsRunInTerminalRequest":true,"locale":"zh-cn","supportsProgressReporting":true,"supportsInvalidatedEvent":true,"supportsMemoryReferences":true,"supportsArgsCanBeInterpretedByShell":true,"supportsMemoryEvent":true,"supportsStartDebuggingRequest":true},"type":"request","seq":1}
// 使用kotlin json



    val request = InitializeRequest(
        command = "initialize",
        arguments = InitializeArguments(
            clientID = "vscode",
            clientName = "Intellij IDEA",
            adapterID = "cangjieDebug",
            pathFormat = "path",
            linesStartAt1 = true,
            columnsStartAt1 = true,
            supportsVariableType = true,
            supportsVariablePaging = true,
            supportsRunInTerminalRequest = true,
            locale = "zh-cn",
            supportsProgressReporting = true,
            supportsInvalidatedEvent = true,
            supportsMemoryReferences = true,
            supportsArgsCanBeInterpretedByShell = true,
            supportsMemoryEvent = true,
            supportsStartDebuggingRequest = true
        ),
        type = "request",
        seq = 1
    )

    override fun toString(): String {
        return Json.encodeToString(request)
    }
}
@Serializable
data class InitializeArguments(
    val clientID: String,
    val clientName: String,
    val adapterID: String,
    val pathFormat: String,
    val linesStartAt1: Boolean,
    val columnsStartAt1: Boolean,
    val supportsVariableType: Boolean,
    val supportsVariablePaging: Boolean,
    val supportsRunInTerminalRequest: Boolean,
    val locale: String,
    val supportsProgressReporting: Boolean,
    val supportsInvalidatedEvent: Boolean,
    val supportsMemoryReferences: Boolean,
    val supportsArgsCanBeInterpretedByShell: Boolean,
    val supportsMemoryEvent: Boolean,
    val supportsStartDebuggingRequest: Boolean
)
@Serializable
data class InitializeRequest(
    val command: String,
    val arguments: InitializeArguments,
    val type: String,
    val seq: Int
)
