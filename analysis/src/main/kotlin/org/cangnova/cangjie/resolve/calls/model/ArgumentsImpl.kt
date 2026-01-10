/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo


class FakeCangJieCallArgumentForCallableReference(
    val index: Int,
    val name: Name?
) : CangJieCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = name
}

class ReceiverExpressionCangJieCallArgument private constructor(
    override val receiver: ReceiverValueWithSmartCastInfo,
    override val isSafeCall: Boolean = false,
    val isForImplicitInvoke: Boolean = false
) : ExpressionCangJieCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = null
    override fun toString() = "$receiver" + if (isSafeCall) "?" else ""

    companion object {
        operator fun invoke(
            receiver: ReceiverValueWithSmartCastInfo,
            isSafeCall: Boolean = false,
            isForImplicitInvoke: Boolean = false
        ) = ReceiverExpressionCangJieCallArgument(
            receiver,
            isSafeCall,
            isForImplicitInvoke
        )
    }
}
