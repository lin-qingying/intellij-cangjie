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

package com.linqingying.cangjie.ide.parameterInfo

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CangJieIdeDescriptorOptions
import com.linqingying.cangjie.renderer.ClassifierNamePolicy
import com.linqingying.cangjie.renderer.render

class HintsDescriptorRendererOptions : CangJieIdeDescriptorOptions() {
    var hintsClassifierNamePolicy: HintsClassifierNamePolicy by property( SOURCE_CODE_QUALIFIED)
}
interface HintsClassifierNamePolicy {
    fun renderClassifier(classifier: ClassifierDescriptor, renderer: HintsTypeRenderer): String
}

/**
 * Almost copy-paste from [ClassifierNamePolicy.SOURCE_CODE_QUALIFIED]
 *
 * for local declarations qualified up to function scope
 */
object SOURCE_CODE_QUALIFIED : HintsClassifierNamePolicy {
    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: HintsTypeRenderer): String =
        qualifiedNameForSourceCode(classifier)

    private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor): String {
        val nameString = descriptor.name.render()
        if (descriptor is TypeParameterDescriptor) {
            return nameString
        }
        val qualifier = qualifierName(descriptor.containingDeclaration)
        return if (qualifier != null && qualifier != "") "$qualifier.$nameString" else nameString
    }

    private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
        is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
        is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
        else -> null
    }
}
