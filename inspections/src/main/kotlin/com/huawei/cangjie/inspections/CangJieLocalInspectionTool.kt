package com.huawei.cangjie.inspections

import com.huawei.cangjie.idea.run.cjpm.isUnitTestMode
import com.huawei.cangjie.idea.run.cjpm.toolchain
import com.huawei.cangjie.psi.CjFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

abstract class CangJieLocalInspectionTool : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val file = session.file
        return if (file is CjFile && isApplicableTo(file)) {
            buildVisitor(holder, isOnTheFly)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    open val isSyntaxOnly: Boolean = false

    /**
     *仅语法检查适用于任何[CjFile]。
     *。
     *其他检查应仅分析符合以下条件的文件：
     *-属于工作空间
     *-包含在模块树中，即具有板条箱根
     *-属于已配置且有效的工具链的项目
     */
    private fun isApplicableTo(file: CjFile): Boolean {
        if (isSyntaxOnly) return true
//        if (!file.isDeeplyEnabledByCfg) return false

        if (isUnitTestMode) return true

        return file.project.toolchain != null
//        return file.cjpmWorkspace != null
//                && file.crateRoot != null
//                && file.project.toolchain != null
    }
}