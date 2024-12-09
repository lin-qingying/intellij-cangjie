package com.linqingying.cangjie.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.linqingying.cangjie.descriptors.PackageViewDescriptor
import com.linqingying.cangjie.diagnostics.Errors.IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED
import com.linqingying.cangjie.diagnostics.Errors.MODULE_PACKAGE_CANNOT_BE_IMPORTED
import com.linqingying.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.importDirectiveVisitor
import com.linqingying.cangjie.resolve.descriptorUtil.targetDescriptors

class CangJieImportCheckInspection : AbstractCangJieInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return importDirectiveVisitor(
            fun(importDeclaration: CjImportDirective) {

                checkRedundantImport(holder, importDeclaration)

            }
        )
    }


    //    检查重导出语句 与 模块导入
    fun checkRedundantImport(holder: ProblemsHolder, importDeclaration: CjImportDirective) {
        val fqname = importDeclaration.importedFqName
        if (fqname?.isModuleName == true) {

            holder.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(importDeclaration.importedReference))
            return
        }

        val importForDirective = importDeclaration.targetDescriptors()

        val isRedundant = importDeclaration.hasModifier(CjTokens.PUBLIC_KEYWORD) ||
                importDeclaration.hasModifier(CjTokens.PROTECTED_KEYWORD) ||
                importDeclaration.hasModifier(CjTokens.INTERNAL_KEYWORD)

        if (isRedundant && importForDirective.any { it is PackageViewDescriptor }) {
            val visibility = importForDirective.first().visibility
        holder.report(
            IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(importDeclaration,importDeclaration.importedFqName,visibility)
        )
        }

    }

}
