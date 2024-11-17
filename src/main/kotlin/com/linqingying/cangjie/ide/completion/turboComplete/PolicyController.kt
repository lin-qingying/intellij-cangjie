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

package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionResultSet


import com.intellij.util.containers.Stack
//
//class PolicyController(protected val originalResult: CompletionResultSet) : () -> ElementsAddingPolicy {
//    private val policies: Stack<ElementsAddingPolicy> = Stack()
//
//    /**
//     * Make the [policy] rule how elements are added to the [originalResult]
//     * If there is already an active policy A in the controller, than it
//     * will be put on the stack. So that when the newly added policy will
//     * be popped, the policy A will be in action again.
//     *
//     * @see popPolicy
//     */
//    private fun pushPolicy(policy: ElementsAddingPolicy) {
//        policies.push(policy)
//        policy.onActivate(originalResult)
//    }
//
//    /**
//     * Revoke currently active policy
//     *
//     * @throws NoActivePolicyException if there is no active policy
//     * @see [pushPolicy]
//     */
//    private fun popPolicy() {
//        verifyNotEmptyStack()
//        val policyToDeactivate = policies.pop()
//        policyToDeactivate.onDeactivate(originalResult)
//    }
//
//    /**
//     * @return A result set, that will be obeying to this controller
//     */
//    fun getObeyingResultSet(): CompletionResultSet {
//        return PolicyObeyingResultSet(originalResult, this)
//    }
//
//    /**
//     * Invoke the given action
//     */
//    fun <T> invokeWithPolicy(policy: ElementsAddingPolicy, action: () -> T): T {
//        pushPolicy(policy)
//        try {
//            return action()
//        }
//        finally {
//            popPolicy()
//        }
//    }
//
//    override fun invoke(): ElementsAddingPolicy {
//        verifyNotEmptyStack()
//        return policies.peek()!!
//    }
//
//    private fun verifyNotEmptyStack() {
//        if (policies.isEmpty()) {
//            throw NoActivePolicyException()
//        }
//    }
//
//    public class NoActivePolicyException : Exception("ElementsAddingPolicyController does not have an active policy")
//}
