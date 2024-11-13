package com.linqingying.cangjie.dapDebugger.protocol.type.serializer

import com.linqingying.cangjie.dapDebugger.protocol.ProtocolMessage
import com.linqingying.cangjie.dapDebugger.protocol.request.*
import com.linqingying.cangjie.dapDebugger.protocol.response.RunInTerminalResponse
import com.linqingying.cangjie.dapDebugger.protocol.type.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

//val jsonConfiguration = JsonConfiguration(classDiscriminator = "kind")
val format = Json {
//    useArrayPolymorphism = true
    classDiscriminator = "kind"
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
//    不序列化值为null的属性

    serializersModule = SerializersModule {
        polymorphic(ProtocolMessage::class) {
//            polymorphic(Request::class){
            subclass(InitializeRequest::class)
//            }
            subclass(ThreadsRequest::class)
            subclass(SetBreakpointsRequest::class)
            subclass(LaunchRequest::class)
            subclass(ConfigurationDoneRequest::class)
            subclass(RunInTerminalResponse::class)
            subclass(StackTraceRequest::class)
            subclass(DisconnectRequest::class)
            subclass(ScopesRequest::class)
            subclass(SetInstructionBreakpointsRequest::class)
            subclass(DebugInConsoleRequest::class)
            subclass(SetDataBreakpointsRequest::class)
            subclass(SetFunctionBreakpointsRequest::class)
            subclass(VariablesRequest::class)

            subclass(NextRequest::class)

            subclass(StepInRequest::class)
            subclass(StepOutRequest::class)
            subclass(ContinueRequest::class)
            subclass(SourceRequest::class)
            subclass(SetVariableRequest::class)
            subclass(EvaluateRequest::class)


        }
    }

}


@Serializer(forClass = EventReason::class)
object EventReasonSerializer : KSerializer<EventReason> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EventReason") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: EventReason) {
        when (value) {
            is EventReason.changed -> encoder.encodeString("changed")
            is EventReason.new -> encoder.encodeString("new")
            is EventReason.removed -> encoder.encodeString("removed")
            is EventReason.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): EventReason {
        val value = decoder.decodeString()
        return when (value) {
            "changed" -> EventReason.changed
            "new" -> EventReason.new
            "removed" -> EventReason.removed
            else -> EventReason.Other(value)
        }
    }
}

@Serializer(forClass = PathFormat::class)
object PathFormatSerializer : KSerializer<PathFormat> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PathFormat") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: PathFormat) {
        when (value) {
            is PathFormat.Path -> encoder.encodeString("path")
            is PathFormat.Uri -> encoder.encodeString("uri")
            is PathFormat.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): PathFormat {
        val value = decoder.decodeString()
        return when (value) {
            "path" -> PathFormat.Path
            "uri" -> PathFormat.Uri
            else -> PathFormat.Other(value)
        }
    }
}

@Serializer(forClass = ColumnDataType::class)
object ColumnDataTypeSerializer : KSerializer<ColumnDataType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ColumnDataType") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ColumnDataType) {
        when (value) {
            is ColumnDataType.string -> encoder.encodeString("string")
            is ColumnDataType.number -> encoder.encodeString("number")
            is ColumnDataType.boolean -> encoder.encodeString("boolean")
            is ColumnDataType.usnixTimestampUTC -> encoder.encodeString("unixTimestampUTC")
            is ColumnDataType.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): ColumnDataType {
        val value = decoder.decodeString()
        return when (value) {
            "string" -> ColumnDataType.string
            "number" -> ColumnDataType.number
            "boolean" -> ColumnDataType.boolean
            "unixTimestampUTC" -> ColumnDataType.usnixTimestampUTC
            else -> ColumnDataType.Other(value)
        }
    }
}

@Serializer(forClass = MessageType::class)
object MessageTypeSerializer : KSerializer<MessageType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MessageType") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: MessageType) {
        when (value) {
            is MessageType.request -> encoder.encodeString("request")
            is MessageType.response -> encoder.encodeString("response")
            is MessageType.event -> encoder.encodeString("event")
            is MessageType.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): MessageType {
        val value = decoder.decodeString()
        return when (value) {
            "request" -> MessageType.request
            "response" -> MessageType.response
            "event" -> MessageType.event
            else -> MessageType.Other(value)
        }
    }
}

