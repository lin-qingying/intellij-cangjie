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

package cn.cangnova.cangjie.ide.searching.usages

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.ide.IdeDescriptorRenderers
import cn.cangnova.cangjie.psi.CjFunction
import cn.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.psi.PsiElement




class CangJieFindUsagesProvider : CangJieFindUsagesProviderBase() {

    override fun getDescriptiveName(element: PsiElement): String {

        if (element !is CjFunction) return super.getDescriptiveName(element)

        val name = element.name ?: ""
        val descriptor = element.unsafeResolveToDescriptor() as FunctionDescriptor
        val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS
        val paramsDescription =
            descriptor.valueParameters.joinToString(prefix = "(", postfix = ")") { renderer.renderType(it.type) }
        val returnType = descriptor.returnType
        val returnTypeDescription = if (returnType != null && !returnType.isUnit()) renderer.renderType(returnType) else null
        val funDescription = "$name$paramsDescription" + (returnTypeDescription?.let { ": $it" } ?: "")
        return element.containerDescription?.let { CangJieBundle.message("find.usage.provider.0.of.1", funDescription, it) }
            ?: CangJieBundle.message("find.usage.provider.0", funDescription)
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
