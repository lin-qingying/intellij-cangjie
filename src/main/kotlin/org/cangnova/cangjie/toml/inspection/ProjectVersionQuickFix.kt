/*
 * Copyright 2025 LinQingYing. and contributors.
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
package org.cangnova.cangjie.toml.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.messages.CJPMConfigInspectionBundle
import org.toml.lang.psi.TomlPsiFactory

/**
 * 对 `v1.0.0` 形式提供快速修复
 */
class RemoveVPrefixQuickFix: LocalQuickFix {
    override fun getFamilyName() = CJPMConfigInspectionBundle.message("quickfix.remove.prefix.desc")
    override fun getName() = familyName

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expr = descriptor.psiElement
        var txt = expr.text.substringAfter('\"').substringBefore('\"')
        txt = versionWithPrefixRegex.find(txt)!!.groupValues[1]
        val fixExpr = TomlPsiFactory(project).createLiteral("\"$txt\"")
        expr.replace(fixExpr)
    }
}

/**
 * 对 `1.0.0+1` 及 `1.0.0-alpha` 形式提供快速修复
 */
class SemVerQuickFix: LocalQuickFix{
    override fun getFamilyName() = CJPMConfigInspectionBundle.message("quickfix.remove.suffix.desc")
    override fun getName() = familyName

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expr = descriptor.psiElement
        var txt = expr.text.substringAfter('\"').substringBefore('\"')
        txt = semverRegex.find(txt)!!.groupValues[1]
        val fixExpr = TomlPsiFactory(project).createLiteral("\"$txt\"")
        expr.replace(fixExpr)
    }
}