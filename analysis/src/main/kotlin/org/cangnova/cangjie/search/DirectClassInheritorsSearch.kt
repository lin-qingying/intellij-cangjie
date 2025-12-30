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
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ExtensibleQueryFactory
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.Query
import com.intellij.util.QueryExecutor
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 搜索给定类的<em>直接</em>继承者。
 *
 * 对于给定的层次结构:
 * ```
 *   class A {}
 *   class B extends A {}
 *   class C extends B {}
 * ```
 * 搜索 `A` 的继承者返回 `B`。
 *
 * 参见 [ClassInheritorsSearch] 以搜索所有继承者（包括传递继承者）。
 *
 * @see com.intellij.psi.util.InheritanceUtil
 */
class DirectClassInheritorsSearch private constructor() :
    ExtensibleQueryFactory<CjTypeStatement, DirectClassInheritorsSearch.SearchParameters>(EP_NAME) {

    /**
     * 直接类继承者搜索参数
     *
     * @property classToProcess 要搜索继承者的类
     * @property scope 搜索范围
     * @property includeAnonymous 是否包含匿名类
     * @property checkInheritance 是否检查继承关系
     */
    open class SearchParameters @JvmOverloads constructor(
        classToProcess: CjTypeStatement,
        scope: SearchScope,
        includeAnonymous: Boolean = true,
        checkInheritance: Boolean = true
    ) {
        private val classToProcess: CjTypeStatement = classToProcess
        private val scope: SearchScope = scope
        private val includeAnonymous: Boolean = includeAnonymous
        private val checkInheritance: Boolean = checkInheritance

        /**
         * 获取要处理的类
         */
        fun getClassToProcess(): CjTypeStatement = classToProcess

        /**
         * 获取搜索范围
         */
        fun getScope(): SearchScope = scope

        /**
         * 是否检查继承关系
         */
        fun isCheckInheritance(): Boolean = checkInheritance

        /**
         * 是否包含匿名类
         */
        fun includeAnonymous(): Boolean = includeAnonymous

        /**
         * 获取原始搜索参数（用于深度搜索时的回溯）
         */
        open fun getOriginalParameters(): ClassInheritorsSearch.SearchParameters? = null

        /**
         * 是否应该在给定语言中搜索
         *
         * @param language 要检查的语言
         * @return 如果应该搜索则返回 true
         */
        open fun shouldSearchInLanguage(language: Language): Boolean = true
    }

    companion object {
        /**
         * 扩展点名称
         */
        val EP_NAME: ExtensionPointName<QueryExecutor<CjTypeStatement, SearchParameters>> =
            ExtensionPointName.create("org.cangnova.cangjie.search.directClassInheritorsSearch")

        /**
         * 单例实例
         */
        @JvmField
        val INSTANCE = DirectClassInheritorsSearch()

        /**
         * 搜索给定类的直接继承者（使用全局作用域）
         *
         * @param aClass 要搜索继承者的类
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement): Query<CjTypeStatement> {
            return search(aClass, GlobalSearchScope.allScope(PsiUtilCore.getProjectInReadAction(aClass)))
        }

        /**
         * 搜索给定类的直接继承者（在指定作用域内）
         *
         * @param aClass 要搜索继承者的类
         * @param scope 搜索范围
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement, scope: SearchScope): Query<CjTypeStatement> {
            return search(aClass, scope, true)
        }

        /**
         * 搜索给定类的直接继承者
         *
         * @param aClass 要搜索继承者的类
         * @param scope 搜索范围
         * @param includeAnonymous 是否包含匿名类
         * @return 查询结果
         */
        @JvmStatic
        fun search(aClass: CjTypeStatement, scope: SearchScope, includeAnonymous: Boolean): Query<CjTypeStatement> {
            return search(SearchParameters(aClass, scope, includeAnonymous, true))
        }

        /**
         * 使用搜索参数搜索直接继承者
         *
         * @param parameters 搜索参数
         * @return 查询结果
         */
        @JvmStatic
        fun search(parameters: SearchParameters): Query<CjTypeStatement> {
            return INSTANCE.createUniqueResultsQuery(parameters)
        }
    }
}
