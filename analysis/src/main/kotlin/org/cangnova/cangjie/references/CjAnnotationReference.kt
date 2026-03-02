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

package org.cangnova.cangjie.references

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 注解引用解析器
 *
 * 处理非内置注解 (@CustomName) 的引用解析。
 * 优先从 ANNOTATION slice 获取注解描述符，回退通过 typeReference 的 REFERENCE_TARGET 查找。
 */
class CjAnnotationReference(annotation: CjAnnotation) :
    AbstractCjReference<CjAnnotation>(annotation), CjReference {

    override val resolvesByNames: Collection<Name>
        get() = listOfNotNull(expression.shortName)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        // 内置注解不走此引用（由内置逻辑处理）
        if (expression.isBuiltInAnnotation) return emptyList()

        // 1. 尝试从 ANNOTATION slice 获取注解描述符
        val annotationDescriptor = context[BindingContext.ANNOTATION, expression]
        if (annotationDescriptor != null) {
            val classDescriptor = annotationDescriptor.type.constructor.declarationDescriptor
            if (classDescriptor != null) {
                return listOf(classDescriptor)
            }
        }

        // 2. 尝试通过 typeReference 内部的 REFERENCE_TARGET 查找
        val typeReference = expression.typeReference
        val typeElement = typeReference?.typeElement as? CjUserType
        val refExpr = typeElement?.referenceExpression
        if (refExpr != null) {
            val target = context[BindingContext.REFERENCE_TARGET, refExpr]
            if (target != null) {
                return listOf(target)
            }
        }

        return emptyList()
    }

    override fun getRangeInElement(): TextRange {
        val typeElement = expression.typeReference?.typeElement as? CjUserType
        val nameElement = typeElement?.referenceExpression?.referencedNameElement
            ?: return TextRange.EMPTY_RANGE
        val startOffset = element.startOffset
        return nameElement.textRange.shiftRight(-startOffset)
    }

    override fun canRename(): Boolean = true
}
