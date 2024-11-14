package com.linqingying.cangjie.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElementVisitor
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Errors.INCONSISTENT_MACRO_PACKAGE_NAME
import com.linqingying.cangjie.diagnostics.Errors.PACKAGE_ACCESS_VIOLATION
import com.linqingying.cangjie.diagnostics.rendering.DefaultErrorMessages
import com.linqingying.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import com.linqingying.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.packageDirectiveVisitor
import com.linqingying.cangjie.resolve.toAccessControlLevel

class CangJiePackageCheckInspection : AbstractCangJieInspection() {


    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return packageDirectiveVisitor(
            fun(packageDeclaration: CjPackageDirective) {


                checkPackagelevel(packageDeclaration, holder)
                checkMacroPackage(packageDeclaration, holder)
            }
        )
    }

    /**
     * 检查宏包相关
     */
    private fun checkMacroPackage(directive: CjPackageDirective, holder: ProblemsHolder) {
        val isMacroPackage = directive.isMacroPackage
        if (isMacroPackage) {
            return
        }
        runReadAction {
            CangJieExactPackagesIndex.get(directive.fqName.asString(), directive.project).forEach { file ->
                if (file.packageDirective?.isMacroPackage == true && directive != file.packageDirective) {
                    holder.report(
                        INCONSISTENT_MACRO_PACKAGE_NAME.on(
                            directive ,

                            )
                    )
                }
            }


        }

    }


    /**
     * 该方法检查包等级，耗时操作
     */
    private fun checkPackagelevel(directive: CjPackageDirective, holder: ProblemsHolder) {

        runReadAction {
            val currentLevel = toAccessControlLevel(directive.modifierVisibility)
            if (currentLevel == 0) {
                return@runReadAction
            }
            if (directive.fqName.isModuleName) {
                return@runReadAction
            }
//                获取父包索引，检查等级
            val parentPackageFqName = directive.fqName.parent()
            CangJieExactPackagesIndex.get(parentPackageFqName.asString(), directive.project).forEach { file ->

                file.packageDirective?.modifierVisibility?.let {
                    if (toAccessControlLevel(it) < currentLevel) {

                        holder.report(
                            PACKAGE_ACCESS_VIOLATION.on(
                                directive,
                                directive.fqName,
                                file.packageFqName
                            )
                        )
                        return@forEach
                    }
                }


            }

        }

    }

}

fun ProblemsHolder.report(diagnostic: Diagnostic) {
//可以扩展  快速修复  错误级别
    registerProblem(
        diagnostic.psiElement,
        DefaultErrorMessages.render(diagnostic)

    )

}
