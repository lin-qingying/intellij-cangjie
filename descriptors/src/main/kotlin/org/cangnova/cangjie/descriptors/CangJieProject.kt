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

package org.cangnova.cangjie.descriptors

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.DefaultBuiltIns

/**
 * 仓颉项目描述符接口
 *
 * `CangJieProject` 是整个仓颉项目的最顶层根对象，代表一个完整的仓颉项目。
 * 一个仓颉项目可以包含多个模块（Module），每个模块由 [ModuleDescriptor] 描述。
 *
 * ## 设计目的
 *
 * 1. **层次结构**: 建立清晰的项目层次：CangJieProject -> ModuleDescriptor -> Package -> Class
 * 2. **模块管理**: 统一管理项目中的所有模块，处理模块间的依赖关系
 * 3. **项目级配置**: 提供项目级别的配置和能力，如全局设置、项目范围的缓存等
 * 4. **跨模块操作**: 支持跨模块的查找、解析和类型检查
 *
 * ## 使用场景
 *
 * - 构建整个仓颉项目的描述符树
 * - 管理多模块项目中的模块依赖关系
 * - 提供项目级别的类型查找和解析服务
 * - 集成 IDE 项目结构
 *
 * ## 示例
 *
 * ```kotlin
 * // 创建仓颉项目
 * val cangJieProject = CangJieProjectImpl(
 *     projectName = Name.identifier("MyProject"),
 *     storageManager = storageManager
 * )
 *
 * // 添加模块
 * val mainModule = ModuleDescriptorImpl(...)
 * cangJieProject.addModule(mainModule)
 *
 * // 查找模块
 * val module = cangJieProject.getModule(Name.identifier("main"))
 * ```
 *
 * @see ModuleDescriptor
 * @see PackageViewDescriptor
 */
interface CangJieProject:DeclarationDescriptor {

    /**
     * 项目名称
     */
    override val name: Name
    /**
     * 项目的 BuiltIns，基于 SDK 创建
     * 整个项目共享同一个 BuiltIns 实例
     */
    val builtIns: CangJieBuiltIns
    /**
     * 关联的 IntelliJ 项目实例
     * 用于集成 IDE 功能，如文件系统访问、索引服务等
     */
    val project: Project

    /**
     * 获取项目中的所有模块
     * @return 项目中所有模块的列表
     */
    val modules: List<ModuleDescriptor>

    /**
     * 根据名称获取模块
     * @param moduleName 模块名称
     * @return 对应的模块描述符，如果不存在则返回 null
     */
    fun getModule(moduleName: Name): ModuleDescriptor?

    /**
     * 项目是否有效
     */
    val isValid: Boolean

    /**
     * 断言项目的有效性
     * 如果项目无效则抛出异常
     */
    fun assertValid()

    companion object {
        val ERROR = object : CangJieProject {
            override val name: Name
                get() = Name.ERROR_NAME
            override val builtIns: CangJieBuiltIns
                get() = DefaultBuiltIns
            override val project: Project
                get() = ProjectManager.getInstance().defaultProject
            override val modules: List<ModuleDescriptor>
                get() = emptyList()

            override fun getModule(moduleName: Name): ModuleDescriptor? {
                return modules.firstOrNull { it.name == moduleName }
            }

            override val isValid: Boolean
                get() = false

            override fun assertValid() {

            }

            override val original: DeclarationDescriptor
                get() = this
            override val containingDeclaration: DeclarationDescriptor?
                get() = null

            override fun <R, D> accept(
                visitor: DeclarationDescriptorVisitor<R, D>,
                data: D?
            ): R? {
              return null
            }

            override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

            }

        }
    }
}
