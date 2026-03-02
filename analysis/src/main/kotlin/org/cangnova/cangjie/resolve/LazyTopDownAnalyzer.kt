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

import org.cangnova.cangjie.dag.CangJieDependencyGraph
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.util.BackgroundTaskUtil.executeOnPooledThread
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.CONSTRUCTOR_IN_INTERFACE
import org.cangnova.cangjie.diagnostics.infos.errors.PACKAGE_ACCESS_VIOLATION
import org.cangnova.cangjie.incremental.CangJieLookupLocation
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.lazy.*
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import org.cangnova.cangjie.resolve.lazy.descriptors.LazyEnumDescriptor
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolverFacade
import org.cangnova.cangjie.stubindex.CangJieExactPackagesIndex
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

/**
 * LazyTopDownAnalyzer类负责按需分析和解析程序中的声明、表达式和语句
 * 它采用惰性分析方式，即仅在需要时才进行相应的解析和分析，以提高性能和效率
 *
 * @param trace 用于记录绑定过程中的跟踪信息，帮助调试和验证分析过程
 * @param lazyDeclarationResolver 用于惰性解析声明，仅在声明实际需要时才进行解析
 * @param declarationResolver 负责解析声明，与lazyDeclarationResolver协同工作
 *
 * @param overrideResolver 用于解析和处理方法重写关系，确保正确处理继承结构
 * @param overloadResolver 用于解析和处理函数和属性的重载情况
 * @param fileScopeProvider 提供文件作用域，帮助解析文件内的声明和表达式
 * @param packagerResolver 用于解析包结构，确保正确处理模块内的包和子包
 * @param bodyResolver 负责解析函数体、属性体等，是分析过程中的关键组件
 * @param identifierChecker 用于检查标识符的有效性，防止关键字冲突和非法字符使用
 * @param qualifiedExpressionResolver 用于解析合格表达式，如成员访问和调用链
 * @param moduleDescriptor 模块描述符，提供模块级别的信息和上下文
 * @param topLevelDescriptorProvider 提供顶级声明的描述符，帮助解析模块顶层的声明
 * @param mainFunctionResolver 用于解析主函数，确保程序的入口点被正确处理
 *
 * @param declarationScopeProvider 提供声明作用域，帮助解析声明在不同上下文中的可见性和有效性
 * @param filePreprocessor 负责预处理文件，如解析文件注释和指令
 * @param extendDescriptorResolver 用于解析扩展声明，如扩展函数和属性
 * @param dependencyGraph 表示依赖关系的图结构，帮助管理和解析声明和模块之间的依赖关系
 */
