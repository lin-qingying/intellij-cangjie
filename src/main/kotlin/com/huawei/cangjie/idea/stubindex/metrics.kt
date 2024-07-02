package com.huawei.cangjie.idea.stubindex

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.annotations.ApiStatus
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ApiStatus.Internal
inline fun <T> getAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getAllKeys", processElementsMaxDuration(), log, block)

@ApiStatus.Internal
inline fun getByKeyMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.single").toDuration(DurationUnit.MILLISECONDS)

@ApiStatus.Internal
inline fun processElementsMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.batch").toDuration(DurationUnit.MILLISECONDS)

@ApiStatus.Internal
inline fun <T> processAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "processAllKeys", processElementsMaxDuration(), log, block)

@ApiStatus.Internal
inline fun <T> getByKeyAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getByKey", getByKeyMaxDuration(), log, block)
