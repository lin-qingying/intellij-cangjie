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

package org.cangnova.cangjie.macro.analysis

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.descriptors.DescriptorWithResolutionScopes
import org.cangnova.cangjie.descriptors.data.CjClassInfo
import org.cangnova.cangjie.macro.psi.MacroPsiExpansionService
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension
import org.cangnova.cangjie.resolve.extensions.SyntheticResolveExtension.PackageSyntheticNames
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyEnumDescriptor
import org.cangnova.cangjie.stubindex.CangJieMacroDeclarationShortNameIndex

/**
 * 宏展开合成解析扩展
 *
 * 将宏展开生成的声明注入到 IDE 的解析管线中。
 * 通过 [MacroPsiExpansionService.getExpandedDeclarations] 获取逐宏解析的展开声明，
 * 并将它们的名称和描述符注入到对应的作用域中。
 *
 * 注册方式：在 `cangjie-analysis.xml` 中作为 `syntheticResolveExtension` 注册。
 */
internal class MacroSyntheticResolveExtension : SyntheticResolveExtension {

    companion object {
        private val LOG = Logger.getInstance(MacroSyntheticResolveExtension::class.java)
    }

    // =========================================================================
    // 包级别：注入宏生成的顶层声明
    // =========================================================================

    override fun getSyntheticPackageNames(
        thisDescriptor: PackageFragmentDescriptor,
        declarationProvider: PackageMemberDeclarationProvider
    ): PackageSyntheticNames {
        val project = getProject(thisDescriptor) ?: return PackageSyntheticNames.EMPTY
        val expansionService = MacroPsiExpansionService.getInstance(project)

        val functionNames = mutableSetOf<Name>()
        val variableNames = mutableSetOf<Name>()
        val classifierNames = mutableSetOf<Name>()

        for (sourceFile in declarationProvider.getPackageFiles()) {
            // 1. 宏展开输出 + 宏输入声明的名字收集
            if (expansionService.isEnabled()) {
                for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                    collectDeclarationNames(info.declarations, functionNames, variableNames, classifierNames)
                    // 宏输入声明名（用于触发解析，但不注入作用域）
                    val inputDecl = info.macroExpression.input?.declarations
                    if (inputDecl != null) {
                        collectDeclarationNames(listOf(inputDecl), functionNames, variableNames, classifierNames)
                    }
                }
            }

            // 2. 非宏的注解声明：@Annotation class A {} 等
            //    解析器统一将 @Name ... 解析为 CjMacroExpression，但注解不是宏，
            //    其输入声明需要正常注入作用域
            for (macroExpr in getFileLevelMacroExpressions(sourceFile)) {
                if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) {
                    continue // 有展开结果的是宏，由上面的逻辑处理
                }
                val inputDecl = macroExpr.input?.declarations ?: continue
                collectDeclarationNames(listOf(inputDecl), functionNames, variableNames, classifierNames)
            }
        }

        if (functionNames.isEmpty() && variableNames.isEmpty() && classifierNames.isEmpty()) {
            return PackageSyntheticNames.EMPTY
        }

        return PackageSyntheticNames(functionNames, variableNames, classifierNames)
    }

    override fun generateSyntheticTopLevelClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)

        for (sourceFile in declarationProvider.getPackageFiles()) {
            // 1. 宏展开输出 → 注入作用域；宏输入 → 仅解析
            if (expansionService.isEnabled()) {
                for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                    for (declaration in info.declarations) {
                        if (declaration !is CjTypeStatement || declaration is CjEnum) continue
                        val declName = declaration.nameAsName ?: continue
                        if (declName != name) continue
                        try {
                            val classInfo = CjClassInfo(declaration, declaration.getClassKind())
                            result.add(LazyClassDescriptor(ctx, thisDescriptor, name, classInfo, false))
                        } catch (e: Exception) {
                            LOG.debug("解析宏生成的类 $name 失败", e)
                        }
                    }
                    // 宏输入声明 → 仅解析，不注入
                    val inputDecl = info.macroExpression.input?.declarations
                    if (inputDecl is CjTypeStatement && inputDecl !is CjEnum && inputDecl.nameAsName == name) {
                        try {
                            LazyClassDescriptor(ctx, thisDescriptor, name, CjClassInfo(inputDecl, inputDecl.getClassKind()), false)
                        } catch (e: Exception) {
                            LOG.debug("解析宏输入类 $name 失败", e)
                        }
                    }
                }
            }

            // 2. 注解声明 → 正常注入作用域
            for (macroExpr in getFileLevelMacroExpressions(sourceFile)) {
                if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) continue
                val inputDecl = macroExpr.input?.declarations ?: continue
                if (inputDecl !is CjTypeStatement || inputDecl is CjEnum) continue
                val declName = inputDecl.nameAsName ?: continue
                if (declName != name) continue
                try {
                    val classInfo = CjClassInfo(inputDecl, inputDecl.getClassKind())
                    result.add(LazyClassDescriptor(ctx, thisDescriptor, name, classInfo, false))
                } catch (e: Exception) {
                    LOG.debug("解析注解声明类 $name 失败", e)
                }
            }
        }
    }

    override fun generateSyntheticEnums(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<EnumDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)

        for (sourceFile in declarationProvider.getPackageFiles()) {
            // 1. 宏展开输出 → 注入；宏输入 → 仅解析
            if (expansionService.isEnabled()) {
                for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                    for (declaration in info.declarations) {
                        if (declaration !is CjEnum) continue
                        val declName = declaration.nameAsName ?: continue
                        if (declName != name) continue
                        try {
                            result.add(LazyEnumDescriptor(ctx, thisDescriptor, name, CjClassInfo(declaration, ClassKind.ENUM)))
                        } catch (e: Exception) {
                            LOG.debug("解析宏生成的枚举 $name 失败", e)
                        }
                    }
                    val inputDecl = info.macroExpression.input?.declarations
                    if (inputDecl is CjEnum && inputDecl.nameAsName == name) {
                        try {
                            LazyEnumDescriptor(ctx, thisDescriptor, name, CjClassInfo(inputDecl, ClassKind.ENUM))
                        } catch (e: Exception) {
                            LOG.debug("解析宏输入枚举 $name 失败", e)
                        }
                    }
                }
            }

            // 2. 注解声明 → 正常注入作用域
            for (macroExpr in getFileLevelMacroExpressions(sourceFile)) {
                if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) continue
                val inputDecl = macroExpr.input?.declarations ?: continue
                if (inputDecl !is CjEnum) continue
                if (inputDecl.nameAsName != name) continue
                try {
                    result.add(LazyEnumDescriptor(ctx, thisDescriptor, name, CjClassInfo(inputDecl, ClassKind.ENUM)))
                } catch (e: Exception) {
                    LOG.debug("解析注解声明枚举 $name 失败", e)
                }
            }
        }
    }

    override fun generateSyntheticFunctions(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<SimpleFunctionDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)

        for (sourceFile in declarationProvider.getPackageFiles()) {
            val fileScope = ctx.fileScopeProvider.getFileResolutionScope(sourceFile)

            // 1. 宏展开输出 → 注入；宏输入 → 仅解析
            if (expansionService.isEnabled()) {
                for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                    for (declaration in info.declarations) {
                        if (declaration !is CjNamedFunction) continue
                        if (declaration.nameAsName != name) continue
                        try {
                            result.add(ctx.functionDescriptorResolver.resolveFunctionDescriptor(
                                thisDescriptor, fileScope, declaration, ctx.trace,
                                DataFlowInfoFactory.EMPTY, ctx.inferenceSession
                            ))
                        } catch (e: Exception) {
                            LOG.debug("解析宏生成的函数 $name 失败", e)
                        }
                    }
                    val inputDecl = info.macroExpression.input?.declarations
                    if (inputDecl is CjNamedFunction && inputDecl.nameAsName == name) {
                        try {
                            ctx.functionDescriptorResolver.resolveFunctionDescriptor(
                                thisDescriptor, fileScope, inputDecl, ctx.trace,
                                DataFlowInfoFactory.EMPTY, ctx.inferenceSession
                            )
                        } catch (e: Exception) {
                            LOG.debug("解析宏输入函数 $name 失败", e)
                        }
                    }
                }
            }

            // 2. 注解声明 → 正常注入作用域
            for (macroExpr in getFileLevelMacroExpressions(sourceFile)) {
                if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) continue
                val inputDecl = macroExpr.input?.declarations ?: continue
                if (inputDecl !is CjNamedFunction) continue
                if (inputDecl.nameAsName != name) continue
                try {
                    result.add(ctx.functionDescriptorResolver.resolveFunctionDescriptor(
                        thisDescriptor, fileScope, inputDecl, ctx.trace,
                        DataFlowInfoFactory.EMPTY, ctx.inferenceSession
                    ))
                } catch (e: Exception) {
                    LOG.debug("解析注解声明函数 $name 失败", e)
                }
            }
        }
    }

    override fun generateSyntheticVariables(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<VariableDescriptor>
    ) {
        val project = getProject(thisDescriptor) ?: return
        val expansionService = MacroPsiExpansionService.getInstance(project)

        for (sourceFile in declarationProvider.getPackageFiles()) {
            val fileScope = ctx.fileScopeProvider.getFileResolutionScope(sourceFile)

            // 1. 宏展开输出 → 注入；宏输入 → 仅解析
            if (expansionService.isEnabled()) {
                for (info in expansionService.getExpandedDeclarations(sourceFile)) {
                    for (declaration in info.declarations) {
                        if (declaration !is CjVariable<*>) continue
                        resolveVariableDeclaration(declaration, name, thisDescriptor, fileScope, ctx, result)
                    }
                    val inputDecl = info.macroExpression.input?.declarations
                    if (inputDecl is CjVariable<*>) {
                        resolveVariableDeclaration(inputDecl, name, thisDescriptor, fileScope, ctx, null)
                    }
                }
            }

            // 2. 注解声明 → 正常注入作用域
            for (macroExpr in getFileLevelMacroExpressions(sourceFile)) {
                if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) continue
                val inputDecl = macroExpr.input?.declarations ?: continue
                if (inputDecl !is CjVariable<*>) continue
                resolveVariableDeclaration(inputDecl, name, thisDescriptor, fileScope, ctx, result)
            }
        }
    }

    // =========================================================================
    // 类成员级别：注入注解包裹的成员声明
    // =========================================================================

    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
        val result = mutableListOf<Name>()
        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl is CjNamedFunction) {
                val name = inputDecl.nameAsName ?: continue
                result.add(name)
            }
        }
        return result
    }

    override fun getSyntheticPropertiesNames(thisDescriptor: ClassDescriptor): List<Name> {
        val result = mutableListOf<Name>()
        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl is CjProperty) {
                val name = inputDecl.nameAsName ?: continue
                result.add(name)
            }
        }
        return result
    }

    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        val result = mutableListOf<Name>()
        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl is CjTypeStatement) {
                val name = inputDecl.nameAsName ?: continue
                result.add(name)
            }
        }
        return result
    }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassAndEnumDescriptor,
        name: Name,
        ctx: LazyClassContext,
        bindingContext: BindingContext,
        fromSupertypes: List<SimpleFunctionDescriptor>,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        val scope = (thisDescriptor as? DescriptorWithResolutionScopes)
            ?.scopeForMemberDeclarationResolution ?: return

        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl !is CjNamedFunction) continue
            if (inputDecl.nameAsName != name) continue
            try {
                result.add(ctx.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor, scope, inputDecl, ctx.trace,
                    DataFlowInfoFactory.EMPTY, ctx.inferenceSession
                ))
            } catch (e: Exception) {
                LOG.debug("解析注解成员函数 $name 失败", e)
            }
        }
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassAndEnumDescriptor,
        name: Name,
        ctx: LazyClassContext,
        bindingContext: BindingContext,
        fromSupertypes: List<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
        val scope = (thisDescriptor as? DescriptorWithResolutionScopes)
            ?.scopeForMemberDeclarationResolution ?: return

        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl !is CjProperty) continue
            if (inputDecl.nameAsName != name) continue
            try {
                result.add(ctx.descriptorResolver.resolvePropertyDescriptor(
                    thisDescriptor, scope, scope, inputDecl, ctx.trace,
                    DataFlowInfoFactory.EMPTY,
                    ctx.inferenceSession ?: InferenceSession.default
                ))
            } catch (e: Exception) {
                LOG.debug("解析注解成员属性 $name 失败", e)
            }
        }
    }

    override fun generateSyntheticNestedClasses(
        thisDescriptor: ClassAndEnumDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: ClassMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        for (macroExpr in getClassBodyMacroExpressions(thisDescriptor)) {
            if (isRealMacro(macroExpr)) continue
            val inputDecl = macroExpr.input?.declarations ?: continue
            if (inputDecl !is CjTypeStatement || inputDecl is CjEnum) continue
            if (inputDecl.nameAsName != name) continue
            try {
                val classInfo = CjClassInfo(inputDecl, inputDecl.getClassKind())
                result.add(LazyClassDescriptor(ctx, thisDescriptor, name, classInfo, false))
            } catch (e: Exception) {
                LOG.debug("解析注解嵌套类 $name 失败", e)
            }
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 解析变量声明并（可选地）添加到结果集
     *
     * @param variable 变量声明 PSI（CjPatternVariable 或 CjFieldVariable）
     * @param name 目标变量名
     * @param container 包含该变量的描述符
     * @param fileScope 文件作用域
     * @param ctx 延迟类上下文
     * @param result 非 null 时将描述符添加到结果集；null 时仅解析（记录绑定）不注入作用域
     */
    private fun resolveVariableDeclaration(
        variable: CjVariable<*>,
        name: Name,
        container: PackageFragmentDescriptor,
        fileScope: org.cangnova.cangjie.resolve.scopes.LexicalScope,
        ctx: LazyClassContext,
        result: MutableSet<VariableDescriptor>?
    ) {
        try {
            when (variable) {
                is CjPatternVariable -> {
                    // 检查该模式变量是否包含目标名称的绑定
                    val hasName = variable.pattern.getAllBindings().any { it.nameAsName == name }
                    if (!hasName) return

                    val descriptors = ctx.descriptorResolver.resolveVariableDescriptorByPattern(
                        name,
                        container,
                        fileScope,
                        variable,
                        ctx.trace,
                        DataFlowInfoFactory.EMPTY,
                        ctx.inferenceSession ?: InferenceSession.default
                    )
                    result?.addAll(descriptors)
                }
                is CjFieldVariable -> {
                    val declName = variable.nameAsName ?: return
                    if (declName != name) return

                    val descriptor = ctx.descriptorResolver.resolveVariableDescriptor(
                        container,
                        fileScope,
                        fileScope,
                        variable,
                        ctx.trace,
                        DataFlowInfoFactory.EMPTY,
                        ctx.inferenceSession ?: InferenceSession.default
                    )
                    result?.add(descriptor)
                }
            }
        } catch (e: Exception) {
            LOG.debug("解析宏变量 $name 失败", e)
        }
    }

    /**
     * 从描述符获取 IntelliJ Project 实例
     */
    private fun getProject(descriptor: DeclarationDescriptor): Project? {
        return try {
            DescriptorUtils.getContainingModule(descriptor).projectDescriptor.project
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取文件直接子级的宏表达式（包含注解和宏调用）
     */
    private fun getFileLevelMacroExpressions(file: CjFile): Collection<CjMacroExpression> {
        return PsiTreeUtil.getChildrenOfTypeAsList(file, CjMacroExpression::class.java)
    }

    /**
     * 获取类体内直接子级的宏表达式
     */
    private fun getClassBodyMacroExpressions(classDescriptor: ClassAndEnumDescriptor): Collection<CjMacroExpression> {
        val classPsi = DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor)
            as? CjTypeStatement ?: return emptyList()
        val body = classPsi.body ?: return emptyList()
        return PsiTreeUtil.getChildrenOfTypeAsList(body, CjMacroExpression::class.java)
    }

    /**
     * 判断 CjMacroExpression 是否是宏调用（而非普通注解）
     *
     * 判断依据（满足任一即为宏）：
     * 1. 宏展开服务已返回展开结果
     * 2. 名字在 Stub 索引中匹配到 CjMacroDeclaration
     */
    private fun isRealMacro(macroExpr: CjMacroExpression): Boolean {
        val project = macroExpr.project ?: return false
        val expansionService = MacroPsiExpansionService.getInstance(project)
        if (expansionService.isEnabled() && expansionService.getExpansionResult(macroExpr) != null) {
            return true
        }
        // 通过 Stub 索引查找名字是否对应宏声明
        val shortName = macroExpr.shortName?.asString() ?: return false
        val scope = GlobalSearchScope.allScope(project)
        return CangJieMacroDeclarationShortNameIndex[shortName, project, scope].isNotEmpty()
    }

    /**
     * 从声明列表中收集函数名、变量名和类名
     */
    private fun collectDeclarationNames(
        declarations: List<CjDeclaration>,
        functionNames: MutableSet<Name>,
        variableNames: MutableSet<Name>,
        classifierNames: MutableSet<Name>
    ) {
        for (declaration in declarations) {
            when (declaration) {
                is CjTypeStatement -> {
                    val name = declaration.nameAsName ?: continue
                    classifierNames.add(name)
                }
                is CjNamedFunction -> {
                    val name = declaration.nameAsName ?: continue
                    functionNames.add(name)
                }
                is CjPatternVariable -> {
                    for (binding in declaration.pattern.getAllBindings()) {
                        val name = binding.nameAsName ?: continue
                        variableNames.add(name)
                    }
                }
                is CjFieldVariable -> {
                    val name = declaration.nameAsName ?: continue
                    variableNames.add(name)
                }
            }
        }
    }

    /**
     * 获取 CjTypeStatement 的 ClassKind
     */
    private fun CjTypeStatement.getClassKind(): ClassKind {
        return when (this) {
            is CjInterface -> ClassKind.INTERFACE
            is CjEnum -> ClassKind.ENUM
            is CjStruct -> ClassKind.CLASS
            else -> ClassKind.CLASS
        }
    }
}
