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

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnchor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.Processor
import org.cangnova.cangjie.psi.CjTypeStatement
import java.util.concurrent.ConcurrentMap

/**
 * 仓颉类继承者搜索器
 *
 * 实现了类继承者的搜索逻辑，支持深度搜索和本地范围搜索优化。
 * 使用缓存机制提高性能，避免重复计算。
 */
class CangJieClassInheritorsSearcher :
    QueryExecutorBase<CjTypeStatement, ClassInheritorsSearch.SearchParameters>() {

    override fun processQuery(
        parameters: ClassInheritorsSearch.SearchParameters,
        consumer: Processor<in CjTypeStatement>
    ) {
        val baseClass = parameters.getClassToProcess()
        assert(parameters.isCheckDeep()) { "checkDeep must be true" }
        assert(parameters.isCheckInheritance()) { "checkInheritance must be true" }

        val progress = ProgressIndicatorProvider.getGlobalProgressIndicator()
        if (progress != null) {
            progress.pushState()
            val className = ReadAction.compute<String?, RuntimeException> { baseClass.name }
            progress.text = if (className != null) {
                CangJieIndexingBundle.message("psi.search.inheritors.of.class.progress", className)
            } else {
                CangJieIndexingBundle.message("psi.search.inheritors.progress")
            }
        }

        try {
            processInheritors(parameters, consumer)
        } finally {
            progress?.popState()
        }
    }

    companion object {
        /**
         * 处理继承者
         *
         * @param parameters 搜索参数
         * @param consumer 结果消费者
         */
        private fun processInheritors(
            parameters: ClassInheritorsSearch.SearchParameters,
            consumer: Processor<in CjTypeStatement>
        ) {
            val baseClass = parameters.getClassToProcess()
            val searchScope = parameters.getScope()
            val project = PsiUtilCore.getProjectInReadAction(baseClass)

            // 对于本地搜索范围的优化处理
            if (searchScope is LocalSearchScope) {
                processLocalScope(project, parameters, searchScope, baseClass, consumer)
                return
            }

            // EDT 优化：如果在 EDT 线程且不是深度搜索，直接使用 DirectClassInheritorsSearch
            if (!parameters.isCheckDeep() && ApplicationManager.getApplication().isDispatchThread) {
                // 优化：如果在 EDT 下请求一个继承者，为了提高延迟，不要费心计算和缓存所有继承者
                DirectClassInheritorsSearch.search(baseClass, searchScope).forEach(consumer)
                return
            }

            // 获取或计算子类（使用缓存）
            val cached = getOrComputeSubClasses(project, baseClass, searchScope, parameters)

            // 遍历缓存的子类
            for (subClass in cached) {
                ProgressManager.checkCanceled()
                if (subClass == null) {
                    // PsiAnchor 检索失败？
                    continue
                }
                if (ReadAction.compute<Boolean, RuntimeException> {
                        checkCandidate(subClass, parameters) && !consumer.process(subClass)
                    }) {
                    return
                }
            }
        }

        /**
         * 获取或计算子类（带缓存）
         *
         * @param project 项目
         * @param baseClass 基类
         * @param searchScopeForNonPhysical 非物理元素的搜索范围
         * @param parameters 搜索参数
         * @return 子类的可迭代集合
         */
        private fun getOrComputeSubClasses(
            project: Project,
            baseClass: CjTypeStatement,
            searchScopeForNonPhysical: SearchScope,
            parameters: ClassInheritorsSearch.SearchParameters
        ): Iterable<CjTypeStatement> {
            val caches = HighlightingCaches.getInstance(project)
            val map: ConcurrentMap<CjTypeStatement, Iterable<CjTypeStatement>> =
                if (parameters.isIncludeAnonymous()) {
                    caches.ALL_SUB_CLASSES
                } else {
                    caches.ALL_SUB_CLASSES_NO_ANONYMOUS
                }

            var cached = map[baseClass]
            if (cached == null) {
                // 返回子类的惰性集合。每次调用 next() 都会计算下一批子类。
                val converter: (PsiAnchor) -> CjTypeStatement? = { anchor ->
                    ReadAction.compute<CjTypeStatement?, RuntimeException> {
                        anchor.retrieve() as? CjTypeStatement
                    }
                }

                val applicableFilter: (CjTypeStatement?) -> Boolean = { it != null }

                // 对于非物理元素，完全忽略缓存，因为非物理元素创建得如此频繁/不可预测，
                // 以至于我无法弄清楚在这种情况下何时清除缓存
                val isPhysical = ReadAction.compute<Boolean, RuntimeException> { baseClass.isPhysical }
                val scopeToUse = if (isPhysical) GlobalSearchScope.allScope(project) else searchScopeForNonPhysical

                val generator = LazyConcurrentCollection.MoreElementsGenerator<PsiAnchor, CjTypeStatement> { candidate, processor ->
                    DirectClassInheritorsSearch.search(
                        object : DirectClassInheritorsSearch.SearchParameters(
                            candidate,
                            scopeToUse,
                            parameters.isIncludeAnonymous(),
                            true
                        ) {
                            override fun shouldSearchInLanguage(language: Language): Boolean {
                                return parameters.shouldSearchInLanguage(language)
                            }

                            override fun getOriginalParameters(): ClassInheritorsSearch.SearchParameters {
                                return parameters
                            }
                        }
                    ).allowParallelProcessing().forEach { subClass ->
                        ProgressManager.checkCanceled()
                        val pointer = ReadAction.compute<PsiAnchor, RuntimeException> { PsiAnchor.create(subClass) }
                        // 尽早将找到的结果附加到 subClasses，以允许其他等待的线程继续
                        processor(pointer)
                        true
                    }
                }

                val seed = ReadAction.compute<PsiAnchor, RuntimeException> { PsiAnchor.create(baseClass) }
                // 惰性集合：将底层队列存储为 PsiAnchors，通过运行直接继承者来生成新元素
                val computed = LazyConcurrentCollection(seed, converter, applicableFilter, generator)
                // 确保此方法的并发调用始终返回相同的集合，以避免昂贵的重复工作
                cached = if (isPhysical) ConcurrencyUtil.cacheOrGet(map, baseClass, computed) else computed
            }
            return cached
        }

        /**
         * 处理本地搜索范围
         *
         * 对于本地范围，枚举所有范围文件并检查其中是否有继承者，
         * 而不是遍历（可能很大的）类层次结构并按范围过滤掉几乎所有内容。
         *
         * @param project 项目
         * @param parameters 搜索参数
         * @param searchScope 本地搜索范围
         * @param baseClass 基类
         * @param consumer 结果消费者
         */
        private fun processLocalScope(
            project: Project,
            parameters: ClassInheritorsSearch.SearchParameters,
            searchScope: LocalSearchScope,
            baseClass: CjTypeStatement,
            consumer: Processor<in CjTypeStatement>
        ) {
            // 优化：对于本地范围，枚举所有范围文件并检查其中是否有继承者，
            // 而不是遍历（可能很大的）类层次结构并按范围过滤掉几乎所有内容。
            val virtualFiles = searchScope.virtualFiles

            val success = booleanArrayOf(true)
            if (virtualFiles.isEmpty()) {
                for (element in searchScope.scope) {
                    processFile(element.containingFile, consumer, parameters, baseClass, success)
                }
            }

            for (virtualFile in virtualFiles) {
                ProgressManager.checkCanceled()
                ApplicationManager.getApplication().runReadAction {
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    if (psiFile != null) {
                        processFile(psiFile, consumer, parameters, baseClass, success)
                    }
                }
            }
        }

        /**
         * 处理单个文件
         *
         * @param psiFile PSI 文件
         * @param consumer 结果消费者
         * @param parameters 搜索参数
         * @param baseClass 基类
         * @param success 成功标志数组
         */
        private fun processFile(
            psiFile: PsiFile,
            consumer: Processor<in CjTypeStatement>,
            parameters: ClassInheritorsSearch.SearchParameters,
            baseClass: CjTypeStatement,
            success: BooleanArray
        ) {
            // TODO: 实现文件内的继承者搜索
            // 需要遍历文件中的所有类并检查继承关系
            // psiFile.accept(new JavaRecursiveElementVisitor() {
            //     @Override
            //     public void visitClass(@NotNull CjTypeStatement candidate) {
            //         ProgressManager.checkCanceled();
            //         if (!success[0]) return;
            //         if (candidate.isInheritor(baseClass, true)
            //                 && checkCandidate(candidate, parameters)
            //                 && !consumer.process(candidate)) {
            //             success[0] = false;
            //             return;
            //         }
            //         super.visitClass(candidate);
            //     }
            // });
        }

        /**
         * 检查候选类是否符合搜索条件
         *
         * @param candidate 候选类
         * @param parameters 搜索参数
         * @return 如果符合条件则返回 true
         */
        private fun checkCandidate(
            candidate: CjTypeStatement,
            parameters: ClassInheritorsSearch.SearchParameters
        ): Boolean {
            val searchScope = parameters.getScope()
            ProgressManager.checkCanceled()

            if (!PsiSearchScopeUtil.isInScope(searchScope, candidate)) {
                return false
            }

            val name = candidate.name ?: return false
            return parameters.getNameCondition().value(name)
        }
    }
}
