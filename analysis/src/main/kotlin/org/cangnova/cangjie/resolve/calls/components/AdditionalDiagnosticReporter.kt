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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.expandIntersectionTypeIfNecessary


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
        val expectedType =
            parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeOptionAsSpecified(true) else it }

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
                val smartCastDiagnostic =
                    createSmartCastDiagnostic(candidate, argument, effectiveExpectedType) ?: continue

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
