package com.linqingying.cangjie.analyzer.lifetime

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
