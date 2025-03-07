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

package cn.cangnova.cangjie.ide.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.ConstructorDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.PackageViewDescriptor
import cn.cangnova.cangjie.ide.codeinsight.hints.InlayInfoDetails
import cn.cangnova.cangjie.ide.codeinsight.hints.InlayInfoOption
import cn.cangnova.cangjie.ide.codeinsight.hints.TextInlayInfoDetail
import cn.cangnova.cangjie.ide.formatter.cangjieCustomSettings
import cn.cangnova.cangjie.ide.intentions.SpecifyTypeExplicitlyIntention
import cn.cangnova.cangjie.name.SpecialNames
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.endOffset
import cn.cangnova.cangjie.psi.psiUtil.getLineNumber
import cn.cangnova.cangjie.psi.psiUtil.isMultiLine
import cn.cangnova.cangjie.psi.stubs.elements.getAllBindings
import cn.cangnova.cangjie.references.resolveMainReferenceToDescriptors
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.caches.resolveToCall
import cn.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.resolve.sam.SamConstructorDescriptor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.util.containsError
import cn.cangnova.cangjie.types.util.immediateSupertypes
import cn.cangnova.cangjie.types.util.isEnum
import cn.cangnova.cangjie.types.util.isUnit


fun provideVariableTypeHint(elem: PsiElement, inlayInfoOption: InlayInfoOption): List<InlayInfoDetails> {
    (elem as? CjVariable)?.let { variable ->
        variable.nameIdentifier?.let { ident ->
            provideTypeHint(variable, ident.endOffset, inlayInfoOption)?.let { return listOf(it) }
        }

        variable.pattern.getAllBindings().mapNotNull {
            provideTypeHint(it, it.endOffset, inlayInfoOption)
        }.let {
            if (it.isNotEmpty()) return it
        }
    }
    return when (elem) {
        is CjVariable -> {
            elem.nameIdentifier?.let { ident ->
                provideTypeHint(elem, ident.endOffset, inlayInfoOption)?.let { listOf(it) }
            } ?: elem.pattern.getAllBindings().mapNotNull {
                provideTypeHint(it, it.endOffset, inlayInfoOption)
            }.let {
                if (it.isNotEmpty()) it else emptyList()
            } ?: emptyList()

        }

        is CjBindingPattern -> {
            provideTypeHint(elem, elem.endOffset, inlayInfoOption)?.let { listOf(it) } ?: emptyList()

        }

        else -> emptyList()
    }

}

/**
 * 提供类型提示信息
 *
 * 此函数负责分析给定的代码元素，以生成适当的类型提示信息它首先尝试获取元素的类型信息，
 * 如果类型中包含错误，则不提供提示如果类型是特殊的或匿名的，则尝试获取其直接超类型的类型
 * 对于局部变量，如果是单元类型且多行定义，则根据代码格式决定是否提供提示最后，如果类型不明确，
 * 则根据设置渲染类型信息到提示详情对象中
 *
 * @param element 代码元素，用于类型分析和提示信息的源
 * @param offset 提示信息插入的偏移量
 * @param inlayInfoOption 提示信息的选项，用于定制提示的行为
 * @return 如果成功生成类型提示信息，则返回提示详情对象，否则返回null
 */
