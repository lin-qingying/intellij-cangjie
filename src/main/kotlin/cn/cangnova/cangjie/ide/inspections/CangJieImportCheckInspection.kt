package cn.cangnova.cangjie.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import cn.cangnova.cangjie.descriptors.PackageViewDescriptor
import cn.cangnova.cangjie.diagnostics.Errors.IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED
import cn.cangnova.cangjie.diagnostics.Errors.MODULE_PACKAGE_CANNOT_BE_IMPORTED
import cn.cangnova.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.CjImportDirective
import cn.cangnova.cangjie.psi.importDirectiveVisitor
import cn.cangnova.cangjie.resolve.descriptorUtil.targetDescriptors

class CangJieImportCheckInspection : AbstractCangJieInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return importDirectiveVisitor(
            fun(importDeclaration: CjImportDirective) {

                checkRedundantImport(holder, importDeclaration)

            }
        )
    }


    //    检查重导出语句 与 模块导入
    private fun checkRedundantImport(holder: ProblemsHolder, importDeclaration: CjImportDirective ) {

        importDeclaration.items.forEach {
            val fqname = it.importedFqName
            if (fqname?.isModuleName == true) {

                holder.report(MODULE_PACKAGE_CANNOT_BE_IMPORTED.on(it.importedReference))
                return
            }

            val importForDirective = it.targetDescriptors()

            val isRedundant = importDeclaration.hasModifier(CjTokens.PUBLIC_KEYWORD) ||
                    importDeclaration.hasModifier(CjTokens.PROTECTED_KEYWORD) ||
                    importDeclaration.hasModifier(CjTokens.INTERNAL_KEYWORD)

            if (isRedundant && importForDirective.any { descriptor -> descriptor is PackageViewDescriptor }) {
                val visibility = importForDirective.first().visibility
                holder.report(
                    IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED.on(it,it.importedFqName,visibility)
                )
            }
        }


    }

}
