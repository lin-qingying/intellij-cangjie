/*
 * Copyright 2026 LinQingYing. and contributors.
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

import org.cangnova.cangjie.descriptors.DeclarationProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
/**
 * 包成员声明提供者接口，继承自 [DeclarationProvider]。
 *
 * 负责提供指定包（Package）内的成员声明信息，包括：
 * - 子包的枚举与过滤
 * - 包内文件的获取
 * - 文件归属关系的判断
 *
 * 该接口通常用于编译器前端或 IDE 索引阶段，
 * 用于遍历和查询包结构中的声明信息。
 */
interface PackageMemberDeclarationProvider : DeclarationProvider {

    /**
     * 获取所有已声明的子包集合。
     *
     * @param nameFilter 用于过滤子包名称的断言函数，
     *                   接收一个 [Name] 参数，返回 true 表示保留该包，false 表示过滤掉。
     * @return 满足过滤条件的子包完全限定名（[FqName]）集合。
     */
    fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName>

    /**
     * 获取当前包中包含的所有文件集合。
     *
     * @return 属于该包的 [CjFile] 文件集合。
     */
    fun getPackageFiles(): Collection<CjFile>

    /**
     * 判断指定文件是否属于当前包。
     *
     * @param file 待检测的 [CjFile] 文件对象。
     * @return 若该文件包含在当前包中则返回 true，否则返回 false。
     */
    fun containsFile(file: CjFile): Boolean
}
