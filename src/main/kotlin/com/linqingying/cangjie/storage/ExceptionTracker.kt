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

package com.linqingying.cangjie.storage

import com.linqingying.cangjie.utils.rethrow
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ModificationTracker
import java.util.concurrent.atomic.AtomicLong


abstract class LazyWrappedTypeComputationException : RuntimeException()

fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}

object CacheResetOnProcessCanceled {
    private const val PROPERTY = "cangjie.internal.cacheResetOnProcessCanceled"
    private const val DEFAULT_VALUE = false

    var enabled: Boolean
        get() = PropertiesComponent.getInstance()?.getBoolean(PROPERTY, DEFAULT_VALUE) ?: DEFAULT_VALUE
        set(value) {
            PropertiesComponent.getInstance()?.setValue(PROPERTY, value, DEFAULT_VALUE)
        }
}

class ReenteringLazyValueComputationException : LazyWrappedTypeComputationException() {
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        val application = ApplicationManager.getApplication()
        if (application == null || application.isInternal || application.isUnitTestMode) {
            return super.fillInStackTrace()
        }
        return this
    }
}


open class ExceptionTracker : ModificationTracker, LockBasedStorageManager.ExceptionHandlingStrategy {

    private val cancelledTracker: AtomicLong = AtomicLong()

    override fun getModificationCount(): Long {
        return cancelledTracker.get()

    }

    private fun incCounter() {
        cancelledTracker.andIncrement
    }

    override fun handleException(throwable: Throwable): RuntimeException {
        // should not increment counter when ReenteringLazyValueComputationException is thrown since it implements correct frontend behaviour
        if (throwable !is ReenteringLazyValueComputationException) {
            if (!throwable.isProcessCanceledException() || CacheResetOnProcessCanceled.enabled) {
                incCounter()
            }
        }
        throw rethrow(throwable)
    }

}
