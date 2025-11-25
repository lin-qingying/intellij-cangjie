package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import lldbprotobuf.Model
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.data.*

import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.result.PagedResult
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.EvalService
import org.cangnova.cangjie.protodebugger.transport.MessageBus


/**
 * 表达式求值服务实现
 */
class EvalServiceImpl(
    private val messageBus: MessageBus, private val driverFacade: DebuggerDriverFacade
) : EvalService {

    @Volatile
    private var valuesFilteringEnabled = false

    override suspend fun evaluate(
        thread: LLDBThread, frame: LLDBFrame, expression: String
    ): LLDBVariable {
        val request = ProtobufFactory.evaluateExpression(
            thread.id, frame.index, expression
        )

        val response = messageBus.request(
            request, ResponseOuterClass.EvaluateResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.message)
        }

        return createLLDBVariable(response.result, expression)
    }

    override suspend fun evaluate(
        valueId: Long, index: Int, expression: String
    ): LLDBVariable {
        val request = ProtobufFactory.evaluateExpression(valueId, index, expression)

        val response = messageBus.request(
            request, ResponseOuterClass.EvaluateResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.message)
        }

        return createLLDBVariable(response.result, expression)
    }

    override suspend fun getVariables(
        thread: LLDBThread,
        frame: LLDBFrame,
        includeArgument: Boolean,
        includeStatics: Boolean,
        includeLocals: Boolean,
        inScopeOnly: Boolean,
        includeRuntimeSupportValues: Boolean,
        useDynamic: Model.DynamicValueType,
        includeRecognizedArguments: Boolean
    ): List<LLDBVariable> {
        return getVariables(
            thread.id.toLong(),
            frame.index,
            includeArgument,
            includeStatics,
            includeLocals,
            inScopeOnly,
            includeRuntimeSupportValues,
            useDynamic,
            includeRecognizedArguments


        )
    }

    override suspend fun getVariables(
        threadId: Long,
        frameIndex: Int,
        includeArgument: Boolean,
        includeStatics: Boolean,
        includeLocals: Boolean,
        inScopeOnly: Boolean,
        includeRuntimeSupportValues: Boolean,
        useDynamic: Model.DynamicValueType,
        includeRecognizedArguments: Boolean
    ): List<LLDBVariable> {
        val request = ProtobufFactory.getVariables(
            threadId,
            frameIndex,
            includeArgument,
            includeStatics,
            includeLocals,
            inScopeOnly,
            includeRuntimeSupportValues,
            useDynamic,
            includeRecognizedArguments
        )
        val result = mutableListOf<LLDBVariable>()
        val errorMessage = Ref<String>()

        val response = messageBus.request(
            request, ResponseOuterClass.VariablesResponse::class.java
        )

        if (!response.status.success) {
            errorMessage.set(response.status.message)
        } else {
            response.variablesList.forEach { lldbValue ->
                result.add(createLLDBVariable(lldbValue, null))
            }
        }

        if (!errorMessage.isNull && !StringUtil.isEmpty(errorMessage.get())) {
            throw DebuggerCommandException(errorMessage.get())
        }

        return result
    }


    override suspend fun getVariableChildren(

        value: LLDBVariable,
        from: Int,
        count: Int
    ): PagedResult<LLDBVariable> {
        if (count == 0) {
            return PagedResult.empty()
        }

        val request = ProtobufFactory.getVariablesChildren(
            variableId = valId(value),

            offset = from,
            count = count
        )

        val response = messageBus.request(
            request, ResponseOuterClass.VariablesChildrenResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.message)
        }

        val children = response.childrenList.map { childValue ->
            createLLDBVariable(childValue, null)
        }
        return if (response.hasMore) {
            PagedResult.partial(children)
        } else {
            PagedResult.complete(children)
        }
    }

    override suspend fun getChildrenCount(value: LLDBVariable): Int {

        TODO()
    }


    override suspend fun getValue(variable: LLDBVariable): LLDBValue {

        if(variable.getUserData(LLDBVARIABLE_VALUE) != null){
            return variable.getUserData(LLDBVARIABLE_VALUE)!!
        }

        val variableId = getValueId(variable)


        val request = ProtobufFactory.getValue(
            variableId = variableId,

            maxStringLength = 1000
        )

        val response = messageBus.request(
            request, ResponseOuterClass.GetValueResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.message)
        }

        val lldbValue = createLLDBValue(response.value)

        variable.putUserData(LLDBVARIABLE_VALUE, lldbValue)

        return lldbValue
    }


    override suspend fun setValue(
        value: LLDBVariable,
        newValue: String
    ): Boolean {
        try {
            val variableId = getValueId(value)

            val request = ProtobufFactory.setVariableValue(
                variableId = variableId,
                newValue = newValue
            )

            val response = messageBus.request(
                request, ResponseOuterClass.SetVariableValueResponse::class.java
            )

            if (!response.status.success) {
                throw DebuggerCommandException(response.status.message)
            }

            // Clear cached value to force reload on next access
            value.putUserData(LLDBVARIABLE_VALUE, null)

            return true
        } catch (e: Exception) {
            throw DebuggerCommandException("Failed to set value: ${e.message}")
        }
    }

    override suspend fun isMutable(value: LLDBVariable): Boolean {
        // 根据变量类型判断是否可修改
        return when (value.valueKind) {
            LLDBVariable.ValueKind.VALUE_LOCAL,
            LLDBVariable.ValueKind.VALUE_ARGUMENT -> true

            LLDBVariable.ValueKind.VALUE_GLOBAL,
            LLDBVariable.ValueKind.VALUE_STATIC -> true

            else -> false
        }
    }

    override suspend fun setValuesFilteringEnabled(enabled: Boolean) {
        valuesFilteringEnabled = enabled
        val request = ProtobufFactory.setValuesFilteringEnabled(enabled)
        messageBus.send(request)
    }

    private fun valId(value: LLDBVariable): Long {
        return getValueId(value)
    }
}
