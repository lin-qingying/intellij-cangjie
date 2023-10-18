package com.huawei.cangjie1.resolve

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.name.Name
import com.huawei.cangjie1.renderer.render


data class ImportPath @JvmOverloads constructor(val fqName: FqName, val isAllUnder: Boolean, val alias: Name? = null) {

    val pathStr: String
        get() = fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.asString() else ""
    }

    fun hasAlias(): Boolean {
        return alias != null
    }

    val importedName: Name?
        get() {
            if (!isAllUnder) {
                return alias ?: fqName.shortName()
            }

            return null
        }

    companion object {
        @JvmStatic
        fun fromString(pathStr: String): ImportPath {
            return if (pathStr.endsWith(".*")) {
                ImportPath(FqName(pathStr.substring(0, pathStr.length - 2)), isAllUnder = true)
            } else {
                ImportPath(FqName(pathStr), isAllUnder = false)

            }
        }
    }
}
