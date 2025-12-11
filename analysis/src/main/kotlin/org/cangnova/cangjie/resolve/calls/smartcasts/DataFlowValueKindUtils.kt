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

package org.cangnova.cangjie.resolve.calls.smartcasts

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.binding.BindingContext


internal fun VariableDescriptor.variableKind(
    usageModule: ModuleDescriptor?,
    bindingContext: BindingContext,
    accessElement: CjElement,
//    languageVersionSettings: LanguageVersionSettings
): DataFlowValue.Kind {
//    if (this is PropertyDescriptor) {
//        return propertyKind(usageModule/*, languageVersionSettings*/)
//    }

//    if (this is LocalVariableDescriptor && this.isDelegated) {
//        // Local delegated property: normally unstable, but can be treated as stable in legacy mode
//        return if (languageVersionSettings.supportsFeature(ProhibitSmartcastsOnLocalDelegatedProperty))
//            DataFlowValue.Kind.PROPERTY_WITH_GETTER
//        else
//            DataFlowValue.Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY
//
//    }

//    if (this !is LocalVariableDescriptor && this !is ParameterDescriptor) return DataFlowValue.Kind.OTHER
    if (!isVar) return DataFlowValue.Kind.STABLE_VALUE
//    if (this is SyntheticFieldDescriptor) return DataFlowValue.Kind.MUTABLE_PROPERTY

//    // Local variable classification: STABLE or CAPTURED
//    val preliminaryVisitor = PreliminaryDeclarationVisitor.getVisitorByVariable(this, bindingContext)
//    // A case when we just analyse an expression alone: counts as captured
//        ?: return DataFlowValue.Kind.CAPTURED_VARIABLE
//
//    // Analyze who writes variable
//    // If there is no writer: stable
//    val writers = preliminaryVisitor.writers(this)
//    if (writers.isEmpty()) return DataFlowValue.Kind.STABLE_VARIABLE
//
//    // If access element is inside closure: captured
//    val variableContainingDeclaration = this.containingDeclaration
//    if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) {
//        // stable iff we have no writers in closures AND this closure is AFTER all writers
//        return if (preliminaryVisitor.languageVersionSettings.supportsFeature(CapturedInClosureSmartCasts) &&
//            hasNoWritersInClosures(variableContainingDeclaration, writers, bindingContext) &&
//            isAccessedInsideClosureAfterAllWriters(writers, accessElement)
//        ) {
//            DataFlowValue.Kind.STABLE_VARIABLE
//        } else {
//            DataFlowValue.Kind.CAPTURED_VARIABLE
//        }
//    }

    // Otherwise, stable iff considered position is BEFORE all writers except declarer itself
//    return if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement))
//        DataFlowValue.Kind.STABLE_VARIABLE
//    else
    return DataFlowValue.Kind.CAPTURED_VARIABLE
}
