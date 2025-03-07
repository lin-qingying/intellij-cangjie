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

package cn.cangnova.cangjie.ide.codeinsight.shorten

import cn.cangnova.cangjie.ide.ShortenReferences
import cn.cangnova.cangjie.highlighter.unwrapped
import cn.cangnova.cangjie.ide.imports.ImportInsertHelper
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.UserDataProperty
import cn.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import java.util.ArrayList
import java.util.LinkedHashSet


interface DelayedRefactoringRequest
class ShorteningRequest(val pointer: SmartPsiElementPointer<CjElement>, val options: ShortenReferences.Options) : DelayedRefactoringRequest

class ImportRequest(
    val elementToImportPointer: SmartPsiElementPointer<PsiElement>,
    val filePointer: SmartPsiElementPointer<CjFile>
) : DelayedRefactoringRequest

private var Project.delayedRefactoringRequests: MutableSet<DelayedRefactoringRequest>?
        by UserDataProperty(Key.create("DELAYED_REFACTORING_REQUESTS"))

fun addDelayedImportRequest(elementToImport: PsiElement, file: CjFile) {
    assert(ApplicationManager.getApplication()!!.isWriteAccessAllowed) { "Write access needed" }
    file.project.getOrCreateRefactoringRequests() += ImportRequest(
        elementToImport.createSmartPointer(),
        file.createSmartPointer()
    )
}

fun CjElement.addToShorteningWaitSet(options: ShortenReferences.Options = ShortenReferences.Options.DEFAULT) {
    assert(ApplicationManager.getApplication()!!.isWriteAccessAllowed) { "Write access needed" }
    val project = project
    val elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
    project.getOrCreateRefactoringRequests().add(ShorteningRequest(elementPointer, options))
}

private fun Project.getOrCreateRefactoringRequests(): MutableSet<DelayedRefactoringRequest> {
    var requests = delayedRefactoringRequests
    if (requests == null) {
        requests = LinkedHashSet()
        delayedRefactoringRequests = requests
    }

    return requests
}
fun performDelayedRefactoringRequests(project: Project) {
    project.delayedRefactoringRequests?.let { requests ->
        project.delayedRefactoringRequests = null
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val shorteningRequests = ArrayList<ShorteningRequest>()
        val importRequests = ArrayList<ImportRequest>()
        requests.forEach {
            when (it) {
                is ShorteningRequest -> shorteningRequests += it
                is ImportRequest -> importRequests += it
            }
        }

        val elementToOptions = shorteningRequests.mapNotNull { req -> req.pointer.element?.let { it to req.options } }.toMap()
        val elements = elementToOptions.keys
        //TODO: this is not correct because it should not shorten deep into the elements!
        ShortenReferences { elementToOptions[it] ?: ShortenReferences.Options.DEFAULT }.process(elements)

        val importInsertHelper = ImportInsertHelper.getInstance(project)

        for ((file, requestsForFile) in importRequests.groupBy { it.filePointer.element }) {
            if (file == null) continue

            for (requestForFile in requestsForFile) {
                val elementToImport = requestForFile.elementToImportPointer.element?.unwrapped ?: continue
                val descriptorToImport = when (elementToImport) {
                    is CjDeclaration -> elementToImport.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)

                    else -> null
                } ?: continue
                importInsertHelper.importDescriptor(file, descriptorToImport)
            }
        }
    }
}
