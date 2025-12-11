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

package org.cangnova.cangjie.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.LexicalScope


interface CheckerContext {
    val trace: BindingTrace

    val deprecationResolver: DeprecationResolver
    val languageVersionSettings: LanguageVersionSettings

    val moduleDescriptor: ModuleDescriptor
}


interface CallChecker {
    /**
     * Note that [reportOn] should only be used as a target element for diagnostics reported by checkers.
     * Logic of the checker should not depend on what element is the target of the diagnostic!
     */
    fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext)
}

class CallCheckerContext @JvmOverloads constructor(
    val resolutionContext: ResolutionContext<*>,
    override val deprecationResolver: DeprecationResolver,
    override val moduleDescriptor: ModuleDescriptor,
    val missingSupertypesResolver: MissingSupertypesResolver,
    val callComponents: CangJieCallComponents,
    override val trace: BindingTrace = resolutionContext.trace
) : CheckerContext {
    val scope: LexicalScope
        get() = resolutionContext.scope

    val dataFlowInfo: DataFlowInfo
        get() = resolutionContext.dataFlowInfo

    val isAnnotationContext: Boolean
        get() = resolutionContext.isAnnotationContext
    override val languageVersionSettings: LanguageVersionSettings
        get() = resolutionContext.languageVersionSettings
    val dataFlowValueFactory: DataFlowValueFactory
        get() = resolutionContext.dataFlowValueFactory


}

// Use this utility to avoid premature computation of deferred return type of a resolved callable descriptor.
// Computing it in CallChecker#check is not feasible since it would trigger "type checking has run into a recursive problem" errors.
// Receiver parameter is present to emphasize that this function should ideally be only used from call checkers.
//@Suppress("unused")
//fun CallChecker.isComputingDeferredType(type: CangJieType) =
//    type is DeferredType && type.isComputing
