package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.util.expandIntersectionTypeIfNecessary


// very initial state of component
// todo: handle all diagnostic inside DiagnosticReporterByTrackingStrategy
// move it to frontend module
class AdditionalDiagnosticReporter(
    private val languageVersionSettings: LanguageVersionSettings
) {

    fun reportAdditionalDiagnostics(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        cangjieDiagnosticsHolder: CangJieDiagnosticsHolder,
        diagnostics: Collection<CangJieCallDiagnostic>
    ) {
        reportSmartCasts(candidate, resultingDescriptor, cangjieDiagnosticsHolder, diagnostics)
    }

    private fun createSmartCastDiagnostic(
        candidate: ResolvedCallAtom,
        argument: CangJieCallArgument,
        expectedResultType: UnwrappedType
    ): SmartCastDiagnostic? {
        if (argument !is ExpressionCangJieCallArgument) return null

        val types = expectedResultType.expandIntersectionTypeIfNecessary()

        val argumentType = argument.receiver.receiverValue.type
        val isSubtype = types.map { CangJieTypeChecker.DEFAULT.isSubtypeOf(argumentType, it) }
        if (isSubtype.any { it }) return null

        return SmartCastDiagnostic(argument, types.first().unwrap(), candidate.atom)
    }

    private fun reportSmartCastOnReceiver(
        candidate: ResolvedCallAtom,
        receiver: SimpleCangJieCallArgument?,
        parameter: ReceiverParameterDescriptor?,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): SmartCastDiagnostic? {
        if (receiver == null || parameter == null) return null
        val expectedType = parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeNullableAsSpecified(true) else it }

        val smartCastDiagnostic = createSmartCastDiagnostic(candidate, receiver, expectedType) ?: return null

        // todo may be we have smart cast to Int?
        return smartCastDiagnostic.takeIf {
            diagnostics.filterIsInstance<UnsafeCallError>().none {
                it.receiver == receiver
            }
                    &&
                    diagnostics.filterIsInstance<UnstableSmartCast>().none {
                        it.argument == receiver
                    }
        }
    }

    private fun reportSmartCasts(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        cangjieDiagnosticsHolder: CangJieDiagnosticsHolder,
        diagnostics: Collection<CangJieCallDiagnostic>
    ) {
        cangjieDiagnosticsHolder.addDiagnosticIfNotNull(
            reportSmartCastOnReceiver(
                candidate,
                candidate.extensionReceiverArgument,
                resultingDescriptor.extensionReceiverParameter,
                diagnostics
            )
        )
        cangjieDiagnosticsHolder.addDiagnosticIfNotNull(
            reportSmartCastOnReceiver(
                candidate,
                candidate.dispatchReceiverArgument,
                resultingDescriptor.dispatchReceiverParameter,
                diagnostics
            )
        )

        for (parameter in resultingDescriptor.valueParameters) {
            for (argument in candidate.argumentMappingByOriginal[parameter.original]?.arguments ?: continue) {
                val effectiveExpectedType = argument.getExpectedType(parameter, languageVersionSettings)
                val smartCastDiagnostic = createSmartCastDiagnostic(candidate, argument, effectiveExpectedType) ?: continue

                val thereIsUnstableSmartCastError = diagnostics.filterIsInstance<UnstableSmartCast>().any {
                    it.argument == argument
                }

                if (!thereIsUnstableSmartCastError) {
                    cangjieDiagnosticsHolder.addDiagnostic(smartCastDiagnostic)
                }
            }
        }
    }
}
