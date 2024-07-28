
package com.huawei.cangjie.ide.formatter

class CangJiePackageEntry(
    packageName: String,
    val withSubpackages: Boolean
) {
    val packageName = packageName.removeSuffix(".*")

    companion object {
        @JvmField
        val ALL_OTHER_IMPORTS_ENTRY = CangJiePackageEntry("<all other imports>", withSubpackages = true)

        @JvmField
        val ALL_OTHER_ALIAS_IMPORTS_ENTRY = CangJiePackageEntry("<all other alias imports>", withSubpackages = true)
    }

    fun matchesPackageName(otherPackageName: String): Boolean {
        if (otherPackageName.startsWith(packageName)) {
            if (otherPackageName.length == packageName.length) return true
            if (withSubpackages) {
                if (otherPackageName[packageName.length] == '.') return true
            }
        }
        return false
    }

    val isSpecial: Boolean get() = this == ALL_OTHER_IMPORTS_ENTRY || this == ALL_OTHER_ALIAS_IMPORTS_ENTRY

    override fun toString(): String {
        return packageName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangJiePackageEntry) return false

        return withSubpackages == other.withSubpackages && packageName == other.packageName
    }

    override fun hashCode(): Int = packageName.hashCode()
}
