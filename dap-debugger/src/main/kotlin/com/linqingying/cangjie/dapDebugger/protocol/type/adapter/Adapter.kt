package com.linqingying.cangjie.dapDebugger.protocol.type.adapter

import com.squareup.moshi.*
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.linqingying.cangjie.dapDebugger.protocol.ProtocolMessage
import com.linqingying.cangjie.dapDebugger.protocol.event.*
import com.linqingying.cangjie.dapDebugger.protocol.request.Request
import com.linqingying.cangjie.dapDebugger.protocol.request.RunInTerminalRequest
import com.linqingying.cangjie.dapDebugger.protocol.response.*
import com.linqingying.cangjie.dapDebugger.protocol.type.*
import com.linqingying.cangjie.dapDebugger.protocol.type.body.*


val protocolMessageFactory: PolymorphicJsonAdapterFactory<ProtocolMessage> =
    PolymorphicJsonAdapterFactory.of(ProtocolMessage::class.java, "type")
//    .withSubtype(Request::class.java, "request")
        .withSubtype(Response::class.java, "response")
        .withSubtype(Event::class.java, "event")
        .withSubtype(Request::class.java, "request")

val moshi: Moshi = Moshi.Builder()
    .add(MessageTypeAdapter())
    .add(ResponseMessageAdapter())
    .add(BodyAdapter())
    .add(ColumnDataTypeAdapter())
    .add(PathFormatAdapter())
    .add(VariablePresentationHintKindAdapter)
    .add(ScopePresentationHintAdapter)
    .add(EvaluateArgumentsContextAdapter)
    .add(BreakpointEventReasonAdapter())
    .add(StoppedEventReasonAdapter)
    .add(ThreadEventReasonAdapter())
    .add(VariablePresentationHintAttributesAdapter)
    .add(VariablePresentationHintVisibilityAdapter)
    .add(CategoryAdapter())
    .add(protocolMessageFactory)

    // 添加多态 JsonAdapter 工厂
//    .add(
//        PolymorphicJsonAdapterFactory.of(Response::class.java, "command")// 基础类型：Game::class.java, 标签Key：type
//            .withSubtype(InitializeResponse::class.java, "initialize")          // 子类型：Zelda::class.java,标签Value：zelda
//            .withSubtype(CancelResponse::class.java, "chancel") // 子类型：EldenRing::class.java,标签Value: elden-ring
////           如果success为false，则为ErrorResponse
//
//    )
    .add(ResponseAdapter())
    .add(EvnetAdapter())
    .add(RequestAdapter)
    .add(KotlinJsonAdapterFactory())
    .build()


object RequestAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, request: Request) {

    }

    @FromJson
    fun formJson(reader: JsonReader): Request {
        val reader1 = reader.peekJson()
        val jsonObject = reader1.readJsonValue() as? Map<*, *>
            ?: throw JsonDataException("Expected a JsonObject")

        return when {
            jsonObject.containsKey("command") && jsonObject["command"] == "runInTerminal" -> moshi.adapter(
                RunInTerminalRequest::class.java
            )
                .fromJson(reader)!!

            else -> throw JsonDataException("Expected a JsonObject")
        }

    }

}

class EvnetAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, event: Event) {

    }

    @FromJson
    fun fromJson(reader: JsonReader): Event {
        val reader1 = reader.peekJson()
        val jsonObject = reader1.readJsonValue() as? Map<*, *>
            ?: throw JsonDataException("Expected a JsonObject")

        return when {

            jsonObject.containsKey("event") && jsonObject["event"] == "initialized" -> moshi.adapter(InitializedEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "output" -> moshi.adapter(OutputEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "breakpoint" -> moshi.adapter(BreakpointEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "continued" -> moshi.adapter(ContinuedEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "exited" -> moshi.adapter(ExitedEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "terminated" -> moshi.adapter(TerminatedEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "stopped" -> moshi.adapter(StoppedEvent::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("event") && jsonObject["event"] == "modules" -> moshi.adapter(ModuleEvent::class.java)
                .fromJson(reader)!!

            else -> throw JsonDataException("Expected a JsonObject to ${jsonObject["event"]}")
        }
    }
}

class ResponseAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, response: Response) {
        when (response) {
            is InitializeResponse -> moshi.adapter(InitializeResponse::class.java).toJson(writer, response)
            is CancelResponse -> moshi.adapter(CancelResponse::class.java).toJson(writer, response)
            is ErrorResponse -> moshi.adapter(ErrorResponse::class.java).toJson(writer, response)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): Response {
//            沈拷贝
        val reader1 = reader.peekJson()

        val jsonObject = reader1.readJsonValue() as? Map<*, *>
            ?: throw JsonDataException("Expected a JsonObject")

        return when {

//            TODO 是否需要解析到ErrorResponse
            jsonObject.containsKey("success") && jsonObject["success"] == false ->
                moshi.adapter(ErrorResponse::class.java).fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "disconnect" ->
                moshi.adapter(DisconnectResponse::class.java).fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "initialize" ->
                moshi.adapter(InitializeResponse::class.java).fromJson(reader)!!
//                format.decodeFromString<InitializeResponse>(reader.readJsonValue().toString())

            jsonObject.containsKey("command") && jsonObject["command"] == "cancel" -> moshi.adapter(CancelResponse::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "launch" -> moshi.adapter(LaunchResponse::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "setBreakpoints" -> moshi.adapter(
                SetBreakpointsResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "configurationDone" -> moshi.adapter(
                ConfigurationDoneResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "debugInConsole" -> moshi.adapter(
                DebugInConsoleResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "setFunctionBreakpoints" -> moshi.adapter(
                SetFunctionBreakpointsResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "setDataBreakpoints" -> moshi.adapter(
                SetDataBreakpointsResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "setInstructionBreakpoints" -> moshi.adapter(
                SetInstructionBreakpointsResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "threads" -> moshi.adapter(
                ThreadsResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "stackTrace" -> moshi.adapter(
                StackTraceResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "scopes" -> moshi.adapter(
                ScopesResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "setVariable" -> moshi.adapter(
                SetVariableResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "variables" -> moshi.adapter(
                VariablesResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "next" -> moshi.adapter(
                NextResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "stepIn" -> moshi.adapter(
                StepInResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "stepOut" -> moshi.adapter(
                StepOutResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "source" -> moshi.adapter(
                SourceResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "continue" -> moshi.adapter(
                ContinueResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "evaluate" -> moshi.adapter(
                EvaluateResponse::class.java
            )
                .fromJson(reader)!!

            jsonObject.containsKey("success") && !(jsonObject["success"] as Boolean) -> moshi.adapter(ErrorResponse::class.java)
                .fromJson(reader)!!

            else -> throw JsonDataException("Expected a JsonObject to command ${jsonObject["command"]} ")
        }
    }
}


class PathFormatAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, pathFormat: PathFormat) {
        when (pathFormat) {
            is PathFormat.Path -> writer.value("path")
            is PathFormat.Uri -> writer.value("uri")
            is PathFormat.Other -> writer.value(pathFormat.value)


        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): PathFormat {
        return when (val value = reader.nextString()) {
            "path" -> PathFormat.Path
            "uri" -> PathFormat.Uri
            else -> PathFormat.Other(value)
        }
    }
}

class BodyAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, body: Body) {
        when (body) {

            is ErrorBody -> moshi.adapter(ErrorBody::class.java).toJson(writer, body)
            is Capabilities -> moshi.adapter(Capabilities::class.java).toJson(writer, body)
            is ThreadEventBody -> moshi.adapter(ThreadEventBody::class.java).toJson(writer, body)

            is SetBreakpointsResponseBody -> moshi.adapter(SetBreakpointsResponseBody::class.java).toJson(writer, body)
            is OutPutEventBody -> moshi.adapter(OutPutEventBody::class.java).toJson(writer, body)
            is BreakpointEventBody -> moshi.adapter(BreakpointEventBody::class.java).toJson(writer, body)

            is LoadedSourceEventBody -> moshi.adapter(LoadedSourceEventBody::class.java).toJson(writer, body)

            is RunInTerminalResponseBody -> moshi.adapter(RunInTerminalResponseBody::class.java).toJson(writer, body)

            is DebugInConsoleResponseBody -> moshi.adapter(DebugInConsoleResponseBody::class.java).toJson(writer, body)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): Body? {
        val jsonObject = reader.readJsonValue() as? Map<*, *>
            ?: throw JsonDataException("Expected a JsonObject")
        return when {
            jsonObject.containsKey("success") && !(jsonObject["success"] as Boolean) -> moshi.adapter(ErrorBody::class.java)
                .fromJson(reader)!!

            jsonObject.containsKey("command") && jsonObject["command"] == "initialize" -> moshi.adapter(Capabilities::class.java)
                .fromJson(reader)!!

//            jsonObject.containsKey("command") && jsonObject["command"] == "initialize" -> moshi.adapter(Capabilities::class.java)
//                .fromJson(reader)!!

            else -> null
//            else ->     throw JsonDataException("Expected a JsonObject to ${jsonObject["command"]}")
        }
    }
}

class ResponseMessageAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, responseMessage: ResponseMessage) {
        when (responseMessage) {
            is ResponseMessage.cancelled -> writer.value("cancelled")
            is ResponseMessage.notStopped -> writer.value("notStopped")
            is ResponseMessage.Other -> writer.value(responseMessage.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): ResponseMessage {
        return when (val value = reader.nextString()) {
            "cancelled" -> ResponseMessage.cancelled
            "notStopped" -> ResponseMessage.notStopped
            else -> ResponseMessage.Other(value)
        }
    }
}

class MessageTypeAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, messageType: MessageType) {
        when (messageType) {
            is MessageType.request -> writer.value("request")
            is MessageType.response -> writer.value("response")
            is MessageType.event -> writer.value("event")
            is MessageType.Other -> writer.value(messageType.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): MessageType {
        return when (val value = reader.nextString()) {
            "request" -> MessageType.request
            "response" -> MessageType.response
            "event" -> MessageType.event
            else -> MessageType.Other(value)
        }
    }
}

class ColumnDataTypeAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, columnDataType: ColumnDataType) {
        when (columnDataType) {
            is ColumnDataType.string -> writer.value("string")
            is ColumnDataType.number -> writer.value("number")
            is ColumnDataType.boolean -> writer.value("boolean")
            is ColumnDataType.usnixTimestampUTC -> writer.value("usnixTimestampUTC")
            is ColumnDataType.Other -> writer.value(columnDataType.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): ColumnDataType {
        return when (val value = reader.nextString()) {
            "string" -> ColumnDataType.string
            "number" -> ColumnDataType.number
            "boolean" -> ColumnDataType.boolean
            "usnixTimestampUTC" -> ColumnDataType.usnixTimestampUTC
            else -> ColumnDataType.Other(value)
        }
    }
}

class BreakpointEventReasonAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, breakpointEventReason: EventReason) {
        when (breakpointEventReason) {
            is EventReason.changed -> writer.value("changed")
            is EventReason.new -> writer.value("new")
            is EventReason.removed -> writer.value("removed")
            is EventReason.Other -> writer.value(breakpointEventReason.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): EventReason {
        return when (val value = reader.nextString()) {
            "changed" -> EventReason.changed
            "new" -> EventReason.new
            "removed" -> EventReason.removed
            else -> EventReason.Other(value)
        }
    }
}


class CategoryAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, category: Category) {

        when (category) {
            is Category.console -> writer.value("console")
            is Category.important -> writer.value("important")
            is Category.stdout -> writer.value("stdout")
            is Category.stderr -> writer.value("stderr")
            is Category.telemetry -> writer.value("telemetry")
            is Category.Other -> writer.value(category.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): Category {
        return when (val value = reader.nextString()) {
            "console" -> Category.console
            "important" -> Category.important
            "stdout" -> Category.stdout
            "stderr" -> Category.stderr
            "telemetry" -> Category.telemetry
            else -> Category.Other(value)
        }
    }
}


class ThreadEventReasonAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, category: ThreadEventReason) {

        when (category) {
            is ThreadEventReason.started -> writer.value("started")
            is ThreadEventReason.exited -> writer.value("exited")

            is ThreadEventReason.Other -> writer.value(category.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): ThreadEventReason {
        return when (val value = reader.nextString()) {
            "started" -> ThreadEventReason.started
            "exited" -> ThreadEventReason.exited

            else -> ThreadEventReason.Other(value)
        }
    }
}

object VariablePresentationHintKindAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, kind: VariablePresentationHintKind) {
        when (kind) {
            is VariablePresentationHintKind.Property -> writer.value("property")
            is VariablePresentationHintKind.Method -> writer.value("method")
            is VariablePresentationHintKind.Class -> writer.value("class")
            is VariablePresentationHintKind.Data -> writer.value("data")
            is VariablePresentationHintKind.Event -> writer.value("event")
            is VariablePresentationHintKind.BaseClass -> writer.value("baseClass")
            is VariablePresentationHintKind.InnerClass -> writer.value("innerClass")
            is VariablePresentationHintKind.Interface -> writer.value("interface")
            is VariablePresentationHintKind.MostDerivedClass -> writer.value("mostDerivedClass")
            is VariablePresentationHintKind.Virtual -> writer.value("virtual")
            is VariablePresentationHintKind.DataBreakpoint -> writer.value("dataBreakpoint")
            is VariablePresentationHintKind.Other -> writer.value(kind.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): VariablePresentationHintKind {
        return when (val value = reader.nextString()) {
            "property" -> VariablePresentationHintKind.Property
            "method" -> VariablePresentationHintKind.Method
            "class" -> VariablePresentationHintKind.Class
            "data" -> VariablePresentationHintKind.Data
            "event" -> VariablePresentationHintKind.Event
            "baseClass" -> VariablePresentationHintKind.BaseClass
            "innerClass" -> VariablePresentationHintKind.InnerClass
            "interface" -> VariablePresentationHintKind.Interface
            "mostDerivedClass" -> VariablePresentationHintKind.MostDerivedClass
            "virtual" -> VariablePresentationHintKind.Virtual
            "dataBreakpoint" -> VariablePresentationHintKind.DataBreakpoint
            else -> VariablePresentationHintKind.Other(value)
        }
    }
}

object VariablePresentationHintAttributesAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, attributes: VariablePresentationHintAttributes) {
        when (attributes) {
            is VariablePresentationHintAttributes.Static -> writer.value("static")
            is VariablePresentationHintAttributes.Constant -> writer.value("constant")
            is VariablePresentationHintAttributes.ReadOnly -> writer.value("readOnly")
            is VariablePresentationHintAttributes.RawString -> writer.value("rawString")
            is VariablePresentationHintAttributes.HasObjectId -> writer.value("hasObjectId")
            is VariablePresentationHintAttributes.CanHaveObjectId -> writer.value("canHaveObjectId")
            is VariablePresentationHintAttributes.HasSideEffects -> writer.value("hasSideEffects")
            is VariablePresentationHintAttributes.HasDataBreakpoint -> writer.value("hasDataBreakpoint")
            is VariablePresentationHintAttributes.Other -> writer.value(attributes.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): VariablePresentationHintAttributes {
        return when (val value = reader.nextString()) {
            "static" -> VariablePresentationHintAttributes.Static
            "constant" -> VariablePresentationHintAttributes.Constant
            "readOnly" -> VariablePresentationHintAttributes.ReadOnly
            "rawString" -> VariablePresentationHintAttributes.RawString
            "hasObjectId" -> VariablePresentationHintAttributes.HasObjectId
            "canHaveObjectId" -> VariablePresentationHintAttributes.CanHaveObjectId
            "hasSideEffects" -> VariablePresentationHintAttributes.HasSideEffects
            "hasDataBreakpoint" -> VariablePresentationHintAttributes.HasDataBreakpoint
            else -> VariablePresentationHintAttributes.Other(value)
        }
    }
}

object VariablePresentationHintVisibilityAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, visibility: VariablePresentationHintVisibility) {
        when (visibility) {
            is VariablePresentationHintVisibility.Public -> writer.value("public")
            is VariablePresentationHintVisibility.Private -> writer.value("private")
            is VariablePresentationHintVisibility.Protected -> writer.value("protected")
            is VariablePresentationHintVisibility.Internal -> writer.value("internal")
            is VariablePresentationHintVisibility.Final -> writer.value("final")
            is VariablePresentationHintVisibility.Other -> writer.value(visibility.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): VariablePresentationHintVisibility {
        return when (val value = reader.nextString()) {
            "public" -> VariablePresentationHintVisibility.Public
            "private" -> VariablePresentationHintVisibility.Private
            "protected" -> VariablePresentationHintVisibility.Protected
            "internal" -> VariablePresentationHintVisibility.Internal
            "final" -> VariablePresentationHintVisibility.Final
            else -> VariablePresentationHintVisibility.Other(value)
        }
    }
}


object StoppedEventReasonAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, stoppedEventReason: StoppedEventReason) {
        when (stoppedEventReason) {
            is StoppedEventReason.Step -> writer.value("layer")
            is StoppedEventReason.Breakpoint -> writer.value("breakpoint")
            is StoppedEventReason.Exception -> writer.value("exception")
            is StoppedEventReason.Pause -> writer.value("pause")
            is StoppedEventReason.Entry -> writer.value("entry")
            is StoppedEventReason.FunctionBreakpoint -> writer.value("function breakpoint")
            is StoppedEventReason.InstructionBreakpoint -> writer.value("instruction breakpoint")
            is StoppedEventReason.DataBreakpoint -> writer.value("data breakpoint")
            is StoppedEventReason.Goto -> writer.value("goto")
            is StoppedEventReason.Other -> writer.value(stoppedEventReason.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): StoppedEventReason {
        return when (val value = reader.nextString()) {
            "layer" -> StoppedEventReason.Step
            "breakpoint" -> StoppedEventReason.Breakpoint
            "exception" -> StoppedEventReason.Exception
            "pause" -> StoppedEventReason.Pause
            "entry" -> StoppedEventReason.Entry
            "function breakpoint" -> StoppedEventReason.FunctionBreakpoint
            "instruction breakpoint" -> StoppedEventReason.InstructionBreakpoint

            "data breakpoint" -> StoppedEventReason.DataBreakpoint
            "goto" -> StoppedEventReason.Goto
            else -> StoppedEventReason.Other(value)
        }
    }
}

object ScopePresentationHintAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, scopePresentationHint: ScopePresentationHint) {
        when (scopePresentationHint) {
            is ScopePresentationHint.Arguments -> writer.value("arguments")
            is ScopePresentationHint.Locals -> writer.value("locals")
            is ScopePresentationHint.Registers -> writer.value("registers")
            is ScopePresentationHint.Statics -> writer.value("statics")
            is ScopePresentationHint.Globals -> writer.value("globals")
            is ScopePresentationHint.This -> writer.value("this")
            is ScopePresentationHint.All -> writer.value("all")
            is ScopePresentationHint.Other -> writer.value(scopePresentationHint.value)
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): ScopePresentationHint {
        return when (val value = reader.nextString()) {
            "arguments" -> ScopePresentationHint.Arguments
            "locals" -> ScopePresentationHint.Locals
            "registers" -> ScopePresentationHint.Registers
            "this" -> ScopePresentationHint.This
            "all" -> ScopePresentationHint.All
            "statics" -> ScopePresentationHint.Statics
            "globals" -> ScopePresentationHint.Globals
            else -> ScopePresentationHint.Other(value)
        }
    }
}

object EvaluateArgumentsContextAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): EvaluateArgumentsContext {
        return when (val value = reader.nextString()) {
            "watch" -> EvaluateArgumentsContext.Watch
            "hover" -> EvaluateArgumentsContext.Hover
            "repl" -> EvaluateArgumentsContext.Repl
            "clipboard" -> EvaluateArgumentsContext.Clipboard
            "variables" -> EvaluateArgumentsContext.Variables
            else -> EvaluateArgumentsContext.Other(value)
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, evaluateArgumentsContext: EvaluateArgumentsContext) {
        when (evaluateArgumentsContext) {
            is EvaluateArgumentsContext.Watch -> writer.value("watch")
            is EvaluateArgumentsContext.Hover -> writer.value("hover")
            is EvaluateArgumentsContext.Repl -> writer.value("repl")
            is EvaluateArgumentsContext.Clipboard -> writer.value("clipboard")
            is EvaluateArgumentsContext.Variables -> writer.value("variables")
            is EvaluateArgumentsContext.Other -> writer.value(evaluateArgumentsContext.value)
        }
    }
}
