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

package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.utils.safeAs
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiAnchor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.AnyPsiChangeListener
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.Processor
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.HashingStrategy
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

@Service(Service.Level.PROJECT)
class HighlightingCaches(private val project: Project) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): HighlightingCaches {
            return project.getService(HighlightingCaches::class.java)
        }


    }
    @JvmField
    val allCaches = ContainerUtil.createConcurrentList<MutableMap<*, *>>()

    init {
        project.messageBus.connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, object : AnyPsiChangeListener {
            override fun beforePsiChanged(isPhysical: Boolean) {
                if (isPhysical) {
                    allCaches.forEach { it.clear() }
                }
            }
        })
    }

    // baseClass -> list of direct subclasses
    val DIRECT_SUB_CLASSES: ConcurrentMap<CjTypeStatement, Array<CjTypeStatement>> = createWeakCache()
    // baseClass -> all sub classes transitively, including anonymous
    @JvmField
    val ALL_SUB_CLASSES: ConcurrentMap<CjTypeStatement, Iterable<CjTypeStatement>> = createWeakCache()
    // baseClass -> all sub classes transitively, excluding anonymous
    @JvmField

    val ALL_SUB_CLASSES_NO_ANONYMOUS: ConcurrentMap<CjTypeStatement, Iterable<CjTypeStatement>> = createWeakCache()
    // baseMethod -> all overriding methods
//    val OVERRIDING_METHODS: MutableMap<PsiMethod, Iterable<PsiMethod>> = createWeakCache()

    private fun <T, V> createWeakCache(): ConcurrentMap<T & Any, V & Any> {
        val map: ConcurrentMap<T & Any, V & Any> = CollectionFactory.createConcurrentWeakKeySoftValueMap(
            10, 0.7f, Runtime.getRuntime().availableProcessors(),
            HashingStrategy.canonical()
        )
        allCaches.add(map)
        return map
    }
}


class CangJieInnerClassInheritorsSearcher :
    QueryExecutorBase<CjTypeStatement, ClassInheritorsSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: ClassInheritorsSearch.SearchParameters,
        consumer: Processor<in CjTypeStatement>
    ) {
        val searchScope = queryParameters.scope.safeAs<LocalSearchScope>() ?: return
        val classToProcess = queryParameters.classToProcess
        val baseClass = classToProcess


        val progress = ProgressIndicatorProvider.getGlobalProgressIndicator()
        if (progress != null) {
            progress.pushState()
            progress.text =
                runReadAction { baseClass.name }?.let {
                    CangJieIndexingBundle.message(
                        "psi.search.inheritors.of.class.progress",
                        it
                    )
                }
                    ?: CangJieIndexingBundle.message("psi.search.inheritors.progress")
        }

        try {
            for (element in searchScope.scope) {
                checkCanceled()
                if (!runReadAction { processElementInScope(element, classToProcess, consumer) }) break
            }
        } finally {
            progress?.popState()
        }
    }

    private fun processElementInScope(
        element: PsiElement,
        classToProcess: CjTypeStatement,
        consumer: Processor<in CjTypeStatement>
    ): Boolean {
        val classesOrObjects = PsiTreeUtil.findChildrenOfType(element, CjTypeStatement::class.java)
        for (cjClassOrObject in classesOrObjects) {
            checkCanceled()
            if (cjClassOrObject.superTypeListEntries.isEmpty()) continue
//            cjClassOrObject.toLightClass()?.let {
//                if (it.isInheritor(classToProcess, true) && !consumer.process(it)) {
//                    return false
//                }
//            }
        }
        return true
    }
}
