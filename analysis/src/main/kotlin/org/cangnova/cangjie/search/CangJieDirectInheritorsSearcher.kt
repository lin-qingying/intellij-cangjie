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

package org.cangnova.cangjie.search

import org.cangnova.cangjie.psi.CjTypeStatement
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.scope.fileScope
import org.cangnova.cangjie.stubindex.CangJieSuperClassIndex
import org.cangnova.cangjie.stubindex.CangJieTypeAliasByExpansionShortNameIndex

class CangJieDirectInheritorsSearcher:
    QueryExecutorBase<CjTypeStatement, DirectClassInheritorsSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: DirectClassInheritorsSearch.SearchParameters,
        consumer: Processor<in CjTypeStatement>
    ) {
        val originalParameters = queryParameters.getOriginalParameters()


        val baseClass = queryParameters.getClassToProcess()

        val baseClassName = baseClass.name ?: return

        val file =  baseClass.containingFile

        val originalScope = queryParameters.getScope()
        val scope = originalScope as? GlobalSearchScope ?: file.fileScope()

        val names = mutableSetOf(baseClassName)
        val project = file.project

        fun searchForTypeAliasesRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            CangJieTypeAliasByExpansionShortNameIndex[typeName, project, scope]
                .asSequence()
                .map { it.name }
                .filterNotNull()
                .filter { it !in names }
                .onEach { names.add(it) }
                .forEach(::searchForTypeAliasesRecursively)
        }

        searchForTypeAliasesRecursively(baseClassName)

        val noLibrarySourceScope = CangJieSourceFilterScope.projectFiles(scope, project)
        names.forEach { name ->
            ProgressManager.checkCanceled()
            CangJieSuperClassIndex[name, project, noLibrarySourceScope].asSequence()
                .map { candidate ->
                    ProgressManager.checkCanceled()
                    candidate
                }
//                .filter { candidate ->
//                    ProgressManager.checkCanceled()
//                    if (originalParameters != null &&
//                        MethodSignatureUtil.findMethodBySignature(candidate, originalParameters.method, false)?.unwrapped is CjClass) {
//                        //don't return classes with implementation by delegation
//                        false
//                    }
//                    else {
//                        candidate.isInheritor(baseClass, false) &&
//                                (originalParameters !is JavaOverridingMethodsSearcherFromCangJieParameters || MethodSignatureUtil.findMethodBySuperMethod(candidate, originalParameters.method, false) == null)
//                    }
//                }
                .forEach { candidate ->
                    ProgressManager.checkCanceled()
                    consumer.process(candidate)
                }
        }
    }
}
