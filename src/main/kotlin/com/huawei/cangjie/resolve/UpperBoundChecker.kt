package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.Errors.*
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.diagnostics.DiagnosticFactory2
import com.huawei.cangjie.diagnostics.DiagnosticFactory3
import com.huawei.cangjie.diagnostics.reportDiagnosticOnce
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjTypeReference
import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.util.containsTypeAliasParameters

@DefaultImplementation(impl = UpperBoundChecker::class)
open class UpperBoundChecker(
    private val typeChecker: CangJieTypeChecker,
) {
    //检查类型边界  （是不是父子关系）
    protected fun checkBound(
        bound: CangJieType,
        argumentType: CangJieType,
        argumentReference: CjTypeReference?,
        substitutor: TypeSubstitutor,
        typeAliasUsageElement: CjElement? = null,
        upperBoundViolatedReporter: UpperBoundViolatedReporter
    ): Boolean {
        val substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT)

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

        // A type reference from Kotlin code can yield a flexible type only if it's `ft<T1, T2>`, whose bounds should not be checked
        if (type.isFlexible() && !type.isDynamic()) {
            assert(cjTypeArguments.size == 2) {
                ("Flexible type cannot be denoted in Kotlin otherwise than as ft<T1, T2>, but was: "
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
                reportWarning = cjTypeArguments.size != arguments.size &&
                        !languageVersionSettings.supportsFeature(
                            LanguageFeature.ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes
                        )
            )
            return
        }

        // If the numbers of type arguments do not match, the error has been already reported in TypeResolver
        if (cjTypeArguments.size != arguments.size) return

        val substitutor = TypeSubstitutor.create(type)

        for (i in cjTypeArguments.indices) {
            val cjTypeArgument = cjTypeArguments[i] ?: continue
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
        val substitutor = TypeSubstitutor.create(type)

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
        substitutor: TypeSubstitutor,
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
