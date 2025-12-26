/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.quickfix.overrideImplement

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.resolve.OverrideResolver
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.messages.CangJieCodeInsightBundle

open class ImplementMembersHandler : GenerateMembersHandler(true), IntentionAction {
    override fun collectMembersToGenerate(
        descriptor: ClassDescriptor,
        project: Project
    ): Collection<OverrideMemberChooserObject> {
        return OverrideResolver.getMissingImplementations(descriptor)
            .map { OverrideMemberChooserObject.create(project, it, it, BodyType.FromTemplate) }
    }

    override fun getChooserTitle() = CangJieCodeInsightBundle.message("implement.members.handler.title")

    override fun getNoMembersFoundHint() = CangJieCodeInsightBundle.message("implement.members.handler.no.members.hint")

    override fun getText() = familyName
    override fun getFamilyName() = CangJieCodeInsightBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile) = isValidFor(editor, file)
}
