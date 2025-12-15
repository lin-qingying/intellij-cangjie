/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.analysis.bridge

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.project.model.CjDependency

/**
 * 库依赖的 AnalysisContext 实现
 *
 * 适配外部库依赖为分析上下文。
 * 与源码模块不同，库依赖：
 * - 是只读的
 * - 通常只包含编译后的产物（.cjo 文件）
 * - 可能没有源码
 * - 不参与增量编译
 *
 * @property dependency 底层的 CjDependency 实例
 * @property project 所属的 IntelliJ 项目
 */
class CjLibraryAnalysisContext(
    private val dependency: CjDependency,
    override val project: Project
) : AnalysisContext {

    override val contextId: String = when (dependency) {
        is CjDependency.Library -> "${dependency.name}:${dependency.versionReq}"
        is CjDependency.Path -> "path:${dependency.path}"
        is CjDependency.Git -> "git:${dependency.url}"
        is CjDependency.Stdlib -> "stdlib:${dependency.versionReq}"
        is CjDependency.Binary -> "binary:${dependency.name}"
    }

    /**
     * 库的文件作用域
     *
     * **TODO**：需要实现库文件的实际查找逻辑
     * - 对于 Library 依赖：从缓存目录或仓库中查找
     * - 对于 Path 依赖：从本地路径查找
     * - 对于 Git 依赖：从 Git 缓存目录查找
     * - 对于 Stdlib：从 SDK 路径查找
     * - 对于 Binary：从指定路径查找 .cjo 文件
     */
    override val scope: GlobalSearchScope by lazy {
        // TODO: 实现库文件查找
        // 需要：
        // 1. 根据依赖类型确定文件位置
        // 2. 查找 .cjo 文件和相关源码（如果有）
        // 3. 创建包含这些文件的作用域

        // 临时实现：返回空作用域
        GlobalSearchScope.EMPTY_SCOPE
    }

    /**
     * 库的传递依赖
     *
     * **TODO**：需要从库的元数据中提取依赖信息
     * - 解析库的 metadata 文件
     * - 提取 dependencies 声明
     * - 递归创建依赖的 AnalysisContext
     */
    override val dependencies: List<AnalysisContext> by lazy {
        // TODO: 从库元数据中解析传递依赖

        // 临时实现：返回空列表
        emptyList()
    }

    /**
     * 库上下文始终不是源码上下文
     */
    override val isSourceContext: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjLibraryAnalysisContext) return false
        return dependency == other.dependency && project == other.project
    }

    override fun hashCode(): Int {
        var result = dependency.hashCode()
        result = 31 * result + project.hashCode()
        return result
    }

    override fun toString(): String {
        return "CjLibraryAnalysisContext(contextId='$contextId', type=${dependency::class.simpleName})"
    }
}

/**
 * CjDependency 扩展函数：转换为 AnalysisContext
 *
 * 将依赖转换为分析上下文。
 *
 * @param project 所属的 IntelliJ 项目
 * @return 适配后的 AnalysisContext
 */
fun CjDependency.asAnalysisContext(project: Project): AnalysisContext {
    return CjLibraryAnalysisContext(this, project)
}
