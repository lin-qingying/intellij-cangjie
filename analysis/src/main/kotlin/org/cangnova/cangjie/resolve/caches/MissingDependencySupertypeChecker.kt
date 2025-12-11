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

package org.cangnova.cangjie.resolve.caches

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.calls.checkers.CallChecker
import org.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import org.cangnova.cangjie.types.CangJieType
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.infos.errors.MISSING_DEPENDENCY_SUPERCLASS
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.fqNameSafe

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
                MISSING_DEPENDENCY_SUPERCLASS.on(
                    reportOn,
                    missingClassifier.fqNameSafe,
                    declaration.fqNameSafe
                )
            )
        }
        return missingSupertypes.isNotEmpty()
    }
}
