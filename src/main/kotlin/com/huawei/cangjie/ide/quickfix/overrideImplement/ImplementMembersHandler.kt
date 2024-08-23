package com.huawei.cangjie.ide.quickfix.overrideImplement

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.resolve.OverrideResolver
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

open class ImplementMembersHandler : GenerateMembersHandler(true), IntentionAction {
    override fun collectMembersToGenerate(
        descriptor: ClassDescriptor,
        project: Project
    ): Collection<OverrideMemberChooserObject> {
        return OverrideResolver.getMissingImplementations(descriptor)
            .map { OverrideMemberChooserObject.create(project, it, it, BodyType.FromTemplate) }
    }

    override fun getChooserTitle() = CangJieBundle.message("implement.members.handler.title")

    override fun getNoMembersFoundHint() = CangJieBundle.message("implement.members.handler.no.members.hint")

    override fun getText() = familyName
    override fun getFamilyName() = CangJieBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)
}
