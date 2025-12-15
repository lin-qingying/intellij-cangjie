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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageViewDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.PACKAGE_ACCESS_VIOLATION
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.binding.BindingTrace

/**
 * 包解析器类，用于检查和解析包级别的访问控制
 *
 * @param trace 绑定追踪对象，用于报告错误和跟踪绑定信息
 * @param moduleDescriptor 模块描述符对象，包含模块级别的描述信息
 */
class PackagerResolver(
    private val trace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor
) {
    /**
     * 检查给定上下文中文件的包级别访问控制
     *
     * @param c 顶层分析上下文，包含待分析的文件集合
     */
    fun check(c: TopDownAnalysisContext) {
        // 获取上下文中第一个文件，如果存在，则进行包级别检查
        c.files.firstOrNull()?.let {
            // 获取文件对应的包视图
            moduleDescriptor.getPackage(it.packageFqName)

            // 调用函数检查包级别
            checkPackageLevel(it)
        }
    }

    /**
     * 检查文件的包级别访问控制是否违反了访问控制规则
     *
     * @param file 要检查的文件对象
     */
    private fun checkPackageLevel(file: CjFile) {
        // 获取文件对应的包视图
        val packageView = moduleDescriptor.getPackage(file.packageFqName)
        // 获取当前包的访问控制级别
        val currentLevel = packageView.visibility.toAccessControlLevel()
        // 如果当前包的访问控制级别为0，则无需检查
        if (currentLevel == 0) {
            return
        }
        // 如果包名是模块名，则无需检查
        if (packageView.fqName.isModuleName) {
            return
        }
        // 获取父包的访问控制级别，如果无法获取，则终止检查
        val parentLevel = packageView.containingDeclaration?.visibility?.toAccessControlLevel() ?: return
        // 如果父包的访问控制级别低于当前包的级别，则报告错误
        if (parentLevel < currentLevel) {
            // 报告包访问控制违规错误
            file.packageDirective?.let {
                packageView.containingDeclaration?.fqName?.let { b ->
                    trace.report(
                        PACKAGE_ACCESS_VIOLATION.on(
                            it,
                            file.packageFqName,
                            b,
                        )
                    )
                }
            }
        }
    }
}
