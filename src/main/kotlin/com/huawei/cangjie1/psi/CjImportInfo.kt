package com.huawei.cangjie1.psi

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.name.Name


interface CjImportInfo {
    sealed class ImportContent {
        class ExpressionBased(val expression: CjExpression) : ImportContent()
        class FqNameBased(val fqName: FqName) : ImportContent()
    }

    val isAllUnder: Boolean
    val importContent: ImportContent?
    val importedFqName: FqName?
    val aliasName: String?

    @JvmDefault
    val importedName: Name?
        get() {
            return computeNameAsString()?.takeIf(CharSequence::isNotEmpty)?.let(Name::identifier)
        }

    private fun computeNameAsString(): String? {
        if (isAllUnder) return null
//        aliasName?.let { return it }
        val importContent = importContent
        return when (importContent) {
            is ImportContent.ExpressionBased -> CjPsiUtil.getLastReference(importContent.expression)
                ?.getReferencedName()

            is ImportContent.FqNameBased -> importContent.fqName.takeUnless(FqName::isRoot)?.shortName()?.asString()
            null -> null
        }
    }
}
