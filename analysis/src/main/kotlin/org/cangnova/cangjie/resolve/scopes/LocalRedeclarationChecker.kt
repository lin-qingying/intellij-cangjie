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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.reportOnDeclarationOrFail
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.OverloadChecker
import org.cangnova.cangjie.resolve.binding.BindingTrace

abstract class AbstractLocalRedeclarationChecker(val overloadChecker: OverloadChecker) : LocalRedeclarationChecker {

    protected abstract fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor)
    protected abstract fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor)


    override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {
        val name = newDescriptor.name
        val location = NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
        when (newDescriptor) {
            is ClassifierDescriptor, is VariableDescriptor -> {
                val otherDescriptor = scope.getContributedClassifier(name, location)
                    ?: scope.getContributedVariables(name, location).firstOrNull()
                if (otherDescriptor != null) {
                    handleRedeclaration(otherDescriptor, newDescriptor)
                }
            }

            is FunctionDescriptor -> {
                val otherFunctions = scope.getContributedFunctions(name, location)
                val otherClass = scope.getContributedClassifier(name, location)
                val potentiallyConflictingOverloads =
                    if (otherClass is ClassDescriptor)
                        otherFunctions + otherClass.constructors
                    else
                        otherFunctions

                for (overloadedDescriptor in potentiallyConflictingOverloads) {
                    if (!overloadChecker.isOverloadable(overloadedDescriptor, newDescriptor)) {
                        handleConflictingOverloads(newDescriptor, overloadedDescriptor)
                        break
                    }
                }
            }

            else -> throw IllegalStateException("Unexpected type of descriptor: ${newDescriptor::class.java.name}, descriptor: $newDescriptor")
        }
    }

}

class TraceBasedLocalRedeclarationChecker(val trace: BindingTrace, overloadChecker: OverloadChecker) :
    AbstractLocalRedeclarationChecker(overloadChecker) {
    override fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor) {
        reportOnDeclarationOrFail(trace, first) { REDECLARATION.on(it, listOf(first, second)) }
        reportOnDeclarationOrFail(trace, second) { REDECLARATION.on(it, listOf(first, second)) }
    }

    override fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        reportOnDeclarationOrFail(trace, first) { CONFLICTING_OVERLOADS.on(it, listOf(first, second)) }
        reportOnDeclarationOrFail(trace, second) { CONFLICTING_OVERLOADS.on(it, listOf(first, second)) }
    }

}

