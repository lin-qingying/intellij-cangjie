/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */


package org.cangnova.cangjie.formatter

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
