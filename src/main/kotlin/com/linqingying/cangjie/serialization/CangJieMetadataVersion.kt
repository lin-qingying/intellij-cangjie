package com.linqingying.cangjie.serialization

import com.linqingying.cangjie.metadata.deserialization.BinaryVersion

class CangJieMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    companion object {
        @JvmField
        val INSTANCE = CangJieMetadataVersion(2, 1, 0)

        @JvmField
        val INSTANCE_NEXT = INSTANCE.next()

        @JvmField
        val INVALID_VERSION = CangJieMetadataVersion()
    }
    private fun newerThan(other: CangJieMetadataVersion): Boolean {
        return when {
            major > other.major -> true
            major < other.major -> false
            minor > other.minor -> true
            else -> false
        }
    }
    fun next(): CangJieMetadataVersion =
        if (major == 1 && minor == 9) CangJieMetadataVersion(2, 0, 0)
        else CangJieMetadataVersion(major, minor + 1, 0)

    override fun isCompatibleWithCurrentCompilerVersion(): Boolean {
        return isCompatibleInternal(if (isStrictSemantics) INSTANCE else INSTANCE_NEXT)
    }
    private fun isCompatibleInternal(limitVersion: CangJieMetadataVersion): Boolean {
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        if (major == 1 && minor == 0) return false
        // The same for 0.*
        if (major == 0) return false
        // Otherwise we just compare with the given limitVersion
        return !newerThan(limitVersion)
    }

}
