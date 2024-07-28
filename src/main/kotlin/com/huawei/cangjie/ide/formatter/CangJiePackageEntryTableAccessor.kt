

package com.huawei.cangjie.ide.formatter

import com.intellij.application.options.codeStyle.properties.CodeStylePropertiesUtil
import com.intellij.application.options.codeStyle.properties.ValueListPropertyAccessor

import java.lang.reflect.Field

class CangJiePackageEntryTableAccessor(kotlinCodeStyle: CangJieCodeStyleSettings, field: Field) :
    ValueListPropertyAccessor<CangJiePackageEntryTable>(kotlinCodeStyle, field) {
    override fun valueToString(value: List<String>): String = CodeStylePropertiesUtil.toCommaSeparatedString(value)

    override fun fromExternal(extVal: List<String>): CangJiePackageEntryTable = CangJiePackageEntryTable(
        extVal.asSequence().map(String::trim).map(::readPackageEntry).toMutableList()
    )

    override fun isEmptyListAllowed(): Boolean = false

    override fun toExternal(value: CangJiePackageEntryTable): List<String> = value.getEntries().map(::writePackageEntry)

    companion object {
        private const val ALIAS_CHAR = "^"
        private const val OTHER_CHAR = "*"

        private fun readPackageEntry(string: String): CangJiePackageEntry = when {
            string == ALIAS_CHAR -> CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
            string == OTHER_CHAR -> CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY
            string.endsWith("**") -> CangJiePackageEntry(string.substring(0, string.length - 1), true)
            else -> CangJiePackageEntry(string, false)
        }

        private fun writePackageEntry(entry: CangJiePackageEntry): String = when (entry) {
            CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY -> ALIAS_CHAR
            CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY -> OTHER_CHAR
            else -> "${entry.packageName}.*" + if (entry.withSubpackages) "*" else ""
        }
    }
}

