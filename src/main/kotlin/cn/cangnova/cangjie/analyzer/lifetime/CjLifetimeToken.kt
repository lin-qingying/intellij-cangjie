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

package cn.cangnova.cangjie.analyzer.lifetime

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.reflect.KClass


abstract class CjLifetimeToken


interface CjLifetimeOwner {
    val token: CjLifetimeToken
}

inline fun <R> CjLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}

@Suppress("NOTHING_TO_INLINE")

inline fun CjLifetimeToken.assertIsValidAndAccessible() {
//    if (!isValid()) {
//        throw CjInvalidLifetimeOwnerAccessException("Access to invalid $this: ${getInvalidationReason()}")
//    }
//    if (!isAccessible()) {
//        throw CjInaccessibleLifetimeOwnerAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
//    }
}

@Suppress("NOTHING_TO_INLINE")

inline fun CjLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

abstract class CjLifetimeTokenFactory {
    abstract val identifier: KClass<out CjLifetimeToken>
    abstract fun create(project: Project): CjLifetimeToken

    open fun beforeEnteringAnalysisContext(token: CjLifetimeToken) {}
    open fun afterLeavingAnalysisContext(token: CjLifetimeToken) {}
}

abstract class CjLifetimeTokenProvider {
    abstract fun getLifetimeTokenFactory(): CjLifetimeTokenFactory

    companion object {

        fun getService(project: Project): CjLifetimeTokenProvider =
            project.getService(CjLifetimeTokenProvider::class.java)
    }
}


class CjReadActionConfinementLifetimeTokenProvider : CjLifetimeTokenProvider() {
    override fun getLifetimeTokenFactory(): CjLifetimeTokenFactory {
        return CjReadActionConfinementLifetimeTokenFactory
    }
}

object CjReadActionConfinementLifetimeTokenFactory : CjLifetimeTokenFactory() {
    override val identifier: KClass<out CjLifetimeToken> = CjReadActionConfinementLifetimeToken::class

    override fun create(project: Project): CjLifetimeToken = CjReadActionConfinementLifetimeToken(project)

    override fun beforeEnteringAnalysisContext(token: CjLifetimeToken) {
        lifetimeOwnersStack.set(lifetimeOwnersStack.get().add(token))
    }

    override fun afterLeavingAnalysisContext(token: CjLifetimeToken) {
        val stack = lifetimeOwnersStack.get()
        val last = stack.last()
        check(last == token)
        lifetimeOwnersStack.set(stack.removeAt(stack.lastIndex))
    }

    private val lifetimeOwnersStack = ThreadLocal.withInitial<PersistentList<CjLifetimeToken>> { persistentListOf() }

    internal fun isInsideAnalysisContext() = lifetimeOwnersStack.get().isNotEmpty()

    internal fun currentToken() = lifetimeOwnersStack.get().last()
}

class CjReadActionConfinementLifetimeToken(project: Project) : CjLifetimeToken() {
//    private val modificationTracker = project.createProjectWideOutOfBlockModificationTracker()
//    private val onCreatedTimeStamp = modificationTracker.modificationCount

//    override fun isValid(): Boolean {
//        return onCreatedTimeStamp == modificationTracker.modificationCount
//    }
//
//    override fun getInvalidationReason(): String {
//        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
//        error("Getting invalidation reason for valid validity token")
//    }
//
//    override fun isAccessible(): Boolean {
//        val application = ApplicationManager.getApplication()
//        if (application.isDispatchThread && !allowOnEdt.get()) return false
//        if (application.isWriteAccessAllowed && !allowFromWriteAction.get()) return false
//        if (CjAnalysisAllowanceManager.resolveIsForbiddenInActionWithName.get() != null) return false
//        if (!application.isReadAccessAllowed) return false
//        if (!CjReadActionConfinementLifetimeTokenFactory.isInsideAnalysisContext()) return false
//        if (CjReadActionConfinementLifetimeTokenFactory.currentToken() != this) return false
//        return true
//    }
//
//    override fun getInaccessibilityReason(): String {
//        val application = ApplicationManager.getApplication()
//        if (application.isDispatchThread && !allowOnEdt.get()) return "Called in EDT thread"
//        if (application.isWriteAccessAllowed && !allowFromWriteAction.get()) return "Called from write action"
//        if (!application.isReadAccessAllowed) return "Called outside read action"
//        CjAnalysisAllowanceManager.resolveIsForbiddenInActionWithName.get()?.let { actionName ->
//            return "Resolve is forbidden in $actionName"
//        }
//        if (!CjReadActionConfinementLifetimeTokenFactory.isInsideAnalysisContext()) return "Called outside analyse method"
//        if (CjReadActionConfinementLifetimeTokenFactory.currentToken() != this) return "Using CjLifetimeOwner from previous analysis"
//
//        error("Getting inaccessibility reason for validity token when it is accessible")
//    }
//

    companion object {

        val allowOnEdt: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }


        val allowFromWriteAction: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }

//    override val factory: CjLifetimeTokenFactory = CjReadActionConfinementLifetimeTokenFactory
}
