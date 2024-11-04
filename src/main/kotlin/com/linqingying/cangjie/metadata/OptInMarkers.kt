
package com.linqingying.cangjie.metadata

/**
 * Marks an API related to the Kotlin's [context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md) experimental feature.
 *
 * Marked API reflects metadata written by this feature, and can be changed or removed as development continues.
 * Therefore, it does not provide any compatibility guarantees.
 */
@RequiresOptIn(
    "The API is related to the experimental feature \"context receivers\" (see KEEP-259) and may be changed or removed in any future release.",
    RequiresOptIn.Level.ERROR
)
@MustBeDocumented
public annotation class ExperimentalContextReceivers