class LazyTopDownAnalyzer(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val declarationResolver: DeclarationResolver,

    private val overrideResolver: OverrideResolver,
    private val overloadResolver: OverloadResolver,
    private val fileScopeProvider: FileScopeProvider,
    private val packagerResolver: PackagerResolver,
    private val bodyResolver: BodyResolver,
    private val identifierChecker: IdentifierChecker,
    private val qualifiedExpressionResolver: QualifiedExpressionResolverFacade,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val mainFunctionResolver: MainFunctionResolver,

    private val declarationScopeProvider: DeclarationScopeProvider,
    private val filePreprocessor: FilePreprocessor,
    private val dependencyGraph: CangJieDependencyGraph,

    ) {


    fun analyzeDeclarations(
        topDownAnalysisMode: TopDownAnalysisMode,
        declarations: Collection<PsiElement>,
        outerDataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        localContext: ExpressionTypingContext? = null
    ): TopDownAnalysisContext {

        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo, declarationScopeProvider, localContext)


        val properties = mutableListOf<CjProperty>()
        val variables = mutableListOf<CjVariable<*>>()
        val functions = mutableListOf<CjNamedFunction>()
        val macroDeclarations = mutableListOf<CjMacroDeclaration>()
        val macroExpressions = mutableListOf<CjMacroExpression>()
        val mainFunctions = mutableListOf<CjMainFunction>()
        val typeAliases = mutableListOf<CjTypeAlias>()

        val reexports = mutableListOf<CjImportItem>()

        val topLevelFqNames = HashMultimap.create<FqName, CjElement>()

        // 填充上下文
        for (declaration in declarations) {
 
            var visitor: CjVisitorUnit? = null
            visitor = ExceptionWrappingCjVisitorUnit(object : CjVisitorUnit() {
                private fun registerDeclarations(declarations: List<CjDeclaration>) {
                    for (cjDeclaration in declarations) {
                        cjDeclaration.accept(visitor!!)
                    }
                }

                override fun visitProperty(property: CjProperty) {
                    properties.add(property)
                }


                override fun visitPatternVariable(variable: CjPatternVariable) {
                    variables.add(variable)

                }


                override fun visitFieldVariable(field: CjFieldVariable) {
                    variables.add(field)
                }


                override fun visitTypeAlias(typeAlias: CjTypeAlias) {
                    typeAliases.add(typeAlias)
                }


                override fun visitDeclaration(dcl: CjDeclaration) {
                    throw IllegalArgumentException("Unsupported declaration: " + dcl + " " + dcl.text)
                }

                override fun visitImportItem(importItem: CjImportItem, data: Unit?) {
                    val importResolver = fileScopeProvider.getImportResolver(importItem.getContainingCjFile())

//                    TODO 修改该语句，添加重导出回调，返回包名映射
                    importResolver.forceResolveImport(importItem)


                }

                override fun visitTypeStatement(typeStatement: CjTypeStatement) {
                    val location = CangJieLookupLocation(typeStatement)

                    val descriptor =
                        lazyDeclarationResolver.getClassDescriptor(
                            typeStatement,
                            location
                        ) as  DescriptorWithResolutionScopes

                    c.declaredClasses[typeStatement] = descriptor
                    registerDeclarations(typeStatement.declarations)
                    registerTopLevelFqName(topLevelFqNames, typeStatement, descriptor)

                    checkTypeStatementDeclarations(typeStatement, descriptor)
                }

                override fun visitExtend(extend: CjExtend) {
                    // 解析扩展声明
                    val descriptor = lazyDeclarationResolver.resolveToDescriptor(extend)
                    c.extends[extend] = descriptor as ExtendDescriptor
                    // 递归注册扩展内部的声明（函数、属性等）
                    registerDeclarations(extend.declarations)
                }

                override fun visitClass(cclass: CjClass) {
                    visitTypeStatement(cclass)
                }

                override fun visitEnum(cenum: CjEnum) {
                    visitTypeStatement(cenum)

                }
                override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
                    c.primaryConstructors[constructor] =
                        lazyDeclarationResolver.resolveToDescriptor(constructor) as ClassConstructorDescriptor
                }

                override fun visitEnumConstructor(enumConstructor: CjEnumConstructor, data: Unit?) {
                    // 枚举构造器通过父枚举的 constructors 属性自动解析
                    // 这里不需要手动调用 resolveToDescriptor
                    val parentEnum = enumConstructor.parentEnum ?: return
                    val enumDescriptor = lazyDeclarationResolver.getEnumDescriptorIfAny(
                        parentEnum,
                        NoLookupLocation.FROM_BACKEND
                    ) ?: return

                    // 触发枚举构造器解析
                    enumDescriptor.constructors

                    // 从绑定上下文中获取已解析的描述符（使用 ENUM_CONSTRUCTOR 专用 key）
                    val descriptor = trace.bindingContext.get(BindingContext.ENUM_CONSTRUCTOR, enumConstructor)
                    if (descriptor != null) {
                        c.enumConstructors[enumConstructor] = descriptor
                    }
                }

                override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
                    c.secondaryConstructors[constructor] =
                        lazyDeclarationResolver.resolveToDescriptor(constructor) as ClassConstructorDescriptor
                }

                override fun visitEndSecondaryConstructor(constructor: CjEndSecondaryConstructor) {
                    c.endSecondaryConstructors[constructor] =
                        lazyDeclarationResolver.resolveToDescriptor(constructor) as ClassConstructorDescriptor
                }

                private fun checkTypeStatementDeclarations(
                    typeStatement: CjTypeStatement,
                    classDescriptor: ClassAndEnumDescriptor
                ) {
                    for (cjDeclaration in typeStatement.declarations) {
                        if (cjDeclaration is CjSecondaryConstructor) {
                            if (classDescriptor.kind == ClassKind.INTERFACE) {
                                trace.report(CONSTRUCTOR_IN_INTERFACE.on(cjDeclaration))
                            }
                        }
                    }
                }

                override fun visitCjFile(file: CjFile) {
                    filePreprocessor.preprocessFile(file)
                    registerDeclarations(file.declarations)

                    // 特殊处理：遍历文件中的所有顶层宏表达式
                    // 因为宏表达式是表达式而不是声明，不在 file.declarations 中
                    file.children.forEach { child ->
                        if (child is CjMacroExpression) {
                            child.accept(visitor!!)
                        }
                    }

                    val packageDirective = file.packageDirective
                    assert(packageDirective != null) { "No package in a non-script file: $file" }
                    packageDirective?.accept(this)
                    c.addFile(file)
                    if (packageDirective != null) topLevelFqNames.put(file.packageFqName, packageDirective)
                }

                override fun visitNamedFunction(function: CjNamedFunction) {
                    functions.add(function)
                }

                override fun visitMacroDeclaration(function: CjMacroDeclaration) {
                    macroDeclarations.add(function)
                }

                override fun visitMacroExpression(expression: CjMacroExpression, data: Unit?) {
                    macroExpressions.add(expression)
                    // 注释掉：不需要遍历宏表达式内部的声明，因为只能在宏展开后分析
                    // expression.children.forEach { child ->
                    //     if (child is CjDeclaration) {
                    //         child.accept(visitor!!)
                    //     }
                    // }
                }

                override fun visitMainFunction(mainFunction: CjMainFunction) {
                    mainFunctions.add(mainFunction)

                }

                override fun visitPackageDirective(directive: CjPackageDirective) {
                    directive.packageNames.forEach { identifierChecker.checkIdentifier(it, trace) }
                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)


                }


            })

            declaration.accept(visitor)
        }
        createFunctionDescriptors(c, functions)
        createMainFunctionDescriptors(c, mainFunctions)
        createMacroDescriptors(c, macroDeclarations)
        // 将收集的宏表达式添加到上下文中，后续由 BodyResolver 处理
        c.macroExpressions.addAll(macroExpressions)
        createPropertyDescriptors(c, topLevelFqNames, properties)

        createVariableDescriptors(c, topLevelFqNames, variables)
        createTypeAliasDescriptors(c, topLevelFqNames, typeAliases)


        resolveAllHeadersInClasses(c)

        packagerResolver.check(c)


        declarationResolver.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames)
        declarationResolver.check(c)

