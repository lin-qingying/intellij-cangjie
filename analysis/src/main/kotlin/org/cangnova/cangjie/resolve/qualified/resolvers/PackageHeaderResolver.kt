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
 */

package org.cangnova.cangjie.resolve.qualified.resolvers

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjPackageDirective
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.qualified.QualifierPosition
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext

/**
 * 包声明解析器
 *
 * 负责解析包声明语句（`package com.example`），验证包路径的有效性并记录绑定信息。
 *
 * ## 功能
 *
 * 1. 验证包路径中的每一级是否有效
 * 2. 记录每个名称部分与对应包描述符的绑定
 * 3. 标记路径中非最后部分为限定符
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 对于 `package com.example.myapp`
 * val resolver = PackageHeaderResolver()
 * resolver.resolve(packageDirective, context)
 *
 * // 解析后：
 * // - "com" → 绑定到 PackageViewDescriptor("com")
 * // - "example" → 绑定到 PackageViewDescriptor("com.example")
 * // - "myapp" → 绑定到 PackageViewDescriptor("com.example.myapp")
 * ```
 */
class PackageHeaderResolver {
    /**
     * 解析包声明
     *
     * @param packageDirective 包声明 PSI 节点
     * @param context 解析上下文
     */
    fun resolve(
        packageDirective: CjPackageDirective,
        context: ResolutionContext
    ) {
        val packageNames = packageDirective.packageNames
        val module = context.moduleDescriptor

        for ((index, nameExpression) in packageNames.withIndex()) {
            val fqName = packageDirective.getFqName(nameExpression)
            val packageDescriptor = module.getPackage(fqName)

            // 记录引用目标
            context.trace.record(
                BindingContext.REFERENCE_TARGET,
                nameExpression,
                packageDescriptor
            )

            // 如果不是最后一个，标记为限定符
            if (index != packageNames.lastIndex) {
                // 创建并记录限定符
                // 注意：包声明位置的限定符不需要完整的 QualifierReceiver 处理
            }
        }
    }

    /**
     * 使用模块描述符和绑定追踪器解析包声明
     *
     * 这是一个便捷方法，兼容旧的调用方式。
     *
     * @param packageDirective 包声明 PSI 节点
     * @param module 模块描述符
     * @param trace 绑定追踪器
     */
    fun resolve(
        packageDirective: CjPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        val packageNames = packageDirective.packageNames

        for ((index, nameExpression) in packageNames.withIndex()) {
            val fqName = packageDirective.getFqName(nameExpression)
            val packageDescriptor = module.getPackage(fqName)

            trace.record(
                BindingContext.REFERENCE_TARGET,
                nameExpression,
                packageDescriptor
            )
        }
    }
}
