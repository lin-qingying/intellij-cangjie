package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.psi.*
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

//        private fun processLightClassLocalImplementations(
//            psiClass: CjLightClass,
//            searchScope: LocalSearchScope,
//            consumer: Processor<PsiElement>
//        ): Boolean {
//            // workaround for IDEA optimization that uses Java PSI traversal to locate inheritors in local search scope
//            val globalScope = runReadAction {
//                val virtualFiles =searchScope.scope.mapTo(HashSet()) { it.containingFile.virtualFile }
//                GlobalSearchScope.filesScope(psiClass.project, virtualFiles)
//            }
//            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, globalScope, true)) { candidate ->
//                val candidateOrigin = candidate.unwrapped ?: candidate
//                val inScope = runReadAction { candidateOrigin in searchScope }
//                if (inScope) {
//                    consumer.process(candidate)
//                } else {
//                    true
//                }
//            }
//        }

//        private fun processFunctionImplementations(
//            function: CjFunction,
//            scope: SearchScope,
//            consumer: Processor<PsiElement>,
//        ): Boolean =
//            ReadAction.nonBlocking(Callable {
//                function.toPossiblyFakeLightMethods().firstOrNull()?.forEachImplementation(scope, consumer::process) ?: true
//            }).executeSynchronously()

//        private fun processPropertyImplementations(
//            declaration: CjNamedDeclaration,
//            scope: SearchScope,
//            consumer: Processor<PsiElement>
//        ): Boolean = runReadAction {
//            processPropertyImplementationsMethods(declaration , scope, consumer)
//        }

//        private fun processActualDeclarations(declaration: CjDeclaration, consumer: Processor<PsiElement>): Boolean = runReadAction {
//            if (!declaration.isExpectDeclaration()) true
//            else declaration.actualsForExpected().all(consumer::process)
//        }
//
//        fun processPropertyImplementationsMethods(
//            accessors: Iterable<PsiMethod>,
//            scope: SearchScope,
//            consumer: Processor<PsiElement>
//        ): Boolean = accessors.all { method ->
//            method.forEachOverridingMethod(scope) { implementation ->
//                if (isDelegated(implementation)) return@forEachOverridingMethod true
//
//                val elementToProcess = runReadAction {
//                    when (val mirrorElement = (implementation as? CjLightMethod)?.kotlinOrigin) {
//                        is CjProperty, is CjParameter -> mirrorElement
//                        is CjPropertyAccessor -> if (mirrorElement.parent is CjProperty) mirrorElement.parent else implementation
//                        else -> implementation
//                    }
//                }
//
//                consumer.process(elementToProcess)
//            }
//        }
    }
}