//        declarationResolver.resolveAnnotationsOnFiles(c, fileScopeProvider)

        overrideResolver.check(c)


        overloadResolver.checkOverloads(c)

        bodyResolver.resolveBodies(c)

        mainFunctionResolver.check(c)
        resolveImportsInAllFiles(c)

        return c

    }


    fun resolveImportsInFile(file: CjFile) {

        fileScopeProvider.getImportResolver(file).forceResolveNonDefaultImports()
    }


    /**
     * 该方法检查包等级，耗时操作
     */
    @Deprecated("")
    private fun checkPackagelevel(directive: CjPackageDirective) {
        executeOnPooledThread({ }) {

            runReadAction {
                val currentLevel = directive.modifierVisibility.toAccessControlLevel()
                if (currentLevel == 0) {
                    return@runReadAction
                }
                if (directive.fqName.isSingleSegment()) {
                    return@runReadAction
                }
//                获取父包索引，检查等级
                val parentPackageFqName = directive.fqName.parent()
                CangJieExactPackagesIndex.get(parentPackageFqName.asString(), directive.project).forEach { file ->

                    file.packageDirective?.modifierVisibility?.let {
                        if (it.toAccessControlLevel() < currentLevel) {

                            trace.report(
                                PACKAGE_ACCESS_VIOLATION.on(
                                    directive,
                                    directive.fqName,
                                    file.packageFqName
                                )
                            )
                            return@forEach
                        }
                    }


                }

            }
        }
    }


    private fun resolveImportsInAllFiles(c: TopDownAnalysisContext) {
        for (file in c.files.map { it.getContainingCjFile() }) {
            resolveImportsInFile(file)
        }
    }

    private fun resolveAllHeadersInClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.declaredClasses.values) {
            when (classDescriptor) {

                is LazyEnumDescriptor ->
                    classDescriptor.resolveMemberHeaders()
                is LazyClassDescriptor ->
                    classDescriptor.resolveMemberHeaders()

            }
        }
    }



    private fun createTypeAliasDescriptors(
        c: TopDownAnalysisContext,
        topLevelFqNames: Multimap<FqName, CjElement>,
        typeAliases: List<CjTypeAlias>
    ) {
        for (typeAlias in typeAliases) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(typeAlias) as TypeAliasDescriptor

            c.typeAliases[typeAlias] = descriptor
            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
            registerTopLevelFqName(topLevelFqNames, typeAlias, descriptor)
        }
    }

    private fun createPropertyDescriptors(
        c: TopDownAnalysisContext,
        topLevelFqNames: HashMultimap<FqName, CjElement>,
        propertys: MutableList<CjProperty>
    ) {
        for (property in propertys) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(property) as PropertyDescriptor

            c.properties[property] = descriptor
            registerTopLevelFqName(topLevelFqNames, property, descriptor)
        }
    }

    private fun createVariableDescriptors(
        c: TopDownAnalysisContext,
        topLevelFqNames: HashMultimap<FqName, CjElement>,
        variables: MutableList<CjVariable<*>>
    ) {
        for (variable in variables) {
            when (variable) {
                is CjFieldVariable -> {
                    // 字段变量
                    val descriptor = lazyDeclarationResolver.resolveToDescriptor(variable) as VariableDescriptor
                    c.variables[variable] = listOf(descriptor)
                    registerTopLevelFqName(topLevelFqNames, variable, descriptor)
                }

                is CjPatternVariable -> {
                    if (variable.pattern != null) {
                        // 模式匹配变量，可能包含多个绑定
                        val descriptors = lazyDeclarationResolver.resolveToVariableByPattern(variable)
                        c.variables[variable] = descriptors
                    } else {
                        // 简单变量
                        val descriptor = lazyDeclarationResolver.resolveToDescriptor(variable) as VariableDescriptor
                        c.variables[variable] = listOf(descriptor)
                        registerTopLevelFqName(topLevelFqNames, variable, descriptor)
                    }
                }

                else -> {
                    // 未知变量类型，默认处理
                    val descriptor = lazyDeclarationResolver.resolveToDescriptor(variable) as VariableDescriptor
                    c.variables[variable] = listOf(descriptor)
                    registerTopLevelFqName(topLevelFqNames, variable, descriptor)
                }
            }
        }
    }

    private fun registerTopLevelFqName(
        topLevelFqNames: Multimap<FqName, CjElement>,
        declaration: CjNamedDeclaration,
        descriptor: DeclarationDescriptor
    ) {
        if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            val fqName = declaration.fqName
            if (fqName != null) {
                topLevelFqNames.put(fqName, declaration)
            }
        }
    }

    private fun createMainFunctionDescriptors(c: TopDownAnalysisContext, mainfunctions: List<CjMainFunction>) {
        // 检查同一包中的 main 函数重复声明
        val mainFunctionsByPackage = mainfunctions.groupBy { it.containingCjFile.packageFqName }

        for ((packageFqName, functionsInPackage) in mainFunctionsByPackage) {
            if (functionsInPackage.size > 1) {
                // 收集所有重复的 main 函数描述符
                val descriptors = functionsInPackage.map {
                    lazyDeclarationResolver.resolveToDescriptor(it) as SimpleFunctionDescriptor
                }

                // 为每个重复的 main 函数报告 REDECLARATION 错误
                for (function in functionsInPackage) {
                    trace.report(
                        org.cangnova.cangjie.diagnostics.infos.errors.REDECLARATION.on(
                            function.nameIdentifier ?: function,
                            descriptors
                        )
                    )
                }
            }
        }

        for (function in mainfunctions) {
            val simpleFunctionDescriptor =
                lazyDeclarationResolver.resolveToDescriptor(function) as SimpleFunctionDescriptor
            c.mainFunctions[function] = simpleFunctionDescriptor
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.annotations)
            for (parameterDescriptor in simpleFunctionDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }

    private fun createMacroDescriptors(c: TopDownAnalysisContext, macros: List<CjMacroDeclaration>) {
        for (macro in macros) {
            val macroDescriptor =
                lazyDeclarationResolver.resolveToDescriptor(macro) as MacroDescriptor
            c.macros[macro] = macroDescriptor
            ForceResolveUtil.forceResolveAllContents(macroDescriptor.annotations)
            for (parameterDescriptor in macroDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }

    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<CjNamedFunction>) {
        for (function in functions) {
            val simpleFunctionDescriptor = lazyDeclarationResolver.resolveToDescriptor(function)
                    as SimpleFunctionDescriptor
            c.functions[function] = simpleFunctionDescriptor
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.annotations)
            for (parameterDescriptor in simpleFunctionDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }
}

fun DescriptorVisibility.toAccessControlLevel(): Int {
    return when (this) {
        DescriptorVisibilities.PRIVATE, DescriptorVisibilities.INTERNAL -> 0
        DescriptorVisibilities.PROTECTED -> 1
        DescriptorVisibilities.PUBLIC -> 2
        else -> 0

    }
}
