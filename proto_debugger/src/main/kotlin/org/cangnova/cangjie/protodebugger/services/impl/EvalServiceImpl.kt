package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.data.*
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.output.ResultList
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.EvalService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import proto.Model
import proto.ProtocolResponses

/**
 * 表达式求值服务实现
 */
class EvalServiceImpl(
    private val messageBus: MessageBus,
    private val configuration: DebuggerDriverConfiguration
) : EvalService {

    @Volatile
    private var valuesFilteringEnabled = false

    override suspend fun evaluate(
        thread: LLThread,
        frame: LLFrame,
        expression: String
    ): LLValue {
        val request = ProtobufMessageFactory.evaluateExpression(
            thread.id,
            frame.index,
            expression
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.EvaluateExpressionResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return createLLValue(response.result, expression, ::loadValueDataForValue)
    }

    override suspend fun evaluate(
        valueId: Long,
        index: Int,
        expression: String
    ): LLValue {
        val request = ProtobufMessageFactory.evaluateExpression(valueId, index, expression)

        val response = messageBus.request(
            request,
            ProtocolResponses.EvaluateExpressionResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return createLLValue(response.result, expression, ::loadValueDataForValue)
    }

    override suspend fun getVariables(
        thread: LLThread,
        frame: LLFrame
    ): List<LLValue> {
        return getVariables(thread.id, frame.index)
    }

    override suspend fun getVariables(
        threadId: Long,
        frameIndex: Int,
        statics: Boolean,
        globals: Boolean
    ): List<LLValue> {
        val request = ProtobufMessageFactory.getVars(threadId, frameIndex, statics, globals)
        val result = mutableListOf<LLValue>()
        val errorMessage = Ref<String>()

        val response = messageBus.request(
            request,
            ProtocolResponses.GetVariablesResponse::class.java
        )

        if (!response.status.success) {
            errorMessage.set(response.status.errorMessage)
        } else {
            response.variablesList.forEach { lldbValue ->
                result.add(createLLValue(lldbValue, null, ::loadValueDataForValue))
            }
        }

        if (!errorMessage.isNull && !StringUtil.isEmpty(errorMessage.get())) {
            throw DebuggerCommandException(errorMessage.get())
        }

        return result
    }

    override suspend fun getVariableChildren(
        value: LLValue,
        from: Int,
        count: Int
    ): ResultList<LLValue> {
        val childrenCount = getChildrenCount(value)

        if (count == 0) {
            return ResultList.empty()
        }

        val request = ProtobufMessageFactory.getValueChildren(
            valId(value),
            from,
            count
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.GetValueChildrenResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        val children = response.childrenList.map { childValue ->
            createLLValue(childValue, null, ::loadValueDataForValue)
        }
        val hasMore = from + count < childrenCount
        return ResultList.create(children, hasMore)
    }

    override suspend fun getChildrenCount(value: LLValue): Int {
        val request = ProtobufMessageFactory.getChildrenCount(valId(value))

        val response = messageBus.request(
            request,
            ProtocolResponses.GetChildrenCountResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return response.count
    }

    override suspend fun getData(value: LLValue): LLValueData {
        return loadValueData(value)
    }

    override suspend fun getDescription(value: LLValue, maxLength: Int): String {
        val request = ProtobufMessageFactory.getValueDescription(valId(value), maxLength)
        val description = Ref.create<String>()
        val exception = Ref.create<DebuggerCommandException>()

        val response = messageBus.request(
            request,
            ProtocolResponses.GetValueDescriptionResponse::class.java
        )

        if (!response.status.success) {
            exception.set(DebuggerCommandException(response.status.errorMessage))
        } else {
            if (response.description != null) {
                description.set(response.description)
            }
        }

        if (!exception.isNull) {
            throw exception.get()
        }

        return description.get()
    }

    override fun getValueAddress(value: LLValue): Long {
        return value.address ?: throw ExecutionException("Value address is null for $value")
    }

    override suspend fun setValuesFilteringEnabled(enabled: Boolean) {
        valuesFilteringEnabled = enabled
        val request = ProtobufMessageFactory.setValuesFilteringEnabled(enabled)
        messageBus.send(request)
    }

    private fun valId(value: LLValue): Int {
        return getValueId(value)
    }

    private fun loadValueDataForValue(value: LLValue): LLValueData {
        val req = ProtobufMessageFactory.getValueData(valId(value), 256)
        val lldbDataRef = Ref.create<Model.ValueData>()
        val exception = Ref.create<DebuggerCommandException>()

        val response = try {
            kotlinx.coroutines.runBlocking {
                messageBus.request(
                    req,
                    ProtocolResponses.GetValueDataResponse::class.java
                )
            }
        } catch (e: Exception) {
            throw DebuggerCommandException("Failed to load value data: ${e.message}")
        }

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        lldbDataRef.set(response.data)

        val lldbData = lldbDataRef.get()
        return LLValueData(
            lldbData.value,
            lldbData.summary,
            lldbData.hasExtendedDescription,
            lldbData.hasChildren,
            lldbData.isSynthetic
        )
    }
}