@Serializer(forClass = Category::class)
object CategorySerializer : KSerializer<Category> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ColumnDataType") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: Category) {
        when (value) {
            is Category.console -> encoder.encodeString("console")
            is Category.important -> encoder.encodeString("important")
            is Category.stdout -> encoder.encodeString("stdout")
            is Category.stderr -> encoder.encodeString("stderr")
            is Category.telemetry -> encoder.encodeString("telemetry")
            is Category.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): Category {
        val value = decoder.decodeString()
        return when (value) {
            "console" -> Category.console
            "important" -> Category.important
            "stdout" -> Category.stdout
            "stderr" -> Category.stderr
            "telemetry" -> Category.telemetry
            else -> Category.Other(value)
        }
    }
}

@Serializer(forClass = ThreadEventReason::class)
object ThreadEventReasonSerializer : KSerializer<ThreadEventReason> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ColumnDataType") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ThreadEventReason) {
        when (value) {
            is ThreadEventReason.started -> encoder.encodeString("started")
            is ThreadEventReason.exited -> encoder.encodeString("exited")

            is ThreadEventReason.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): ThreadEventReason {
        val value = decoder.decodeString()
        return when (value) {
            "started" -> ThreadEventReason.started
            "exited" -> ThreadEventReason.exited

            else -> ThreadEventReason.Other(value)
        }
    }
}

