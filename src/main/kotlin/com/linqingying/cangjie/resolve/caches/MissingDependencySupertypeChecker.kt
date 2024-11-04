package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjTypeParameterListOwner
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.MissingSupertypesResolver
import com.linqingying.cangjie.resolve.calls.checkers.CallChecker
import com.linqingying.cangjie.resolve.calls.checkers.CallCheckerContext
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.linqingying.cangjie.resolve.descriptorUtil.fqNameSafe
import com.linqingying.cangjie.types.CangJieType
import com.intellij.psi.PsiElement

object MissingDependencySupertypeChecker {
    object ForDeclarations : DeclarationChecker {
        override fun check(
            declaration: CjDeclaration,
            descriptor: DeclarationDescriptor,
            context: DeclarationCheckerContext
        ) {
            val trace = context.trace

            if (descriptor is ClassDescriptor) {
                checkSupertypes(descriptor, declaration, trace, context.missingSupertypesResolver)
            }

            if (declaration is CjTypeParameterListOwner) {
                for (cjTypeParameter in declaration.typeParameters) {
                    val typeParameterDescriptor =
                        trace.bindingContext.get(BindingContext.TYPE_PARAMETER, cjTypeParameter) ?: continue
                    for (upperBound in typeParameterDescriptor.upperBounds) {
                        checkSupertypes(upperBound, cjTypeParameter, trace, context.missingSupertypesResolver)
                    }
                }
            }
        }
    }

    object ForCalls : CallChecker {
        override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
            val descriptor = resolvedCall.resultingDescriptor

            val errorReported = checkSupertypes(
                descriptor.dispatchReceiverParameter?.declaration, reportOn,
                context.trace, context.missingSupertypesResolver
            )

//            val eagerChecksAllowed = context.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)
            val unresolvedLazySupertypesByDefault =
                descriptor is ConstructorDescriptor || descriptor is FakeCallableDescriptorForObject

            if (/*eagerChecksAllowed ||*/ !unresolvedLazySupertypesByDefault && !errorReported) {
                // The constructed class' own supertypes are not resolved after constructor call,
                // so its containing declaration should not be checked.
                // Dispatch receiver is checked before for case of inner class constructor call.
                checkSupertypes(
                    descriptor.containingDeclaration,
                    reportOn,
                    context.trace,
                    context.missingSupertypesResolver
                )
                checkSupertypes(
                    descriptor.extensionReceiverParameter?.declaration, reportOn,
                    context.trace, context.missingSupertypesResolver
                )
            }
        }

        private val ReceiverParameterDescriptor.declaration
            get() = value.type.constructor.declarationDescriptor
    }

    // true for reported error
    fun checkSupertypes(
        classifierType: CangJieType,
        reportOn: PsiElement,
        trace: BindingTrace,
        missingSupertypesResolver: MissingSupertypesResolver
    ) = checkSupertypes(classifierType.constructor.declarationDescriptor, reportOn, trace, missingSupertypesResolver)

    // true for reported error
    fun checkSupertypes(
        declaration: DeclarationDescriptor?,
        reportOn: PsiElement,
        trace: BindingTrace,
        missingSupertypesResolver: MissingSupertypesResolver
    ): Boolean {
        if (declaration !is ClassifierDescriptor)
            return false

        val missingSupertypes = missingSupertypesResolver.getMissingSuperClassifiers(declaration)
        for (missingClassifier in missingSupertypes) {
            trace.report(
                Errors.MISSING_DEPENDENCY_SUPERCLASS.on(
                    reportOn,
                    missingClassifier.fqNameSafe,
                    declaration.fqNameSafe
                )
            )
        }
        return missingSupertypes.isNotEmpty()
    }
}
