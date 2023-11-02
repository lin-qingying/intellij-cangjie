
package org.jetbrains.kotlin.idea.core.formatter

import com.intellij.openapi.util.InvalidDataException
import org.jdom.Element

class CangJiePackageEntryTable(private val entries: MutableList<CangJiePackageEntry>) :
    com.intellij.openapi.util.JDOMExternalizable, Cloneable {
    constructor() : this(mutableListOf())

    val entryCount: Int get() = entries.size

    public override fun clone(): CangJiePackageEntryTable {
        val clone = CangJiePackageEntryTable()
        clone.copyFrom(this)
        return clone
    }

    fun copyFrom(packageTable: CangJiePackageEntryTable) {
        entries.clear()
        entries.addAll(packageTable.entries)
    }

    fun getEntries(): Array<CangJiePackageEntry> {
        return entries.toTypedArray()
    }

    fun insertEntryAt(entry: CangJiePackageEntry, index: Int) {
        entries.add(index, entry)
    }

    fun removeEntryAt(index: Int) {
        entries.removeAt(index)
    }

    fun getEntryAt(index: Int): CangJiePackageEntry {
        return entries[index]
    }

    fun setEntryAt(entry: CangJiePackageEntry, index: Int) {
        entries[index] = entry
    }

    operator fun contains(packageName: String): Boolean {
        return entries.any { !it.isSpecial && it.matchesPackageName(packageName) }
    }

    fun removeEmptyPackages() {
        entries.removeAll { it.packageName.isBlank() }
    }

    fun addEntry(entry: CangJiePackageEntry) {
        entries.add(entry)
    }

    override fun readExternal(element: Element) {
        entries.clear()

        element.children.forEach {
            if (it.name == "package") {
                val packageName = it.getAttributeValue("name") ?: throw InvalidDataException()
                val alias = it.getAttributeValue("alias")?.toBoolean() ?: false
                val withSubpackages = it.getAttributeValue("withSubpackages")?.toBoolean() ?: false

                val entry = when {
                    packageName.isEmpty() && !alias -> CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY
                    packageName.isEmpty() && alias -> CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY
                    else -> CangJiePackageEntry(packageName, withSubpackages)
                }

                entries.add(entry)
            }
        }
    }

    override fun writeExternal(parentNode: Element) {
        for (entry in entries) {
            val element = Element("package")
            parentNode.addContent(element)
            val name = if (entry.isSpecial) "" else entry.packageName
            val alias = (entry == CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY)

            element.setAttribute("name", name)
            element.setAttribute("alias", alias.toString())
            element.setAttribute("withSubpackages", entry.withSubpackages.toString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangJiePackageEntryTable) return false

        if (entryCount != other.entryCount) return false
        for (i in entries.indices) {
            if (entries[i] != other.entries[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int = entries.firstOrNull()?.hashCode() ?: 0
}
