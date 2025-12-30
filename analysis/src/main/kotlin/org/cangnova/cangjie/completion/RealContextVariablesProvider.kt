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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.FuzzyType
import org.cangnova.cangjie.types.TypeSubstitutor
import org.cangnova.cangjie.types.fuzzyReturnType
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.cangnova.cangjie.codeinsight.ReferenceVariantsHelper
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.isBuiltinFunctionalTypeOrSubtype
import java.util.HashSet



class RealContextVariablesProvider(
    private val referenceVariantsHelper: ReferenceVariantsHelper,
    private val contextElement: PsiElement
) : ContextVariablesProvider {

    val allFunctionTypeVariables by lazy {
        collectVariables().filter { it.type.isBuiltinFunctionalTypeOrSubtype }
    }

    /*
    * The reason for using `nameFilter = MemberScope.ALL_NAME_FILTER` here is that we have
    * functionality that allows to complete arguments for a completing call like here:
    * class C {
    *   companion object {
    *     fun create(p: (Int) -> Unit) {}
    *   }
    * }
    *
    * val handler: (Int) -> Unit = {}
    *
    * val v: C = cr<caret>
    *
    * And here at <caret> it's possible to complete the full line: C.create(handler) !!!
    * */
    private fun collectVariables(): Collection<VariableDescriptor> {
        val descriptorFilter =
            DescriptorKindFilter.VARIABLES exclude DescriptorKindExclude.Extensions // we exclude extensions by performance reasons
        return referenceVariantsHelper.getReferenceVariants(
            contextElement,
            CallTypeAndReceiver.DEFAULT,
            descriptorFilter,
            nameFilter = MemberScope.ALL_NAME_FILTER).map { it as VariableDescriptor }
    }

    override fun functionTypeVariables(requiredType: FuzzyType): Collection<Pair<VariableDescriptor, TypeSubstitutor>> {
        val result = SmartList<Pair<VariableDescriptor, TypeSubstitutor>>()
        for (variable in allFunctionTypeVariables) {
            val substitutor = variable.fuzzyReturnType()?.checkIsSubtypeOf(requiredType) ?: continue
            result.add(variable to substitutor)
        }
        return result
    }
}

