package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.Errors.UPPER_BOUND_VIOLATED
import com.huawei.cangjie.descriptors.Errors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.diagnostics.DiagnosticFactory2
import com.huawei.cangjie.diagnostics.DiagnosticFactory3
import com.huawei.cangjie.diagnostics.reportDiagnosticOnce
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjTypeReference
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.Variance
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