fun provideTypeHint(element: CjCallableDeclaration, offset: Int, inlayInfoOption: InlayInfoOption): InlayInfoDetails? {
    // 尝试获取代码元素的类型信息
    var type: CangJieType = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element).unwrap()
    // 如果类型信息中包含错误，则不提供提示
    if (type.containsError()) return null
    // 获取类型的声明描述符，用于进一步的类型分析
    val declarationDescriptor = type.constructor.declarationDescriptor
    val name = declarationDescriptor?.name
    // 如果类型名称是默认的无名称提供，则尝试获取其直接超类型
    if (name == SpecialNames.NO_NAME_PROVIDED) {
        if (element is CjVariable && element.isLocal) {
            // 对于局部变量，匿名对象类型不会被合并到其超类型中，
            // 所以显示超类型会误导
            return null
        }
        type = type.immediateSupertypes().singleOrNull() ?: return null
    } else if (name?.isSpecial == true) {
        // 如果类型名称是特殊名称，则不提供提示
        return null
    }

    // 对于局部变量，如果是单元类型且多行定义，则根据代码格式决定是否提供提示
    if (element is CjVariable && element.isLocal && type.isUnit() && element.isMultiLine()) {
        val propertyLine = element.getLineNumber()
        val equalsTokenLine = element.equalsToken?.getLineNumber() ?: -1
        val initializerLine = element.initializer?.getLineNumber() ?: -1
        if (propertyLine == equalsTokenLine && propertyLine != initializerLine) {
            val indentBeforeProperty = (element.prevSibling as? PsiWhiteSpace)?.text?.substringAfterLast('\n')
            val indentBeforeInitializer =
                (element.initializer?.prevSibling as? PsiWhiteSpace)?.text?.substringAfterLast('\n')
            if (indentBeforeProperty == indentBeforeInitializer) {
                // 如果属性和初始化器的缩进相同，则不提供提示
                return null
            }
        }
    }

    // 根据类型是否清晰来决定是否提供提示
    return if (isUnclearType(type, element)) {
        // 获取元素所属文件的自定义设置
        val settings = element.containingCjFile.cangjieCustomSettings
        // 渲染类型信息到提示内容
        val renderedType = HintsTypeRenderer.getInlayHintsTypeRenderer(element.safeAnalyzeNonSourceRootCode(), element)
            .renderTypeIntoInlayInfo(type)
        // 构建类型提示前缀，根据设置决定是否在类型冒号前后添加空格
        val prefix = buildString {
            if (settings.SPACE_BEFORE_TYPE_COLON) {
                append(" ")
            }

            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON) {
                append(" ")
            }
        }

        // 创建基础的提示信息对象
        val inlayInfo = InlayInfo(
            text = "", offset = offset,
            isShowOnlyIfExistedBefore = false, isFilterByBlacklist = true, relatesToPrecedingText = true
        )
        // 返回包含提示详情的对象
        return InlayInfoDetails(inlayInfo, listOf(TextInlayInfoDetail(prefix)) + renderedType, inlayInfoOption)
    } else {
        // 如果类型清晰，则不提供提示
        null
    }
}


private fun isUnclearType(type: CangJieType, element: CjCallableDeclaration): Boolean {
    if (element !is CjProperty) return true

    val initializer = element.initializer ?: return true
    if (initializer is CjConstantExpression || initializer is CjStringTemplateExpression) return false
    if (initializer is CjUnaryExpression && initializer.baseExpression is CjConstantExpression) return false

    if (isConstructorCall(initializer)) {
        return false
    }

    if (initializer is CjDotQualifiedExpression) {
        val selectorExpression = initializer.selectorExpression
        if (type.isEnum()) {
            // Do not show type for enums if initializer has enum entry with explicit enum name: val p = Enum.ENTRY
            val enumEntryDescriptor: DeclarationDescriptor? =
                selectorExpression?.resolveMainReferenceToDescriptors()?.singleOrNull()

            if (enumEntryDescriptor != null && DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                return false
            }
        }

        if (initializer.receiverExpression.isClassOrPackageReference() && isConstructorCall(selectorExpression)) {
            return false
        }
    }

    return true
}

private fun isConstructorCall(initializer: CjExpression?): Boolean {
    if (initializer is CjCallExpression) {
        val resolvedCall = initializer.resolveToCall(BodyResolveMode.FULL)
        val resolvedDescriptor = resolvedCall?.candidateDescriptor
        if (resolvedDescriptor is SamConstructorDescriptor) {
            return true
        }
        if (resolvedDescriptor is ConstructorDescriptor &&
            (resolvedDescriptor.constructedClass.declaredTypeParameters.isEmpty() || initializer.typeArgumentList != null)
        ) {
            return true
        }
        return false
    }

    return false
}

private fun CjExpression.isClassOrPackageReference(): Boolean =
    when (this) {
        is CjNameReferenceExpression -> this.resolveMainReferenceToDescriptors().singleOrNull()
            .let { it is ClassDescriptor || it is PackageViewDescriptor }

        is CjDotQualifiedExpression -> this.selectorExpression?.isClassOrPackageReference() ?: false
        else -> false
    }
