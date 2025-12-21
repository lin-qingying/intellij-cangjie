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

import org.cangnova.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.NotNullablePsiCopyableUserDataProperty
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.lazy.ResolveSession

fun analyzeControlFlow(resolveSession: ResolveSession, resolveElement: CjElement, trace: BindingTrace) {
    val controlFlowTrace = DelegatingBindingTrace(
        trace.bindingContext, "Element control flow resolveName", resolveElement, allowSliceRewrite = true
    )
    ControlFlowInformationProviderImpl(
        resolveElement,
        controlFlowTrace,
        resolveElement.languageVersionSettings,/* resolveSession.platformDiagnosticSuppressor*/
    ).checkDeclaration()
    controlFlowTrace.addOwnDataTo(
        trace,
        filter = null,
        commitDiagnostics = resolveElement.getContainingCjFile().reportControlFlowDiagnostics
    )
}

var CjFile.reportControlFlowDiagnostics: Boolean by NotNullablePsiCopyableUserDataProperty(
    Key.create("REPORT_CONTROL_FLOW_DIAGNOSTICS_IN_K1"),
    false
)
