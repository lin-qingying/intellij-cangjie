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
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ExtensibleQueryFactory
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.*
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 搜索给定类的继承者。
 *
 * 对于给定的层次结构:
 * ```
 *   class A {}
 *   class B extends A {}
 *   class C extends B {}
 * ```
 * 使用默认的 `checkDeep=true` 搜索 `A` 的继承者会返回 `B` 和 `C`。
 *
 * 使用 `checkDeep=false` 或 [DirectClassInheritorsSearch] 仅搜索直接继承者。
 *
 * @see com.intellij.psi.util.InheritanceUtil
 */
class ClassInheritorsSearch private constructor() :
    ExtensibleQueryFactory<CjTypeStatement, ClassInheritorsSearch.SearchParameters>(EP_NAME) {

    /**
     * 类继承者搜索参数
     *
     * @property myClass 要搜索继承者的类
     * @property myScope 搜索范围
     * @property myCheckDeep 是否深度搜索（包括传递继承者）
     * @property myCheckInheritance 是否检查继承关系
     * @property myIncludeAnonymous 是否包含匿名类
     * @property myNameCondition 名称过滤条件
     * @property myProject 项目实例
     */
    class SearchParameters(
        private val myClass: CjTypeStatement,
        private val myScope: SearchScope,
        private val myCheckDeep: Boolean,
        private val myCheckInheritance: Boolean,
        private val myIncludeAnonymous: Boolean,
        private val myNameCondition: Condition<in String> = Conditions.alwaysTrue()
    ) : QueryParameters {

        private val myProject: Project = PsiUtilCore.getProjectInReadAction(myClass)

        /**
         * 次级构造函数（无名称条件）
         */
        constructor(
            aClass: CjTypeStatement,
            scope: SearchScope,
            checkDeep: Boolean,
            checkInheritance: Boolean,
            includeAnonymous: Boolean
        ) : this(aClass, scope, checkDeep, checkInheritance, includeAnonymous, Conditions.alwaysTrue())

        init {
            assert(myCheckInheritance) { "checkInheritance must be true" }
        }

        /**
         * 获取要处理的类
         */
        fun getClassToProcess(): CjTypeStatement = myClass

        override fun getProject(): Project = myProject

        override fun isQueryValid(): Boolean = myClass.isValid

        /**
         * 获取名称过滤条件
         */
        fun getNameCondition(): Condition<in String> = myNameCondition

        /**
         * 是否深度搜索
         */
        fun isCheckDeep(): Boolean = myCheckDeep

        /**
         * 获取搜索范围
         */
        fun getScope(): SearchScope = myScope

        /**
         * 是否检查继承关系
         */
        fun isCheckInheritance(): Boolean = myCheckInheritance

        /**
         * 是否包含匿名类
         */
        fun isIncludeAnonymous(): Boolean = myIncludeAnonymous

        /**
         * 是否应该在给定语言中搜索
         *
         * @param language 要检查的语言
         * @return 如果应该搜索则返回 true
         */
        open fun shouldSearchInLanguage(language: Language): Boolean = true

        override fun toString(): String {
            return buildString {
                append("'${myClass.fqName}'")
                append(" scope=$myScope")
                if (myCheckDeep) append(" (deep)")
                if (myCheckInheritance) append(" (check inheritance)")
                if (myIncludeAnonymous) append(" (anonymous)")
                if (myNameCondition != Conditions.alwaysTrue<String>()) {
                    append(" condition: $myNameCondition")
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false

            other as SearchParameters

            if (myCheckDeep != other.myCheckDeep) return false
            if (myCheckInheritance != other.myCheckInheritance) return false
            if (myIncludeAnonymous != other.myIncludeAnonymous) return false
            if (myClass != other.myClass) return false
            if (myScope != other.myScope) return false
            return myNameCondition == other.myNameCondition
        }

        override fun hashCode(): Int {
            var result = myClass.hashCode()
            result = 31 * result + myScope.hashCode()
            result = 31 * result + myCheckDeep.hashCode()
            result = 31 * result + myCheckInheritance.hashCode()
            result = 31 * result + myIncludeAnonymous.hashCode()
            result = 31 * result + myNameCondition.hashCode()
            return result
        }
    }

    companion object {
        /**
         * 扩展点名称
         */
        val EP_NAME: ExtensionPointName<QueryExecutor<CjTypeStatement, SearchParameters>> =
            ExtensionPointName.create("org.cangnova.cangjie.search.classInheritorsSearch")

        /**
         * 单例实例
         */
        @JvmField
        val INSTANCE = ClassInheritorsSearch()

        /**
         * 搜索给定类的继承者
         *
         * @param aClass 要搜索继承者的类
         * @param scope 搜索范围
         * @param checkDeep 是否深度搜索
         * @param checkInheritance 是否检查继承关系
         * @param includeAnonymous 是否包含匿名类
         * @return 查询结果
         */
        @JvmStatic
        fun search(
            aClass: CjTypeStatement,
            scope: SearchScope,
            checkDeep: Boolean,
            checkInheritance: Boolean,
            includeAnonymous: Boolean
        ): Query<CjTypeStatement> {
            return search(SearchParameters(aClass, scope, checkDeep, checkInheritance, includeAnonymous))
        }

        /**
         * 使用搜索参数搜索继承者
         *
         * @param parameters 搜索参数
         * @return 查询结果
         */
        @JvmStatic
        fun search(parameters: SearchParameters): Query<CjTypeStatement> {
            // 如果不是深度搜索，直接使用 DirectClassInheritorsSearch
            if (!parameters.isCheckDeep()) {
                var directQuery = DirectClassInheritorsSearch.search(
                    object : DirectClassInheritorsSearch.SearchParameters(
                        parameters.getClassToProcess(),
                        parameters.getScope(),
                        parameters.isIncludeAnonymous(),
                        true
                    ) {
                        override fun shouldSearchInLanguage(language: Language): Boolean {
                            return parameters.shouldSearchInLanguage(language)
                        }

                        override fun getOriginalParameters(): SearchParameters {
                            return parameters
                        }
                    }
                )

                // 应用名称过滤条件
                if (parameters.getNameCondition() != Conditions.alwaysTrue<String>()) {
                    directQuery = FilteredQuery(directQuery) { cjTypeStatement ->
                        com.intellij.openapi.application.ReadAction.compute<Boolean, RuntimeException> {
                            parameters.getNameCondition().value(cjTypeStatement.name)
                        }
                    }
                }
                return AbstractQuery.wrapInReadAction(directQuery)
            }

            // 深度搜索：创建唯一结果查询
            return INSTANCE.createUniqueResultsQuery(parameters) { cjTypeStatement ->
                com.intellij.openapi.application.ReadAction.compute<SmartPsiElementPointer<CjTypeStatement>, RuntimeException> {
                    SmartPointerManager.getInstance(cjTypeStatement.project)
                        .createSmartPsiElementPointer(cjTypeStatement)
                }
            }
        }

        /**
         * 搜索给定类的继承者
         *
         * @param aClass 要搜索继承者的类
         * @param scope 搜索范围
         * @param checkDeep 是否深度搜索
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement, scope: SearchScope, checkDeep: Boolean): Query<CjTypeStatement> {
            return search(aClass, scope, checkDeep, true, true)
        }

        /**
         * 搜索给定类的继承者（自动确定搜索范围）
         *
         * @param aClass 要搜索继承者的类
         * @param checkDeep 是否深度搜索
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement, checkDeep: Boolean): Query<CjTypeStatement> {
            return search(
                aClass,
                com.intellij.openapi.application.ReadAction.compute<SearchScope, RuntimeException> {
                    if (!aClass.isValid) {
                        throw ProcessCanceledException()
                    }
                    val file = aClass.containingFile
                    PsiSearchHelper.getInstance(aClass.project).getUseScope(file ?: aClass)
                },
                checkDeep
            )
        }

        /**
         * 搜索给定类的所有继承者（深度搜索，自动确定搜索范围）
         *
         * @param aClass 要搜索继承者的类
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement): Query<CjTypeStatement> {
            return search(aClass, true)
        }
    }
}
