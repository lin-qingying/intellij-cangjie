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

import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.diagnostics.infos.errors.EXTEND_MEMBER_CANNOT_SHADOW
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.isExtension

/**
 * 检查 extend 成员是否遮蔽原始类型的成员
 *
 * 在仓颉语言中：
 * - extend 成员不允许遮蔽原始类型的同名成员
 * - extend 成员使用 dispatchReceiver（类似普通成员）
 * - 当 extend 成员遮蔽原始成员时，应该报告错误
 */
class ShadowedExtensionChecker(val typeSpecificityComparator: TypeSpecificityComparator, val trace: DiagnosticSink) {

    /**
     * 检查扩展函数是否被成员函数遮蔽
     *
     * 宽松检查：
     * (1) 函数应该有相同数量的参数
     * (2) 扩展签名不应该比成员签名更不具体
     */
    private fun isExtensionFunctionShadowedByMemberFunction(
        extension: FunctionDescriptor,
        member: FunctionDescriptor
    ): Boolean {
        if (extension.valueParameters.size != member.valueParameters.size) return false
        if (extension.isOperator && !member.isOperator) return false

        val extensionSignature = FlatSignature.createForPossiblyShadowedExtension(extension)
        val memberSignature = FlatSignature.createFromCallableDescriptor(member)
        return isSignatureNotLessSpecific(extensionSignature, memberSignature)
    }

    fun checkDeclaration(declaration: CjDeclaration, descriptor: DeclarationDescriptor) {
        if (declaration.name == null) return
        if (descriptor !is CallableMemberDescriptor) return

        // 只检查在 extend 中声明的成员
        if (!descriptor.isExtension) return

        // 获取 extend 扩展的原始类型
        val extendDescriptor = descriptor.containingDeclaration as? ExtendDescriptor ?: return
        val extendedType = extendDescriptor.extendType
        if (extendedType.isError) return

        when (descriptor) {
            is FunctionDescriptor ->
                checkShadowedExtensionFunction(declaration, descriptor, trace)

            is PropertyDescriptor ->
                checkShadowedExtensionProperty(declaration, descriptor, trace)

            is VariableDescriptor ->
                checkShadowedExtensionVariable(declaration, descriptor, trace)
        }
    }

    private fun checkShadowedExtensionVariable(
        declaration: CjDeclaration,
        extensionVariable: VariableDescriptor,
        trace: DiagnosticSink
    ) {
        // 获取 extend 扩展的原始类型的成员作用域
        val extendDescriptor = extensionVariable.containingDeclaration as? ExtendDescriptor ?: return
        val extendedType = extendDescriptor.extendType
        val memberScope = extendedType.memberScope

        memberScope.getContributedVariables(extensionVariable.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            .firstOrNull { !it.isExtension }
            ?.let {
                val extendedClass = extendedType.constructor.declarationDescriptor as? ClassifierDescriptor ?: return
                trace.report(
                    EXTEND_MEMBER_CANNOT_SHADOW.on(
                        declaration,
                        extensionVariable.name.asString(),
                        extendedClass
                    )
                )
            }
    }

    private fun checkShadowedExtensionProperty(
        declaration: CjDeclaration,
        extensionProperty: PropertyDescriptor,
        trace: DiagnosticSink
    ) {
        // 获取 extend 扩展的原始类型的成员作用域
        val extendDescriptor = extensionProperty.containingDeclaration as? ExtendDescriptor ?: return
        val extendedType = extendDescriptor.extendType
        val memberScope = extendedType.memberScope

        memberScope.getContributedPropertys(extensionProperty.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            .firstOrNull { !it.isExtension }
            ?.let {
                val extendedClass = extendedType.constructor.declarationDescriptor as? ClassifierDescriptor ?: return
                trace.report(
                    EXTEND_MEMBER_CANNOT_SHADOW.on(
                        declaration,
                        extensionProperty.name.asString(),
                        extendedClass
                    )
                )
            }
    }

    private fun isSignatureNotLessSpecific(
        extensionSignature: FlatSignature<FunctionDescriptor>,
        memberSignature: FlatSignature<FunctionDescriptor>
    ): Boolean =
        ConstraintSystemBuilderImpl.forSpecificity().isSignatureNotLessSpecific(
            extensionSignature,
            memberSignature,
            OverloadabilitySpecificityCallbacks,
            typeSpecificityComparator
        )

    private fun checkShadowedExtensionFunction(
        declaration: CjDeclaration,
        extensionFunction: FunctionDescriptor,
        trace: DiagnosticSink
    ) {
        // 获取 extend 扩展的原始类型的成员作用域
        val extendDescriptor = extensionFunction.containingDeclaration as? ExtendDescriptor ?: return
        val extendedType = extendDescriptor.extendType
        val memberScope = extendedType.memberScope

        val contributedFunctions =
            memberScope.getContributedFunctions(
                extensionFunction.name,
                NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
            )
        for (memberFunction in contributedFunctions) {
            // 跳过同样是 extend 成员的函数
            if (memberFunction.isExtension) continue

            if (isExtensionFunctionShadowedByMemberFunction(extensionFunction, memberFunction)) {
                val extendedClass = extendedType.constructor.declarationDescriptor as? ClassifierDescriptor ?: return
                trace.report(
                    EXTEND_MEMBER_CANNOT_SHADOW.on(
                        declaration,
                        extensionFunction.name.asString(),
                        extendedClass
                    )
                )
                return
            }
        }
    }
}
