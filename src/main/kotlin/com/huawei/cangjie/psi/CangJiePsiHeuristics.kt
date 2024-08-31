package com.huawei.cangjie.psi

import com.google.common.collect.HashMultimap
import com.huawei.cangjie.psi.stubs.containingCangJieFileStub

object CangJiePsiHeuristics {
    @JvmStatic
    fun isProbablyNothing(type: CjBasicType): Boolean {
        val referencedName = type.text

        if (referencedName == "Nothing") {
            return true
        }

        // TODO: why don't use PSI-less stub for calculating aliases?
        val file = type.getContainingCjFile()

        // TODO: support type aliases
        if (!file.hasImportAlias()) return false
        return file.aliasImportMap[referencedName].contains("Nothing")
    }
    @JvmStatic
    fun isProbablyNothing(typeReference: CjTypeReference): Boolean {
        return false
        val userType = typeReference.typeElement as? CjBasicType ?: return false
        return isProbablyNothing(userType)
    }

    private val CjFile.aliasImportMap by userDataCached("ALIAS_IMPORT_MAP_KEY") { file ->
        HashMultimap.create<String, String>().apply {
            for (import in file.importList?.imports.orEmpty()) {
                val aliasName = import.aliasName ?: continue
                val name = import.importPath?.fqName?.shortName()?.asString() ?: continue
                put(aliasName, name)
            }
        }
    }
}
