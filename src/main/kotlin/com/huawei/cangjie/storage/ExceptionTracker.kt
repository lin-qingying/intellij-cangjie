package com.huawei.cangjie.storage

import com.huawei.cangjie.utils.rethrow
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
