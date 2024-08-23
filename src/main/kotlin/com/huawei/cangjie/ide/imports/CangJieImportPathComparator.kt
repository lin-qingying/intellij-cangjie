package com.huawei.cangjie.ide.imports

import com.huawei.cangjie.ide.formatter.CangJiePackageEntry
import com.huawei.cangjie.ide.formatter.CangJiePackageEntry.Companion.ALL_OTHER_ALIAS_IMPORTS_ENTRY
import com.huawei.cangjie.ide.formatter.CangJiePackageEntryTable
import com.huawei.cangjie.ide.formatter.cangjieCustomSettings
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.ImportPath

class CangJieImportPathComparator private constructor(private val packageTable: CangJiePackageEntryTable) : Comparator<ImportPath> {
    override fun compare(import1: ImportPath, import2: ImportPath): Int {
        val ignoreAlias = import1.hasAlias() && import2.hasAlias()

        return compareValuesBy(
            import1,
            import2,
            { import -> bestEntryMatchIndex(import, ignoreAlias) },
            { import ->
                // Ignore backticks when comparing lexicographically
                import.toString().replace("`", "")
            }
        )
    }

    private fun bestEntryMatchIndex(path: ImportPath, ignoreAlias: Boolean): Int {
        var bestEntryMatch: CangJiePackageEntry? = null
        var bestIndex: Int = -1

        for ((index, entry) in packageTable.getEntries().withIndex()) {
            if (entry.isBetterMatchForPackageThan(bestEntryMatch, path, ignoreAlias)) {
                bestEntryMatch = entry
                bestIndex = index
            }
        }

        return bestIndex
    }

    companion object {
        fun create(file: CjFile): Comparator<ImportPath> {
            val packagesImportLayout = file.cangjieCustomSettings.PACKAGES_IMPORT_LAYOUT
            return CangJieImportPathComparator(packagesImportLayout)
        }
    }
}
private fun CangJiePackageEntry.isBetterMatchForPackageThan(entry: CangJiePackageEntry?, path: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!matchesImportPath(path, ignoreAlias)) return false
    if (entry == null) return true

    // Any matched package is better than ALL_OTHER_IMPORTS_ENTRY
    if (this == CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY) return false
    if (entry == CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY) return true

    if (entry.withSubpackages != withSubpackages) return !withSubpackages

    return entry.packageName.count { it == '.' } < packageName.count { it == '.' }
}
/**
 * In current implementation we assume that aliased import can be matched only by
 * [ALL_OTHER_ALIAS_IMPORTS_ENTRY] which is always present.
 */
private fun CangJiePackageEntry.matchesImportPath(importPath: ImportPath, ignoreAlias: Boolean): Boolean {
    if (!ignoreAlias && importPath.hasAlias()) {
        return this == ALL_OTHER_ALIAS_IMPORTS_ENTRY
    }

    if (this == CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY) return true

    return matchesPackageName(importPath.pathStr)
}
