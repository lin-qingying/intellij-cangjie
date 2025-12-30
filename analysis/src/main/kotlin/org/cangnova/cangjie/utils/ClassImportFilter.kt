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

package org.cangnova.cangjie.utils

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * 类导入过滤器
 *
 * 用于控制哪些类可以被自动导入。
 * 在某些操作（如代码补全、快速修复）触发自动导入时，
 * 此过滤器可以阻止某些不应该导入的类被自动添加到导入列表中。
 *
 * **使用场景**：
 * - 阻止导入内部 API 类（internal 可见性）
 * - 阻止导入实验性 API（experimental 标记）
 * - 阻止导入已废弃的类
 * - 根据项目配置自定义导入策略
 *
 * **扩展机制**：
 * 实现此接口并注册到扩展点 `org.cangnova.cangjie.classImportFilter`，
 * 可以定制自己的导入过滤策略。
 *
 * **SAM 接口**：
 * 使用 `fun interface` 定义，可以通过 lambda 简洁地创建实例：
 * ```kotlin
 * ClassImportFilter { classInfo, file -> classInfo.visibility == Visibility.Public }
 * ```
 */
fun interface ClassImportFilter {
    /**
     * 类信息数据类
     *
     * 封装了类的关键信息，用于判断是否允许导入。
     * 使用数据类的优势是：
     * - API 稳定：添加新字段时无需修改接口
     * - 结构化：信息组织清晰
     * - 可扩展：未来可以添加更多元数据而不破坏兼容性
     *
     * @property fqName 类的全限定名（如 `std.collection.ArrayList`）
     * @property classKind 类的种类（类、接口、struct、枚举等）
     * @property modality 类的修饰符（open、sealed、abstract 等）
     * @property visibility 类的可见性（public、private、internal 等）
     * @property isNested 是否为嵌套类（定义在其他类内部）
     */
    data class ClassInfo(val fqName: FqName, val classKind: ClassKind, val modality: Modality, val visibility: Visibility, val isNested: Boolean)

    /**
     * 判断是否允许导入此类
     *
     * 过滤器的核心方法。根据类的信息和当前文件的上下文，
     * 决定是否应该将此类自动导入。
     *
     * **典型的过滤逻辑**：
     * - 仅允许导入 public 类：`classInfo.visibility == Visibility.Public`
     * - 阻止导入抽象类：`classInfo.modality != Modality.Abstract`
     * - 阻止导入嵌套类：`!classInfo.isNested`
     * - 根据包名过滤：`classInfo.fqName.parent().asString() != "internal"`
     *
     * @param classInfo 类的信息
     * @param contextFile 触发导入的当前文件（用于上下文相关的过滤）
     * @return Boolean true 表示允许导入，false 表示阻止导入
     */
    fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) : Boolean

    companion object {
        /**
         * 类导入过滤器的扩展点
         *
         * 所有注册到此扩展点的过滤器都会参与导入决策。
         * 如果任何一个过滤器返回 false，则阻止导入。
         */
        val EP_NAME = ExtensionPointName.create<ClassImportFilter>("org.cangnova.cangjie.classImportFilter")

        /**
         * 检查所有注册的过滤器是否都允许导入
         *
         * 使用 "全部同意" 策略：所有过滤器都返回 true 时，才允许导入。
         * 这确保了任何一个过滤器都可以否决导入操作。
         *
         * @param classInfo 类的信息
         * @param contextFile 当前文件
         * @return Boolean true 表示所有过滤器都允许导入
         */
        fun allowClassImport(classInfo: ClassInfo, contextFile: CjFile) =
            EP_NAME.extensions.all { it.allowClassImport(classInfo, contextFile) }
    }
}