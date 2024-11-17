/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.NotNullablePsiCopyableUserDataProperty
import com.linqingying.cangjie.resolve.DelegatingBindingTrace
import com.linqingying.cangjie.resolve.lazy.ResolveSession
import com.intellij.openapi.util.Key

fun analyzeControlFlow(resolveSession: ResolveSession, resolveElement: CjElement, trace: BindingTrace) {
    val controlFlowTrace = DelegatingBindingTrace(
        trace.bindingContext, "Element control flow resolve", resolveElement, allowSliceRewrite = true
    )
    ControlFlowInformationProviderImpl(
        resolveElement, controlFlowTrace, resolveElement.languageVersionSettings,/* resolveSession.platformDiagnosticSuppressor*/
    ) .checkDeclaration()
    controlFlowTrace.addOwnDataTo(trace, filter = null, commitDiagnostics = resolveElement.getContainingCjFile().reportControlFlowDiagnostics )
}
var CjFile.reportControlFlowDiagnostics : Boolean by NotNullablePsiCopyableUserDataProperty(Key.create("REPORT_CONTROL_FLOW_DIAGNOSTICS_IN_K1"), false)
