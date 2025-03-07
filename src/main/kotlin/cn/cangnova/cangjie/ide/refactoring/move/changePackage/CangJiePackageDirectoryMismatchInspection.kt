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

package cn.cangnova.cangjie.ide.refactoring.move.changePackage

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import cn.cangnova.cangjie.ide.refactoring.hasIdentifiersOnly
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjPackageDirective
import cn.cangnova.cangjie.psi.packageDirectiveVisitor
import cn.cangnova.cangjie.psi.psiUtil.startOffset
import cn.cangnova.cangjie.utils.getFqNameByDirectory
import cn.cangnova.cangjie.utils.packageMatchesDirectoryOrImplicit
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.SingleFileSourcesTracker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

val PsiFile.isInjectedFragment: Boolean
    get() = InjectedLanguageManager.getInstance(project).isInjectedFragment(this)


class CangJiePackageDirectoryMismatchInspection : AbstractCangJieInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        packageDirectiveVisitor(fun(directive: CjPackageDirective) {
            val file = directive.getContainingCjFile()
            if (file.textLength == 0 || file.isInjectedFragment || file.packageMatchesDirectoryOrImplicit()) return

            val fixes = mutableListOf<LocalQuickFix>()
            val qualifiedName = directive.qualifiedName
            val dirName = if (qualifiedName.isEmpty())
                CangJieBundle.message("fix.move.file.to.package.dir.name.text")
            else
                "'${qualifiedName.replace('.', '/')}'"

            val singleFileSourcesTracker = SingleFileSourcesTracker.getInstance(file.project)
            val isSingleFileSource = singleFileSourcesTracker.isSingleFileSource(file.virtualFile)
            if (!isSingleFileSource) fixes += MoveFileToPackageFix(dirName)
            val fqNameByDirectory = file.getFqNameByDirectory()
            when {
                fqNameByDirectory.isRoot ->
                    fixes += ChangePackageFix(
                        CangJieBundle.message("fix.move.file.to.package.dir.name.text"),
                        fqNameByDirectory
                    )

                fqNameByDirectory.hasIdentifiersOnly() ->
                    fixes += ChangePackageFix("'${fqNameByDirectory.asString()}'", fqNameByDirectory)
            }

            val textRange =
                if (directive.textLength != 0) directive.textRange else file.declarations.firstOrNull()?.let {
                    TextRange.from(it.startOffset, 1)
                }
            holder.registerProblem(
                file,
                textRange,
                CangJieBundle.message("text.package.directive.dont.match.file.location"),
                *fixes.toTypedArray()
            )
        })

    private class MoveFileToPackageFix(val dirName: String) : LocalQuickFix {
        override fun getFamilyName() = CangJieBundle.message("fix.move.file.to.package.family")

        override fun getName() = CangJieBundle.message("fix.move.file.to.package.text", dirName)

        override fun startInWriteAction() = false

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
//            val file = descriptor.psiElement as? CjFile ?: return
//            val directive = file.packageDirective ?: return
//
//            val sourceRoots = file.module?.findExistingNonGeneratedCangJieSourceRootFiles() ?: getSuitableDestinationSourceRoots(project)
//            val packageWrapper = PackageWrapper(PsiManager.getInstance(project), directive.qualifiedName)
//            val fileToMove = directive.containingFile
//            val chosenRoot =
//                sourceRoots.singleOrNull()
//                    ?: CommonMoveClassesOrPackagesUtil.chooseSourceRoot(packageWrapper, sourceRoots, fileToMove.containingDirectory)
//                    ?: return
//
//            val targetDirFactory = AutocreatingSingleSourceRootMoveDestination(packageWrapper, chosenRoot)
//            targetDirFactory.verify(fileToMove)?.let {
//                Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
//                return
//            }
//            val targetDirectory = runWriteAction {
//                targetDirFactory.getTargetDirectory(fileToMove)
//            } ?: return
//
//            RefactoringMessageUtil.checkCanCreateFile(targetDirectory, file.name)?.let {
//                Messages.showMessageDialog(project, it, CommonBundle.getErrorTitle(), Messages.getErrorIcon())
//                return
//            }
//
//            runWriteAction {
//                MoveFilesOrDirectoriesUtil.doMoveFile(file, targetDirectory)
//            }
        }
    }

    private class ChangePackageFix(val packageName: String, val packageFqName: FqName) : LocalQuickFix {
        override fun getFamilyName() = CangJieBundle.message("fix.change.package.family")

        override fun getName() = CangJieBundle.message("fix.change.package.text", packageName)

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement as? CjFile ?: return
            CangJieChangePackageRefactoring(file).run(packageFqName)
        }

        override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
            val file = previewDescriptor.psiElement as? CjFile ?: return IntentionPreviewInfo.EMPTY
            val packageDirective = file.packageDirective ?: return IntentionPreviewInfo.EMPTY
            packageDirective.fqName = packageFqName
            return IntentionPreviewInfo.DIFF
        }
    }
}
