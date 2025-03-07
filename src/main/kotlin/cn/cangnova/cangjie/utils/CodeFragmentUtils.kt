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

package cn.cangnova.cangjie.utils

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.psi.CjCodeFragment
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty

object CodeFragmentUtils {
    val RUNTIME_TYPE_EVALUATOR: Key<Function1<CjExpression, CangJieType?>> = Key.create("RUNTIME_TYPE_EVALUATOR")

    // Identifier that the codeFragment is used in the debugger evaluator for compilation. See [PerFileAnalysisCache.getAnalysisResults]
    val USED_FOR_COMPILATION_IN_IR_EVALUATOR: Key<Boolean> = Key.create("USED_FOR_COMPILATION_IN_EVALUATOR")
}

var CjCodeFragment.externalDescriptors: List<DeclarationDescriptor>? by CopyablePsiUserDataProperty(Key.create("EXTERNAL_DESCRIPTORS"))

class CopyablePsiUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key)

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putCopyableUserData(key, value)
}
