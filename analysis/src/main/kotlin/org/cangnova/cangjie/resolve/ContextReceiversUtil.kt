/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.diagnostics.infos.errors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS
import org.cangnova.cangjie.psi.CjContextReceiverList
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.NewCangJieTypeChecker
import org.cangnova.cangjie.types.isTypeParameter
import org.cangnova.cangjie.types.supertypes


fun checkSubtypingBetweenContextReceivers(
    trace: BindingTrace,
    contextReceiverList: CjContextReceiverList,
    contextReceiverTypes: List<CangJieType>
) {
    fun CangJieType.prepared(): CangJieType = when {
        isTypeParameter() -> supertypes().first()
//        containsTypeParameter() -> replaceArgumentsWithStarProjections()
        else -> this
    }
    for (i in 0 until contextReceiverTypes.lastIndex) {
        val contextReceiverType = contextReceiverTypes[i].prepared()
        for (j in (i + 1) until contextReceiverTypes.size) {
            val anotherContextReceiverType = contextReceiverTypes[j].prepared()
            if (NewCangJieTypeChecker.Default.isSubtypeOf(contextReceiverType, anotherContextReceiverType) ||
                NewCangJieTypeChecker.Default.isSubtypeOf(anotherContextReceiverType, contextReceiverType)
            ) {
                trace.report(SUBTYPING_BETWEEN_CONTEXT_RECEIVERS.on(contextReceiverList))
                return
            }
        }
    }
}

