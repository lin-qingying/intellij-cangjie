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

package cn.cangnova.cangjie.ide.search

import cn.cangnova.cangjie.psi.*
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.containers.ContainerUtil
import java.util.concurrent.Callable

class CangJieDefinitionsSearcher : QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {

    override fun execute(queryParameters: DefinitionsScopedSearch.SearchParameters, consumer: Processor<in PsiElement>): Boolean {
        val processor = skipDelegatedMethodsConsumer(consumer)
        val element = queryParameters.element
        val scope = queryParameters.scope

        return when (element) {
            is CjTypeStatement -> {
//                val isExpectEnum = runReadAction { element.isEnum() && element.isExpectDeclaration() }
//                if (isExpectEnum) {
//                    processActualDeclarations(element, processor)
//                } else {
                    processClassImplementations(element, processor) /*&& processActualDeclarations(element, processor)*/
//                }
            }



//            is CjLightClass -> {
//                val useScope = runReadAction { element.useScope }
//                if (useScope is LocalSearchScope)
//                    processLightClassLocalImplementations(element, useScope, processor)
//                else
//                    true
//            }

//            is CjNamedFunction, is CjSecondaryConstructor -> {
//                processFunctionImplementations(element as CjFunction, scope, processor) && processActualDeclarations(element, processor)
//            }

//            is CjProperty -> {
//                processPropertyImplementations(element, scope, processor) && processActualDeclarations(element, processor)
//            }
//
//            is CjParameter -> {
//                if (isFieldParameter(element)) {
//                    processPropertyImplementations(element, scope, processor) && processActualDeclarations(element, processor)
//                } else {
//                    true
//                }
//            }

            else -> true
        }
    }

    companion object {

        private fun skipDelegatedMethodsConsumer(baseConsumer: Processor<in PsiElement>): Processor<PsiElement> = Processor { element ->
//            if (isDelegated(element)) {
//                return@Processor true
//            }

            baseConsumer.process(element)
        }

//        private fun isDelegated(element: PsiElement): Boolean = element is CjLightMethod && element.isDelegated

        private fun isFieldParameter(parameter: CjParameter): Boolean = runReadAction {
            CjPsiUtil.getClassIfParameterIsProperty(parameter) != null
        }

        private fun processClassImplementations(cclass: CjTypeStatement, consumer: Processor<PsiElement>): Boolean {

//            val searchScope = runReadAction { cclass.useScope }
//            if (searchScope is LocalSearchScope) {
//                return processLightClassLocalImplementations(cclass, searchScope, consumer)
//            }

            return runReadAction { ContainerUtil.process(ClassInheritorsSearch.search(cclass, true), consumer) }
        }

    }
}