@Serializer(forClass = VariablePresentationHintKind::class)
object VariablePresentationHintKindSerializer : KSerializer<VariablePresentationHintKind> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Kind") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: VariablePresentationHintKind) {
        when (value) {
            is VariablePresentationHintKind.Property -> encoder.encodeString("property")
            is VariablePresentationHintKind.Method -> encoder.encodeString("method")
            is VariablePresentationHintKind.Class -> encoder.encodeString("class")
            is VariablePresentationHintKind.Data -> encoder.encodeString("data")
            is VariablePresentationHintKind.Event -> encoder.encodeString("event")
            is VariablePresentationHintKind.BaseClass -> encoder.encodeString("baseClass")
            is VariablePresentationHintKind.InnerClass -> encoder.encodeString("innerClass")
            is VariablePresentationHintKind.Interface -> encoder.encodeString("interface")
            is VariablePresentationHintKind.MostDerivedClass -> encoder.encodeString("mostDerivedClass")
            is VariablePresentationHintKind.Virtual -> encoder.encodeString("virtual")
            is VariablePresentationHintKind.DataBreakpoint -> encoder.encodeString("dataBreakpoint")
            is VariablePresentationHintKind.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): VariablePresentationHintKind {
        val value = decoder.decodeString()
        return when (value) {
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


@Serializer(forClass = VariablePresentationHintAttributes::class)
object VariablePresentationHintAttributesSerializer : KSerializer<VariablePresentationHintAttributes> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Attributes") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: VariablePresentationHintAttributes) {
        when (value) {
            is VariablePresentationHintAttributes.Static -> encoder.encodeString("static")
            is VariablePresentationHintAttributes.Constant -> encoder.encodeString("constant")
            is VariablePresentationHintAttributes.ReadOnly -> encoder.encodeString("readOnly")
            is VariablePresentationHintAttributes.RawString -> encoder.encodeString("rawString")
            is VariablePresentationHintAttributes.HasObjectId -> encoder.encodeString("hasObjectId")
            is VariablePresentationHintAttributes.CanHaveObjectId -> encoder.encodeString("canHaveObjectId")
            is VariablePresentationHintAttributes.HasSideEffects -> encoder.encodeString("hasSideEffects")
            is VariablePresentationHintAttributes.HasDataBreakpoint -> encoder.encodeString("hasDataBreakpoint")
            is VariablePresentationHintAttributes.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): VariablePresentationHintAttributes {
        val value = decoder.decodeString()
        return when (value) {
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

@Serializer(forClass = VariablePresentationHintVisibility::class)
object VariablePresentationHintVisibilitySerializer : KSerializer<VariablePresentationHintVisibility> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Visibility") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: VariablePresentationHintVisibility) {
        when (value) {
            is VariablePresentationHintVisibility.Public -> encoder.encodeString("public")
            is VariablePresentationHintVisibility.Private -> encoder.encodeString("private")
            is VariablePresentationHintVisibility.Protected -> encoder.encodeString("protected")
            is VariablePresentationHintVisibility.Internal -> encoder.encodeString("internal")
            is VariablePresentationHintVisibility.Final -> encoder.encodeString("final")
            is VariablePresentationHintVisibility.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): VariablePresentationHintVisibility {
        val value = decoder.decodeString()
        return when (value) {
            "public" -> VariablePresentationHintVisibility.Public
            "private" -> VariablePresentationHintVisibility.Private
            "protected" -> VariablePresentationHintVisibility.Protected
            "internal" -> VariablePresentationHintVisibility.Internal
            "final" -> VariablePresentationHintVisibility.Final
            else -> VariablePresentationHintVisibility.Other(value)
        }
    }
}


@Serializer(forClass = StoppedEventReason::class)
object StoppedEventReasonSerializer : KSerializer<StoppedEventReason> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StoppedEventReason") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: StoppedEventReason) {
        when (value) {
            is StoppedEventReason.Step -> encoder.encodeString("layer")
            is StoppedEventReason.Breakpoint -> encoder.encodeString("breakpoint")
            is StoppedEventReason.Exception -> encoder.encodeString("exception")
            is StoppedEventReason.Pause -> encoder.encodeString("pause")
            is StoppedEventReason.Entry -> encoder.encodeString("entry")
            is StoppedEventReason.DataBreakpoint -> encoder.encodeString("data breakpoint")
            is StoppedEventReason.Goto -> encoder.encodeString("goto")
            is StoppedEventReason.FunctionBreakpoint -> encoder.encodeString("function breakpoint")
            is StoppedEventReason.InstructionBreakpoint -> encoder.encodeString("instruction breakpoint")


            is StoppedEventReason.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): StoppedEventReason {
        val value = decoder.decodeString()
        return when (value) {
            "layer" -> StoppedEventReason.Step
            "breakpoint" -> StoppedEventReason.Breakpoint
            "exception" -> StoppedEventReason.Exception
            "pause" -> StoppedEventReason.Pause
            "entry" -> StoppedEventReason.Entry
            "data breakpoint" -> StoppedEventReason.DataBreakpoint
            "goto" -> StoppedEventReason.Goto
            "function breakpoint" -> StoppedEventReason.FunctionBreakpoint
            "instruction breakpoint" -> StoppedEventReason.InstructionBreakpoint
            else -> StoppedEventReason.Other(value)
        }
    }
}


@Serializer(forClass = ScopePresentationHint::class)
object ScopePresentationHintSerializer : KSerializer<ScopePresentationHint> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ScopePresentationHint") {
        element<String>("value", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ScopePresentationHint) {
        when (value) {
            is ScopePresentationHint.Arguments -> encoder.encodeString("arguments")
            is ScopePresentationHint.Locals -> encoder.encodeString("locals")
            is ScopePresentationHint.Registers -> encoder.encodeString("registers")
            is ScopePresentationHint.All -> encoder.encodeString("all")
            is ScopePresentationHint.This -> encoder.encodeString("this")

            is ScopePresentationHint.Statics -> encoder.encodeString("statics")
            is ScopePresentationHint.Globals -> encoder.encodeString("globals")
            is ScopePresentationHint.Other -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): ScopePresentationHint {
        val value = decoder.decodeString()
        return when (value) {
            "arguments" -> ScopePresentationHint.Arguments
            "locals" -> ScopePresentationHint.Locals
            "registers" -> ScopePresentationHint.Registers
            "all" -> ScopePresentationHint.All
            "this" -> ScopePresentationHint.This
            "statics" -> ScopePresentationHint.Statics
            "globals" -> ScopePresentationHint.Globals
            else -> ScopePresentationHint.Other(value)
        }
    }
}


@Serializer(forClass = EvaluateArgumentsContext::class)
object EvaluateArgumentsContextSerializer : KSerializer<EvaluateArgumentsContext> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("EvaluateArgumentsContext") {
        element<String>("value", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): EvaluateArgumentsContext {
        val value = decoder.decodeString()
        return when (value){
            "watch" -> EvaluateArgumentsContext.Watch
            "hover" -> EvaluateArgumentsContext.Hover
            "repl" -> EvaluateArgumentsContext.Repl
            "clipboard" -> EvaluateArgumentsContext.Clipboard
            "variables" -> EvaluateArgumentsContext.Variables

            else -> EvaluateArgumentsContext.Other(value)
        }

    }


    override fun serialize(encoder: Encoder, value: EvaluateArgumentsContext) {
        when (value) {
            is EvaluateArgumentsContext.Watch -> encoder.encodeString("watch")
            is EvaluateArgumentsContext.Hover -> encoder.encodeString("hover")
            is EvaluateArgumentsContext.Repl -> encoder.encodeString("repl")
            is EvaluateArgumentsContext.Clipboard -> encoder.encodeString("clipboard")
            is EvaluateArgumentsContext.Variables -> encoder.encodeString("variables")

            is EvaluateArgumentsContext.Other -> encoder.encodeString(value.value)

        }
    }
}
