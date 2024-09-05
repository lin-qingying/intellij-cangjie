package com.huawei.cangjie.resolve.deprecation

abstract class DeprecationInfo : Comparable<DeprecationInfo> {
    abstract val deprecationLevel: DeprecationLevelValue
    abstract val propagatesToOverrides: Boolean
    abstract val message: String?

    override fun compareTo(other: DeprecationInfo): Int {
        val lr = deprecationLevel.compareTo(other.deprecationLevel)
        //to prefer inheritable deprecation
        return if (lr == 0 && !propagatesToOverrides && other.propagatesToOverrides) 1
        else lr
    }
}
/**
 * This corresponds to [DeprecationLevel] in Kotlin standard library. A symbol annotated with [java.lang.Deprecated] is considered a
 * warning.
 */
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}
