package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.ArgumentsToParametersMapper
import com.huawei.cangjie.types.ErrorUtils



fun ResolvedCall<*>.isReallySuccess(): Boolean = status.isSuccess && !ErrorUtils.isError(resultingDescriptor)


enum class ArgumentMatchStatus(val isError: Boolean = true) {
    SUCCESS(false),
    TYPE_MISMATCH(),
    ARGUMENT_HAS_NO_TYPE(),

    // The case when there is no type mismatch, but parameter has uninferred types:
    // fun <T> foo(l: List<T>) {}; val l = foo(emptyList())
    MATCH_MODULO_UNINFERRED_TYPES(),

    UNKNOWN()
}
interface ArgumentMapping {
    fun isError(): Boolean
}
object ArgumentUnmapped : ArgumentMapping {
    override fun isError(): Boolean = true
}

interface ArgumentMatch :  ArgumentMapping {
    val valueParameter: ValueParameterDescriptor
    val status: ArgumentMatchStatus

    override fun isError(): Boolean = status.isError
}
class ArgumentMatchImpl(override val valueParameter: ValueParameterDescriptor) : ArgumentMatch {
    private var _status: ArgumentMatchStatus? = null

    override val status: ArgumentMatchStatus
        get() = _status ?: ArgumentMatchStatus.UNKNOWN

    fun recordMatchStatus(status: ArgumentMatchStatus) {
        _status = status
    }

    fun replaceValueParameter(newValueParameter: ValueParameterDescriptor): ArgumentMatchImpl {
        val newArgumentMatch = ArgumentMatchImpl(newValueParameter)
        newArgumentMatch._status = _status
        return newArgumentMatch
    }
}
