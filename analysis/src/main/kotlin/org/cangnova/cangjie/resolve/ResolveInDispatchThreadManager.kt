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

package org.cangnova.cangjie.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry

private const val RESOLVE_IN_DISPATCH_THREAD_ERROR_MESSAGE = "Resolve is not allowed in dispatch thread!"

class ResolveInDispatchThreadException(message: String? = null) :
    IllegalThreadStateException(message ?: RESOLVE_IN_DISPATCH_THREAD_ERROR_MESSAGE)

/**
 * Temporary allow resolve in dispatch thread.
 *
 * All resolve should be banned from the UI thread. This method is needed for the transition period to document
 * places that are not fixed yet.
 */
fun <T> allowResolveInDispatchThread(runnable: () -> T): T {
    return ResolveInDispatchThreadManager.runWithResolveAllowedInDispatchThread(runnable)
}

object ResolveInDispatchThreadManager {
    private val LOG = Logger.getInstance(ResolveInDispatchThreadManager::class.java)

    // Guarded by isDispatchThread check
    private var isResolveAllowed: Boolean = false

    // Guarded by isDispatchThread check
    private var isForceCheckInTests: Boolean = false

    // Guarded by isDispatchThread check
    private var errorHandler: (() -> Unit)? = null

    fun assertNoResolveInDispatchThread() {
        val application = ApplicationManager.getApplication() ?: return
        if (!application.isDispatchThread) {
            return
        }

        if (isResolveAllowed) return

        if (application.isUnitTestMode) {
            if (!isForceCheckInTests) return

            val handler = errorHandler
            if (handler != null) {
                handler()
                return
            }

            throw ResolveInDispatchThreadException()
        } else {
            if (!Registry.`is`("cangjie.dispatch.thread.resolve.check", false)) return

            LOG.error(RESOLVE_IN_DISPATCH_THREAD_ERROR_MESSAGE)
        }
    }

    internal inline fun <T> runWithResolveAllowedInDispatchThread(runnable: () -> T): T {
        val wasSet =
            if (ApplicationManager.getApplication()?.isDispatchThread == true && !isResolveAllowed) {
                isResolveAllowed = true
                true
            } else {
                false
            }
        try {
            return runnable()
        } finally {
            if (wasSet) {
                isResolveAllowed = false
            }
        }
    }

    internal fun <T> runWithForceCheckForResolveInDispatchThreadInTests(
        errorHandler: (() -> Unit)?,
        runnable: () -> T
    ): T {
        ApplicationManager.getApplication()?.assertIsDispatchThread() ?: error("Application is not available")

        val wasSet = if (!isForceCheckInTests) {
            isForceCheckInTests = true
            this.errorHandler = errorHandler
            true
        } else {
            false
        }

        try {
            return runnable()
        } finally {
            if (wasSet) {
                isForceCheckInTests = false
                this.errorHandler = null
            }
        }
    }
}
