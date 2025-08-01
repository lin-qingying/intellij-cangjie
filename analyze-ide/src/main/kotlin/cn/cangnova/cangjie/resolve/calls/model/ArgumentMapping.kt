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

package cn.cangnova.cangjie.resolve.calls.model

import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.types.ErrorUtils



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
