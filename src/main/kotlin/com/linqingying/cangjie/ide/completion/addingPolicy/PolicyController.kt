package com.linqingying.cangjie.ide.completion.addingPolicy

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.containers.Stack
import com.linqingying.cangjie.ide.completion.turboComplete.ElementsAddingPolicy

class PolicyController(private val originalResult: CompletionResultSet) : () -> ElementsAddingPolicy {
    private val policies: Stack<ElementsAddingPolicy> = Stack()

    /**
     * Make the [policy] rule how elements are added to the [originalResult]
     * If there is already an active policy A in the controller, than it
     * will be put on the stack. So that when the newly added policy will
     * be popped, the policy A will be in action again.
     *
     * @see popPolicy
     */
    private fun pushPolicy(policy: ElementsAddingPolicy) {
        policies.push(policy)
        policy.onActivate(originalResult)
    }

    /**
     * Revoke currently active policy
     *
     * @throws NoActivePolicyException if there is no active policy
     * @see [pushPolicy]
     */
    private fun popPolicy() {
        verifyNotEmptyStack()
        val policyToDeactivate = policies.pop()
        policyToDeactivate.onDeactivate(originalResult)
    }

    /**
     * @return A result set, that will be obeying to this controller
     */
    fun getObeyingResultSet(): CompletionResultSet {
//        return PolicyDrivenResultSet(originalResult, this)
        return originalResult
    }

    /**
     * Invoke the given action
     */
    fun <T> invokeWithPolicy(policy: ElementsAddingPolicy, action: () -> T): T {
        pushPolicy(policy)
        try {
            return action()
        } finally {
            popPolicy()
        }
    }

    override fun invoke(): ElementsAddingPolicy {
        verifyNotEmptyStack()
        return policies.peek()!!
    }

    private fun verifyNotEmptyStack() {
        if (policies.isEmpty()) {
            throw NoActivePolicyException()
        }
    }

    class NoActivePolicyException : Exception("ElementsAddingPolicyController does not have an active policy")
}