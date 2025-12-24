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

package org.cangnova.cangjie.decompiler.navigation

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.moduleinfo.BinaryModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.util.binariesScope

import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.DescriptorUtils.isLocal
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.stubindex.CangJieFullClassNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelFunctionFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelTypeAliasFqNameIndex
import org.cangnova.cangjie.stubindex.CangJieTopLevelVariableFqNameIndex
import org.cangnova.cangjie.types.ErrorUtils


/**
 * 在反编译代码中查找声明（传统版本，向后兼容）
 *
 * **注意**: 此版本使用全局搜索范围，性能较差。推荐使用接受 [AnalysisContext] 的重载版本。
 *
 * @param project 当前项目
 * @param referencedDescriptor 被引用的声明描述符
 * @param builtInsSearchScope 内置库的搜索范围（可选），如果为 null 则使用全局范围查找内置库
 * @return 反编译得到的 CangJie 声明，如果未找到则返回 null
 *
 * @see findDecompiledDeclaration(AnalysisContext, DeclarationDescriptor, GlobalSearchScope?)
 */
fun findDecompiledDeclaration(
    project: Project,
    referencedDescriptor: DeclarationDescriptor,
    builtInsSearchScope: GlobalSearchScope?
): CjDeclaration? {
    // 1. 过滤无效的 Descriptor
    if (ErrorUtils.isError(referencedDescriptor)) return null
    if (isLocal(referencedDescriptor)) return null
    if (referencedDescriptor is PackageFragmentDescriptor || referencedDescriptor is PackageViewDescriptor) return null


    val binaryInfo = referencedDescriptor.module.getCapability(ModuleInfo.Capability) as? BinaryModuleInfo

    binaryInfo?.binariesScope?.let {
        return findInScope(referencedDescriptor, it)
    }
    // 2. 处理内置库
    if (CangJieBuiltIns.isBuiltIn(referencedDescriptor)) {
        // 内置库模块不包含其来源信息
        // 使用提供的 builtInsSearchScope 或回退到全局搜索
        return builtInsSearchScope?.let { findInScope(referencedDescriptor, it) }
            ?: findInScope(referencedDescriptor, GlobalSearchScope.allScope(project))
            ?: findInScope(referencedDescriptor, GlobalSearchScope.everythingScope(project))
    }

    // 3. 对于普通库文件，在整个项目范围内查找
    // 注意：由于移除了 BinaryModuleInfo，我们需要在更广的范围内搜索
    // 可以考虑添加 LibraryScope capability 来优化性能
    return findInScope(referencedDescriptor, GlobalSearchScope.allScope(project))
}

/**
 * 在指定范围内查找声明
 *
 * 该方法是查找的核心实现：
 * 1. 使用 Stub 索引找到候选的反编译文件
 * 2. 在每个反编译文件中使用 [ByDescriptorIndexer] 精确匹配
 *
 * @param referencedDescriptor 要查找的 Descriptor
 * @param scope 搜索范围
 * @return 找到的 PSI 声明，如果未找到则返回 null
 */
private fun findInScope(referencedDescriptor: DeclarationDescriptor, scope: GlobalSearchScope): CjDeclaration? {
    val project = scope.project ?: return null

    // 1. 使用 Stub 索引找到候选的反编译文件
    // 只在库类文件范围内搜索，提高性能
    val decompiledFiles = findCandidateDeclarationsInIndex(
        referencedDescriptor.original,
        CangJieSourceFilterScope.libraryClasses(scope, project),
        project
    ).mapNotNullTo(LinkedHashSet()) {
        it?.containingFile as? CjDecompiledFile
    }

    // 2. 在每个候选文件中精确查找声明
    return decompiledFiles.asSequence().mapNotNull { file ->
        ByDescriptorIndexer.getDeclarationForDescriptor(referencedDescriptor, file)
    }.firstOrNull()
}

/**
 * 判断 Descriptor 是否为局部声明
 *
 * 局部声明（函数内部的变量、嵌套函数等）不会出现在反编译文件中。
 * 递归检查参数的容器，因为参数本身总是局部的。
 *
 * @param descriptor 要检查的 Descriptor
 * @return 是否为局部声明
 */
private fun isLocal(descriptor: DeclarationDescriptor): Boolean = if (descriptor is ParameterDescriptor) {
    isLocal(descriptor.containingDeclaration)
} else {
    DescriptorUtils.isLocal(descriptor)
}

/**
 * 在 Stub 索引中查找候选声明
 *
 * 根据 Descriptor 的类型，使用不同的 Stub 索引来查找。
 * 这是一个快速的初筛步骤，返回可能包含目标声明的 PSI 元素。
 *
 * ## 查找逻辑
 *
 * - **类成员**: 查找包含该成员的类
 * - **顶层声明**: 根据类型使用对应的顶层索引
 *   - 函数: [CangJieTopLevelFunctionFqNameIndex]
 *   - 变量: [CangJieTopLevelVariableFqNameIndex]
 *   - 类型别名: [CangJieTopLevelTypeAliasFqNameIndex]
 *
 * @param referencedDescriptor 要查找的 Descriptor
 * @param scope 搜索范围
 * @param project 当前项目
 * @return 候选的 PSI 声明集合
 */
private fun findCandidateDeclarationsInIndex(
    referencedDescriptor: DeclarationDescriptor,
    scope: GlobalSearchScope,
    project: Project
): Collection<CjDeclaration?> {
    // 1. 如果是类成员，直接查找包含它的类
    val containingClass = DescriptorUtils.getParentOfType(referencedDescriptor, ClassDescriptor::class.java, false)
    if (containingClass != null) {
        return CangJieFullClassNameIndex.get(containingClass.fqNameSafe.asString(), project, scope)
    }

    // 2. 查找顶层声明
    // 向上查找直到找到顶层的属性、函数或类型别名
    val topLevelDeclaration =
        DescriptorUtils.getParentOfType(
            referencedDescriptor,
            PropertyDescriptor::class.java,
            false
        ) as DeclarationDescriptor?
            ?: DescriptorUtils.getParentOfType(
                referencedDescriptor,
                TypeAliasConstructorDescriptor::class.java,
                false
            )?.typeAliasDescriptor
            ?: DescriptorUtils.getParentOfType(referencedDescriptor, FunctionDescriptor::class.java, false)
            ?: DescriptorUtils.getParentOfType(referencedDescriptor, TypeAliasDescriptor::class.java, false)
            ?: return emptyList()

    // 3. 过滤掉合成的 Descriptor（它们不是真正的顶层声明）
    if (!DescriptorUtils.isTopLevelDeclaration(topLevelDeclaration)) return emptyList()

    // 4. 根据声明类型使用相应的索引
    val fqName = topLevelDeclaration.fqNameSafe.asString()
    return when (topLevelDeclaration) {
        is FunctionDescriptor -> CangJieTopLevelFunctionFqNameIndex.get(fqName, project, scope)
        is VariableDescriptor -> CangJieTopLevelVariableFqNameIndex.get(fqName, project, scope)
        is TypeAliasDescriptor -> CangJieTopLevelTypeAliasFqNameIndex.get(fqName, project, scope)
        else -> error("Referenced non local declaration that is not inside top level function, property, class or typealias:\n $referencedDescriptor")
    }
}
