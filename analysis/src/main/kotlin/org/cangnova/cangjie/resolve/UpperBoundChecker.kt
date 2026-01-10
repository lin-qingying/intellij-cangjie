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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.diagnostics.DiagnosticFactory2
import org.cangnova.cangjie.diagnostics.DiagnosticFactory3
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.diagnostics.infos.errors.*

import org.cangnova.cangjie.diagnostics.reportDiagnosticOnce
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

@DefaultImplementation(impl = UpperBoundChecker::class)
open class UpperBoundChecker(
    private val typeChecker: CangJieTypeChecker,
) {
    //检查类型边界  （是不是父子关系）
    protected fun checkBound(
        bound: CangJieType,
        argumentType: CangJieType,
        argumentReference: CjTypeReference?,
        substitutor: DefaultTypeSubstitutor,
        typeAliasUsageElement: CjElement? = null,
        upperBoundViolatedReporter: UpperBoundViolatedReporter
    ): Boolean {
        // 仓颉语言的所有类型参数都是不变的(invariant)，直接进行类型替换
        val substitutedBound = substitutor.substitute(bound) ?: bound

        if (!typeChecker.isSubtypeOf(argumentType, substitutedBound)) {
            if (argumentReference != null) {
                upperBoundViolatedReporter.report(argumentReference, substitutedBound)
            } else if (typeAliasUsageElement != null && !substitutedBound.containsTypeAliasParameters() && !argumentType.containsTypeAliasParameters()) {
                upperBoundViolatedReporter.reportForTypeAliasExpansion(typeAliasUsageElement, substitutedBound)
            }
            return false
        }

        return true
    }

    fun checkBoundsInSupertype(
        typeReference: CjTypeReference,
        type: CangJieType,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        if (type.isError) return

        val typeElement = typeReference.typeElement ?: return
        val parameters = type.constructor.parameters
        val arguments = type.arguments

        assert(parameters.size == arguments.size)

        val cjTypeArguments = typeElement.typeArgumentsAsTypes

        if (type.isFlexible() && !type.isDynamic()) {
            assert(cjTypeArguments.size == 2) {
                ("Flexible type cannot be denoted in CangJie otherwise than as ft<T1, T2>, but was: "
                        + typeReference.getElementTextWithContext())
            }
            // it's really ft<Foo, Bar>
            val flexibleType = type.asFlexibleType()
            checkBoundsInSupertype(cjTypeArguments[0], flexibleType.lowerBound, trace, languageVersionSettings)
            checkBoundsInSupertype(cjTypeArguments[1], flexibleType.upperBound, trace, languageVersionSettings)
            return
        }

        if (type is AbbreviatedType) {
            checkBoundsForAbbreviatedSupertype(
                type, trace, typeReference,
                // The errors have been reported previously if cjTypeArguments.size accidentally was equal to the amount of arguments
                // in the expanded type
                reportWarning = cjTypeArguments.size != arguments.size
            )
            return
        }

        // If the numbers of type arguments do not match, the error has been already reported in TypeResolver
        if (cjTypeArguments.size != arguments.size) return

        val substitutor = DefaultTypeSubstitutor.create(type)

        for (i in cjTypeArguments.indices) {
            val cjTypeArgument = cjTypeArguments[i]
            checkBoundsInSupertype(cjTypeArgument, arguments[i].type, trace, languageVersionSettings)
            checkBounds(cjTypeArgument, arguments[i].type, parameters[i], substitutor, trace)
        }
    }

    private fun checkBoundsForAbbreviatedSupertype(
        type: CangJieType,
        trace: BindingTrace,
        typeReference: CjTypeReference,
        reportWarning: Boolean
    ) {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        val substitutor = DefaultTypeSubstitutor.create(type)

        val diagnostic =
            if (reportWarning)
                UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING
            else
                UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION

        for (i in arguments.indices) {

            val argumentType = arguments[i].type

            checkBoundsForAbbreviatedSupertype(argumentType, trace, typeReference, reportWarning)

            checkBounds(
                argumentReference = null,
                argumentType, parameters[i], substitutor, trace,
                typeAliasUsageElement = typeReference, diagnosticForTypeAliases = diagnostic,
            )
        }
    }

    open fun checkBounds(
        argumentReference: CjTypeReference?,
        argumentType: CangJieType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: DefaultTypeSubstitutor,
        trace: BindingTrace,
        typeAliasUsageElement: CjElement? = null,
        diagnosticForTypeAliases: DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
    ) {
        if (typeParameterDescriptor.upperBounds.isEmpty()) return

        val diagnosticsReporter =
            UpperBoundViolatedReporter(
                trace,
                argumentType,
                typeParameterDescriptor,
                diagnosticForTypeAliases = diagnosticForTypeAliases
            )

        for (bound in typeParameterDescriptor.upperBounds) {
            checkBound(bound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporter)
        }
    }

}

class UpperBoundViolatedReporter(
    private val trace: BindingTrace,
    private val argumentType: CangJieType,
    private val typeParameterDescriptor: TypeParameterDescriptor,
    private val baseDiagnostic: DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> = UPPER_BOUND_VIOLATED,
    private val diagnosticForTypeAliases: DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
) {
    fun report(typeArgumentReference: CjTypeReference, substitutedBound: CangJieType) {
        trace.reportDiagnosticOnce(baseDiagnostic.on(typeArgumentReference, substitutedBound, argumentType))
    }

    fun reportForTypeAliasExpansion(callElement: CjElement, substitutedBound: CangJieType) {
        trace.reportDiagnosticOnce(
            diagnosticForTypeAliases.on(
                callElement,
                substitutedBound,
                argumentType,
                typeParameterDescriptor
            )
        )
    }
}
