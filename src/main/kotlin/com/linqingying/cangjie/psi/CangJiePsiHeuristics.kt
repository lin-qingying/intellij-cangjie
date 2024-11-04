package com.linqingying.cangjie.psi

import com.google.common.collect.HashMultimap
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.stubs.containingCangJieFileStub
import com.linqingying.cangjie.utils.exceptions.OperatorConventions

object CangJiePsiHeuristics {
    @JvmStatic
    fun isPossibleOperator(declaration: CjNamedFunction): Boolean {
        if (declaration.hasModifier(CjTokens.OPERATOR_KEYWORD)) {
            return true
        } else if (!declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
            // Operator modifier could be omitted only for overridden function
            return false
        }

        val name = declaration.name ?: return false
        return OperatorConventions.isConventionName(Name.identifier(name))
    }
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
