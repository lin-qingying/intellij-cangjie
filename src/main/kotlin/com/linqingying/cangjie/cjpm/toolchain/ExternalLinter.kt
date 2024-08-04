package com.linqingying.cangjie.cjpm.toolchain

import com.linqingying.cangjie.BUNDLE
import com.linqingying.cangjie.CangJieBundle
import org.jetbrains.annotations.PropertyKey

enum class ExternalLinter( val titleKey: String) {
    CJPM_CHECK("cangjie.external.linter.cjpm.check.item"),
    CLIPPY("cangjie.external.linter.clippy.item");

    val title: String get() = CangJieBundle.message(titleKey)

    override fun toString(): String = title

    companion object {
        @JvmField
        val DEFAULT: ExternalLinter = CJPM_CHECK
    }
}
