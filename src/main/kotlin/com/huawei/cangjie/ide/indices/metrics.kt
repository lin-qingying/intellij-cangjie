package com.huawei.cangjie.ide.indices

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.stubs.StubIndexKey
import kotlin.time.*

  fun getByKeyMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.single").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> getByKeyAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getByKey", getByKeyMaxDuration(), log, block)

fun processElementsMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.batch").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> measureIndexCall(
    index: StubIndexKey<*, *>,
    prefix: String,
    threshold: Duration,
    log: Logger,
    crossinline block: () -> T
): T {
    val mark = TimeSource.Monotonic.markNow()
    val t = block()
    val elapsed = mark.elapsedNow()
    if (elapsed > threshold) {
        val application = ApplicationManager.getApplication()
        if (application.isInternal && !application.isUnitTestMode && Registry.`is`("cangjie.indices.timing.enabled")) {
            log.error("${index.name} $prefix took $elapsed more than expected $threshold")
        }
    }
    return t
}

inline fun <T> processElementsAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(
        index,
        "processElements",
        processElementsMaxDuration(),
        log,
        block
    )
