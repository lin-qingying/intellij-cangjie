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

package com.linqingying.cangjie.ide.refactoring.move.changePackage

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.codeinsight.shorten.performDelayedRefactoringRequests
import com.linqingying.cangjie.ide.refactoring.CangJieRefactoringSettings
import com.linqingying.cangjie.ide.refactoring.move.*
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.quoteIfNeeded
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.utils.executeCommand
import com.linqingying.cangjie.utils.runSynchronouslyWithProgress
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.refactoring.RefactoringBundle


class CangJieChangePackageRefactoring(val file: CjFile) {
    private val project = file.project

    fun run(newFqName: FqName) {
        val packageDirective = file.packageDirective ?: return
        val currentFqName = packageDirective.fqName

//        val declarationProcessor = MoveCangJieDeclarationsProcessor(
//            MoveDeclarationsDescriptor(
//                project = project,
//                moveSource = CangJieMoveSource(file),
//                moveTarget = CangJieMoveTarget.Directory(newFqName, file.containingDirectory!!.virtualFile),
//                delegate = CangJieMoveDeclarationDelegate.TopLevel,
//                searchInCommentsAndStrings = CangJieRefactoringSettings.instance.MOVE_SEARCH_IN_COMMENTS,
//                searchInNonCode = CangJieRefactoringSettings.instance.MOVE_SEARCH_FOR_TEXT,
//            )
//        )
//
//        val declarationUsages = project.runSynchronouslyWithProgress(RefactoringBundle.message("progress.text"), true) {
//            runReadAction {
//                declarationProcessor.findUsages().toList()
//            }
//        } ?: return
//        val changeInfo = MoveContainerChangeInfo(MoveContainerInfo.Package(currentFqName), MoveContainerInfo.Package(newFqName))
//        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(changeInfo)

        project.executeCommand(CangJieBundle.message("text.change.file.package.to.0", newFqName)) {
            runWriteAction {
                packageDirective.fqName = newFqName.quoteIfNeeded()
//       //         引用重构
//                postProcessMoveUsages(internalUsages)
//                performDelayedRefactoringRequests(project)
            }
//            declarationProcessor.execute(declarationUsages)
        }

    }
}
