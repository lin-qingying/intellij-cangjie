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

package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.types.CangJieType
import com.intellij.psi.PsiElement


@DefaultImplementation(RttiExpressionChecker.Default::class)
interface RttiExpressionChecker {
    fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace)

    object Default : RttiExpressionChecker {
        override fun check(rttiInformation: RttiExpressionInformation, reportOn: PsiElement, trace: BindingTrace) {

        }
    }
}

enum class RttiOperation {
    IS,

    AS

}

class RttiExpressionInformation(
    val subject: CjElement,
    val sourceType: CangJieType?,
    val targetType: CangJieType?,
    val operation: RttiOperation
)
