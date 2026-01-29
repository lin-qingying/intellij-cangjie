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

import jakarta.inject.Inject
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.resolve.lazy.FileScopeProvider
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyDeclarationResolver
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendDescriptor
import org.cangnova.cangjie.storage.StorageManager

/**
 * 扩展描述符解析器
 *
 * 负责从 PSI 层的 CjExtend 创建 ExtendDescriptor，并注册到 ExtendManager 中。
 *
 * @param trace 绑定追踪器，用于记录解析结果
 * @param lazyDeclarationResolver 延迟声明解析器
 * @param fileScopeProvider 文件作用域提供者
 * @param descriptorResolver 描述符解析器
 * @param typeResolver 类型解析器
 * @param overloadChecker 重载检查器
 * @param builtIns 内建类型
 * @param storageManager 存储管理器
 */
class ExtendDescriptorResolver(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val fileScopeProvider: FileScopeProvider,
    private val descriptorResolver: DescriptorResolver,
    private val typeResolver: TypeResolver,
    private val overloadChecker: OverloadChecker,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager
) {
    @set:Inject
    lateinit var lazyClassContext: LazyClassContext

    /**
     * 从 PSI 创建扩展描述符
     *
     * 该方法：
     * 1. 检查是否已经解析过（避免重复解析）
     * 2. 确定扩展的包含声明（通常是包描述符）
     * 3. 创建 LazyExtendDescriptor 实例
     * 4. 将扩展注册到 BindingContext 中
     * 5. 将扩展注册到 ExtendManager 中（用于类型系统集成）
     * 6. 执行扩展声明的约束检查（孤儿规则、扩展冲突等）
     *
     * @param cjExtend PSI 层的扩展声明
     * @return 创建的扩展描述符
     */
    fun getExtendDescriptor(cjExtend: CjExtend): ExtendDescriptor {
        // 1. 检查是否已经解析过
        trace.bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, cjExtend)?.let {
            return it as ExtendDescriptor
        }

        // 2. 获取包含声明（通常是包描述符）
        val containingDeclaration = getContainingDeclaration(cjExtend)

        // 3. 创建 LazyExtendDescriptor
        val descriptor = LazyExtendDescriptor(
            c = lazyClassContext,
            containingDeclaration = containingDeclaration,
            cjExtend = cjExtend
        )

        // 4. 注册到 BindingContext
        trace.record(BindingContext.EXTEND, cjExtend, descriptor)

        // 5. 注册到 ExtendManager（用于类型系统集成）
        registerExtendInManager(descriptor, lazyClassContext.moduleDescriptor)

        // 6. 执行扩展声明的约束检查
        performExtendChecks(cjExtend, descriptor)

        return descriptor
    }

    /**
     * 获取扩展的包含声明
     *
     * 扩展通常定义在包级别，因此返回对应的包描述符。
     */
    private fun getContainingDeclaration(cjExtend: CjExtend): DeclarationDescriptor {
        val packageFqName = cjExtend.getContainingCjFile().packageFqName
        return lazyClassContext.moduleDescriptor.getPackage(packageFqName)
    }

    /**
     * 将扩展注册到 ExtendManager
     *
     * ExtendManager 维护模块级的所有扩展定义，供类型系统查询。
     * 这样当类型检查器需要查找某个类型的扩展接口时，可以从 ExtendManager 获取。
     */
    private fun registerExtendInManager(
        descriptor: ExtendDescriptor,
        module: ModuleDescriptor
    ) {
        val extendManager = module.getCapability(ExtendManager.CAPABILITY) ?: return

        val extensionDef = ExtendManager.ExtensionDef(
            id = descriptor.extendId,
            extendedConstructor = descriptor.extendType.constructor,
            typeParameters = descriptor.declaredTypeParameters,
            interfaces = descriptor.superTypes.toList(),
            memberScope = descriptor.unsubstitutedMemberScope,
            descriptor = descriptor  // 保存 ExtendDescriptor 引用，便于后续查询
        )

        // 使用 register 方法增量注册扩展，避免覆盖已注册的其他扩展
        extendManager.register(extensionDef)
    }

    /**
     * 执行扩展声明的约束检查
     *
     * 包括：
     * 1. 孤儿规则检查
     * 2. 扩展冲突检查
     *
     * @param cjExtend PSI 扩展声明
     * @param descriptor 扩展描述符
     */
    private fun performExtendChecks(
        cjExtend: CjExtend,
        descriptor: ExtendDescriptor
    ) {
        val extendChecker = ExtendChecker(trace)

        // 1. 检查孤儿规则
        extendChecker.checkOrphanRule(cjExtend, descriptor)

        // 2. 检查扩展冲突
        // 获取当前模块的所有扩展（从 ExtendManager）
        val extendManager = descriptor.module.getCapability(ExtendManager.CAPABILITY)
        if (extendManager != null) {
            val allExtends = getAllExtendsInModule(descriptor.module)
            extendChecker.checkExtendConflicts(cjExtend, descriptor, allExtends)
        }
    }

    /**
     * 获取模块中的所有扩展描述符
     *
     * 从 ExtendManager 获取该模块已注册的所有扩展定义，并提取其中的 ExtendDescriptor。
     *
     * 注意：此方法只能获取到当前已解析并注册的扩展。如果还有扩展未解析，则无法检测到与它们的冲突。
     * 这是延迟解析的固有限制。
     *
     * @param module 模块描述符
     * @return 模块中已解析的所有扩展描述符
     */
    private fun getAllExtendsInModule(module: ModuleDescriptor): List<ExtendDescriptor> {
        val extendManager = module.getCapability(ExtendManager.CAPABILITY) ?: return emptyList()

        // 从 ExtendManager 获取所有扩展定义
        val allExtensionDefs = extendManager.getAllExtensions()

        // 从 ExtensionDef 中提取 ExtendDescriptor
        return allExtensionDefs.mapNotNull { extensionDef ->
            // descriptor 字段存储的是 ExtendDescriptor，但声明为 Any 以避免模块循环依赖
            extensionDef.descriptor as? ExtendDescriptor
        }
    }
}
