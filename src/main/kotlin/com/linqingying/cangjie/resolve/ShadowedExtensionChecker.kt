package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.PropertyDescriptor
import com.linqingying.cangjie.descriptors.impl.VariableDescriptorImpl
import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import com.linqingying.cangjie.resolve.calls.results.*
import com.linqingying.cangjie.types.isError


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
        if (extensionReceiverType.isMarkedOption) return

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
                        Errors.EXTENSION_SHADOWED_BY_MEMBER.on(
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
                trace.report(Errors.EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberProperty))
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
                trace.report(Errors.EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberFunction))
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
