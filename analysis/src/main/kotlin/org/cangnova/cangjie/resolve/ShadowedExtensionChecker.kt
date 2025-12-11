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
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.impl.VariableDescriptorImpl
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.diagnostics.infos.errors.EXTENSION_SHADOWED_BY_MEMBER
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.isExtension


class ShadowedExtensionChecker(val typeSpecificityComparator: TypeSpecificityComparator, val trace: DiagnosticSink) {
    private fun isExtensionFunctionShadowedByMemberFunction(
        extension: FunctionDescriptor,
        member: FunctionDescriptor
    ): Boolean {
        // Permissive check:
        //      (1) functions should have same number of arguments;
        //      (2) varargs should be in the same positions;
        //      (3) extension signature should be not less specific than member signature.
        // (1) & (2) are required so that we can match signatures easily.

        if (extension.valueParameters.size != member.valueParameters.size) return false
//        if (extension.varargParameterPosition() != member.varargParameterPosition()) return false
        if (extension.isOperator && !member.isOperator) return false


        val extensionSignature = FlatSignature.createForPossiblyShadowedExtension(extension)
        val memberSignature = FlatSignature.createFromCallableDescriptor(member)
        return isSignatureNotLessSpecific(extensionSignature, memberSignature)
    }

    fun checkDeclaration(declaration: CjDeclaration, descriptor: DeclarationDescriptor) {
        if (declaration.name == null) return
        if (descriptor !is CallableMemberDescriptor) return
//        if (descriptor.hasHidesMembersAnnotation()) return
        val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return
        if (extensionReceiverType.isError) return
        if (extensionReceiverType.isOption) return

        when (descriptor) {
            is FunctionDescriptor ->
                checkShadowedExtensionFunction(declaration, descriptor, trace)

            is PropertyDescriptor ->
                checkShadowedExtensionProperty(declaration, descriptor, trace)

            is VariableDescriptorImpl -> checkShadowedExtensionVariable(declaration, descriptor, trace)
        }
    }

    private fun checkShadowedExtensionVariable(
        declaration: CjDeclaration,
        extensionProperty: VariableDescriptorImpl,
        trace: DiagnosticSink
    ) {
        val memberScope = extensionProperty.extensionReceiverParameter?.type?.memberScope ?: return

        memberScope.getContributedVariables(extensionProperty.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            .firstOrNull { !it.isExtension }
            ?.let { memberProperty ->
                (memberProperty as? VariableDescriptorImpl)?.let {
                    trace.report(
                        EXTENSION_SHADOWED_BY_MEMBER.on(
                            declaration,
                            it
                        )
                    )
                }

            }
    }

    private fun checkShadowedExtensionProperty(
        declaration: CjDeclaration,
        extensionProperty: PropertyDescriptor,
        trace: DiagnosticSink
    ) {
        val memberScope = extensionProperty.extensionReceiverParameter?.type?.memberScope ?: return

        memberScope.getContributedPropertys(extensionProperty.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            .firstOrNull { !it.isExtension }
            ?.let { memberProperty ->
                trace.report(EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberProperty))
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
        val memberScope = extensionFunction.extensionReceiverParameter?.type?.memberScope ?: return

        val contributedFunctions =
            memberScope.getContributedFunctions(
                extensionFunction.name,
                NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
            )
        for (memberFunction in contributedFunctions) {
            if (isExtensionFunctionShadowedByMemberFunction(extensionFunction, memberFunction)) {
                trace.report(EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberFunction))
                return
            }
        }

//        val nestedClass = memberScope.getContributedClassifier(extensionFunction.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
//
//
//        val contributedVariables =
//            memberScope.getContributedVariables(extensionFunction.name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
//        for (memberProperty in contributedVariables) {
//            if (!memberProperty.isPublic()) continue
//
//            val invokeOperator = getInvokeOperatorShadowingExtensionFunction(extensionFunction, memberProperty)
//            if (invokeOperator != null) {
//                trace.report(
//                    Errors.EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE.on(
//                        declaration,
//                        memberProperty,
//                        invokeOperator
//                    )
//                )
//                return
//            }
//        }
    }

}
