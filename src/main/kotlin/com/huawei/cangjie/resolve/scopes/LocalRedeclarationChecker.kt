package com.huawei.cangjie.resolve.scopes

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.reportOnDeclarationOrFail
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.resolve.OverloadChecker

abstract class AbstractLocalRedeclarationChecker(val overloadChecker: OverloadChecker) : LocalRedeclarationChecker{

    protected abstract fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor)
    protected abstract fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor)


    override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {
        val name = newDescriptor.name
        val location = NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS
        when (newDescriptor) {
            is ClassifierDescriptor, is VariableDescriptorBase -> {
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
        reportOnDeclarationOrFail(trace, first) { Errors.REDECLARATION.on(it, listOf(first, second)) }
        reportOnDeclarationOrFail(trace, second) { Errors.REDECLARATION.on(it, listOf(first, second)) }
    }

    override fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        reportOnDeclarationOrFail(trace, first) { Errors.CONFLICTING_OVERLOADS.on(it, listOf(first, second)) }
        reportOnDeclarationOrFail(trace, second) { Errors.CONFLICTING_OVERLOADS.on(it, listOf(first, second)) }
    }

}

