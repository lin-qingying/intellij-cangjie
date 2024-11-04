package com.linqingying.cangjie.ide.inspections

import com.intellij.codeInsight.daemon.impl.actions.IntentionActionWithFixAllOption
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

@JvmDefaultWithCompatibility
interface CangJieUniversalQuickFix : IntentionActionWithFixAllOption, LocalQuickFix {
    override fun getName() = text

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        invoke(project, null, descriptor.psiElement?.containingFile)
    }
}
