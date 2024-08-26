package com.huawei.cangjie.ide.refactoring.move.changePackage

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.codeinsight.shorten.performDelayedRefactoringRequests
import com.huawei.cangjie.ide.refactoring.CangJieRefactoringSettings
import com.huawei.cangjie.ide.refactoring.move.*
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.quoteIfNeeded
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.utils.executeCommand
import com.huawei.cangjie.utils.runSynchronouslyWithProgress
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
