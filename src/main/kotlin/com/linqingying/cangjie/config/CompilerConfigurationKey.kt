package com.linqingying.cangjie.config

import com.intellij.openapi.util.Key
import org.jetbrains.annotations.NonNls

class CompilerConfigurationKey<T>(@NonNls name: String) {
    var ideaKey: Key<T> = Key.create(name)

    override fun toString(): String {
        return ideaKey.toString()
    }

    companion object {
        fun <T> create(@NonNls name: String): CompilerConfigurationKey<T> {
            return CompilerConfigurationKey (name)
        }
    }
}
