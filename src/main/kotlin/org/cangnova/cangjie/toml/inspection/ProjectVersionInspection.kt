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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.messages.CJPMConfigInspectionBundle
import org.jetbrains.annotations.NonNls
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlVisitor

val versionRegex by lazy{
    Regex("^\\d+.\\d+.\\d+\$")
}

val versionWithPrefixRegex by lazy {
    Regex("^v(\\d+.\\d+.\\d+)\$")
}

val semverRegex by lazy{
    Regex("^((0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*))(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?\$")
}

class ProjectVersionInspection : LocalInspectionTool() {
    override fun isAvailableForFile(file: PsiFile) = StringUtil.equalsIgnoreCase("cjpm.toml",file.name)

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object:TomlVisitor(){
        override fun visitLiteral(element: TomlLiteral) {
            val kv = element.parent as? TomlKeyValue ?: return
            val key = kv.key
            if(key.text != "version") return

            val table = kv.parent as? TomlTable ?: return
            if(table.header.key?.text!="package") return

            if(table.parent !is TomlFile) return

            val txt = element.text.substringAfter('\"').substringBefore('\"')
            if(versionRegex.matches(txt)) return
            if(versionWithPrefixRegex.matches(txt)){
                holder.registerProblem(element, CJPMConfigInspectionBundle.message("problem.project.version.prefix.desc"),
                    RemoveVPrefixQuickFix())
            }else if(semverRegex.matches(txt)){
                holder.registerProblem(element,CJPMConfigInspectionBundle.message("problem.project.version.suffix.desc"), SemVerQuickFix())
            }

            holder.registerProblem(element,CJPMConfigInspectionBundle.message("problem.project.version.desc"))
        }
    }

    override fun getShortName() = "CJPMProjectConfigVersionValueCheck"
}