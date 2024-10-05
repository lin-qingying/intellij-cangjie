package com.huawei.cangjie.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex
import com.huawei.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex
import com.huawei.cangjie.ide.stubindex.CangJieMainFunctionFqnNameIndex
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.lazy.*
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.util.BackgroundTaskUtil.executeOnPooledThread
import com.intellij.psi.PsiElement

class LazyTopDownAnalyzer(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val declarationResolver: DeclarationResolver,

    private val overrideResolver: OverrideResolver,
    private val overloadResolver: OverloadResolver,
    private val fileScopeProvider: FileScopeProvider,
    private val bodyResolver: BodyResolver,
    private val identifierChecker: IdentifierChecker,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
    private val mainFunctionResolver: MainFunctionResolver,

    private val declarationScopeProvider: DeclarationScopeProvider,
    private val filePreprocessor: FilePreprocessor,
    private val extendDescriptorResolver: ExtendDescriptorResolver,

    ) {


    fun analyzeDeclarations(
        topDownAnalysisMode: TopDownAnalysisMode,
        declarations: Collection<PsiElement>,
        outerDataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        localContext: ExpressionTypingContext? = null
    ): TopDownAnalysisContext {

        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo, declarationScopeProvider, localContext)


        val properties = mutableListOf<CjProperty>()
        val variables = mutableListOf<CjVariable>()
        val functions = mutableListOf<CjNamedFunction>()
        val mainFunctions = mutableListOf<CjMainFunction>()
        val typeAliases = mutableListOf<CjTypeAlias>()
        val destructuringDeclarations = mutableListOf<CjDestructuringDeclaration>()

        val reexports = mutableListOf<CjImportDirective>()

        val topLevelFqNames = HashMultimap.create<FqName, CjElement>()

        // 填充上下文
        for (declaration in declarations) {
            //  在内部使用‘VIRECTOR’变量
            var visitor: CjVisitorVoid? = null
            visitor = ExceptionWrappingCjVisitorVoid(object : CjVisitorVoid() {
                private fun registerDeclarations(declarations: List<CjDeclaration>) {
                    for (cjDeclaration in declarations) {
                        cjDeclaration.accept(visitor!!)
                    }
                }

                override fun visitProperty(property: CjProperty) {
                    properties.add(property)
                }

                override fun visitVariable(variable: CjVariable) {
                    variables.add(variable)
                }


                override fun visitTypeAlias(typeAlias: CjTypeAlias) {
                    typeAliases.add(typeAlias)
                }


                override fun visitDeclaration(dcl: CjDeclaration) {
                    throw IllegalArgumentException("Unsupported declaration: " + dcl + " " + dcl.text)
                }

                override fun visitImportDirective(importDirective: CjImportDirective) {
                    val importResolver = fileScopeProvider.getImportResolver(importDirective.getContainingCjFile())

//                    xTODO 修改该语句，添加重导出回调，返回包名映射
                    importResolver.forceResolveImport(importDirective)



                    if (importDirective.modifierVisibility != DescriptorVisibilities.PRIVATE) {
                        reexports.add(importDirective)
                    }
                }

                override fun visitTypeStatement(typeStatement: CjTypeStatement) {
//                    val location =
//                        if (typeStatement.isTopLevel()) CangJieLookupLocation(typeStatement) else NoLookupLocation.MATCH_RESOLVE_DECLARATION
                    val location = CangJieLookupLocation(typeStatement)

                    val descriptor =
                        lazyDeclarationResolver.getClassDescriptor(
                            typeStatement,
                            location
                        ) as ClassDescriptorWithResolutionScopes

                    c.declaredClasses[typeStatement] = descriptor
                    registerDeclarations(typeStatement.declarations)
                    registerTopLevelFqName(topLevelFqNames, typeStatement, descriptor)

                    checkTypeStatementDeclarations(typeStatement, descriptor)
                }

                override fun visitExtend(cjExtend: CjExtend) {

                    extendDescriptorResolver.check(c, cjExtend)
                    registerDeclarations(cjExtend.declarations)


                }

                override fun visitClass(cclass: CjClass) {
                    visitTypeStatement(cclass)
//                    registerPrimaryConstructorParameters(cclass)
                }

                // TODO 注册主构造函数
//                private fun registerPrimaryConstructorParameters(cclass: CjClass) {
//                    for (cjParameter in cclass.primaryConstructorParameters) {
//                        if (cjParameter.hasLetOrVar()) {
//                            c.primaryConstructorParameterProperties.put(
//                                cjParameter,
//                                lazyDeclarationResolver.resolveToDescriptor(cjParameter) as PropertyDescriptor
//                            )
//                        }
//                    }
//                }
                override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
                    c.primaryConstructors[constructor] =
                        lazyDeclarationResolver.resolveToDescriptor(constructor) as ClassConstructorDescriptor
                }

                override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
                    c.secondaryConstructors[constructor] =
                        lazyDeclarationResolver.resolveToDescriptor(constructor) as ClassConstructorDescriptor
                }

                private fun checkTypeStatementDeclarations(
                    typeStatement: CjTypeStatement,
                    classDescriptor: ClassDescriptor
                ) {
//                    var companionObjectAlreadyFound = false
                    for (cjDeclaration in typeStatement.declarations) {
                        if (cjDeclaration is CjSecondaryConstructor) {
                            /*  if (DescriptorUtils.isSingletonOrAnonymousObject(classDescriptor)) {
                                  trace.report(CONSTRUCTOR_IN_OBJECT.on(cjDeclaration))
                              } else */if (classDescriptor.kind == ClassKind.INTERFACE) {
                                trace.report(CONSTRUCTOR_IN_INTERFACE.on(cjDeclaration))
                            }
                        }
                    }
                }

                override fun visitCjFile(file: CjFile) {
                    filePreprocessor.preprocessFile(file)
                    registerDeclarations(file.declarations)
                    val packageDirective = file.packageDirective
                    assert(packageDirective != null) { "No package in a non-script file: $file" }
                    packageDirective?.accept(this)
                    c.addFile(file)
                    if (packageDirective != null) topLevelFqNames.put(file.packageFqName, packageDirective)
                }

                override fun visitNamedFunction(function: CjNamedFunction) {
                    functions.add(function)


                }

                override fun visitMainFunction(cjMainFunction: CjMainFunction) {
                    mainFunctions.add(cjMainFunction)

                }

                override fun visitPackageDirective(directive: CjPackageDirective) {
                    directive.packageNames.forEach { identifierChecker.checkIdentifier(it, trace) }
                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)



                    checkPackagelevel(directive)
                }


            })

            declaration.accept(visitor)
        }
        createFunctionDescriptors(c, functions)
        createMainFunctionDescriptors(c, mainFunctions)

        createPropertyDescriptors(c, topLevelFqNames, properties)

        createVariableDescriptors(c, topLevelFqNames, variables)
        createTypeAliasDescriptors(c, topLevelFqNames, typeAliases)
        createReexportsDescriptors(c, topLevelFqNames, reexports)


        resolveAllHeadersInClasses(c)
        declarationResolver.checkRedeclarationsInPackages(topLevelDescriptorProvider, topLevelFqNames)
        declarationResolver.checkRedeclarations(c)
//        declarationResolver.resolveAnnotationsOnFiles(c, fileScopeProvider)

        overrideResolver.check(c)


        overloadResolver.checkOverloads(c)

        bodyResolver.resolveBodies(c)

        mainFunctionResolver.check(c)
        resolveImportsInAllFiles(c)

        return c

    }


    fun resolveImportsInFile(file: CjFile) {
        checkForCycles(file)
        fileScopeProvider.getImportResolver(file).forceResolveNonDefaultImports()
    }


    /**
     * 该方法检查包等级，耗时操作
     */
    private fun checkPackagelevel(directive: CjPackageDirective) {
        executeOnPooledThread(object : Disposable {
            override fun dispose() {

            }
        }) {

            runReadAction {
                val currentLevel = toAccessControlLevel(directive.modifierVisibility)
                if (currentLevel == 0) {
                    return@runReadAction
                }
                if (directive.fqName.isModuleName) {
                    return@runReadAction
                }
//                获取父包索引，检查等级
                val parentPackageFqName = directive.fqName.parent()
                CangJieExactPackagesIndex.get(parentPackageFqName.asString(), directive.project).forEach { file ->

                    file.packageDirective?.modifierVisibility?.let {
                        if (toAccessControlLevel(it) < currentLevel) {

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

    //    由于该操作过于耗时，导致这里还没检查完成，整个文件已经分析完成了，所以先返回了分析结果，导致错误无法被报告
//    这里应该写一个单独的分析线程，并报告错误，但是现在就先这样吧
    //    检查循环导入
    private fun checkForCycles(file: CjFile) {
//        操作是非常耗时的操作，在后台执行
//        executeOnPooledThread(object : Disposable {
//            override fun dispose() {
//
//            }
//        }) {


//            流程
//            1 获取该包所有导入语句
//            2 获取被导入语句的包的导入语句
//            3 检查是否包含该包名称
        runReadAction {
            val packageFqname = file.packageFqName

            for (importDirective in file.importDirectives) {
                val result =
                    importDirective.importedFqName
                        ?.let { CangJieImportFqNameForPackageNameIndex.contains(packageFqname, it, file.project) }


                if (result != null) {
                    if (result.first) {

                        trace.report(CYCLIC_IMPORT.on(importDirective, packageFqname, result.second))


                    }
                }


            }
        }


//        }

    }

    private fun resolveImportsInAllFiles(c: TopDownAnalysisContext) {
        for (file in c.files.map { it.getContainingCjFile() }) {
            resolveImportsInFile(file)
        }
    }

    private fun resolveAllHeadersInClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.allClasses) {
            when (classDescriptor) {
                is LazyClassDescriptor ->
                    classDescriptor.resolveMemberHeaders()

                is LazyExtendClassDescriptor ->
                    classDescriptor.resolveMemberHeaders()
            }
        }
    }

    private fun createReexportsDescriptors(
        c: TopDownAnalysisContext,
        topLevelFqNames: Multimap<FqName, CjElement>,
        reexports: List<CjImportDirective>
    ) {
//        for (reexport in reexports) {
//            val descriptor = lazyDeclarationResolver.resolveToDescriptor(typeAlias) as TypeAliasDescriptor
//
//            c.reexports[typeAlias] = descriptor
//            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
//            registerTopLevelFqName(topLevelFqNames, typeAlias, descriptor)
//        }
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
        variables: MutableList<CjVariable>
    ) {
        for (variable in variables) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(variable) as VariableDescriptor

            c.variables[variable] = descriptor
            registerTopLevelFqName(topLevelFqNames, variable, descriptor)
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

    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<CjNamedFunction>) {
        for (function in functions) {
            val simpleFunctionDescriptor =
                lazyDeclarationResolver.resolveToDescriptor(function) as SimpleFunctionDescriptor
            c.functions[function] = simpleFunctionDescriptor
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.annotations)
            for (parameterDescriptor in simpleFunctionDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }
}

fun toAccessControlLevel(visiblity: DescriptorVisibility): Int {
    return when (visiblity) {
        DescriptorVisibilities.PRIVATE, DescriptorVisibilities.INTERNAL -> 0
        DescriptorVisibilities.PROTECTED -> 1
        DescriptorVisibilities.PUBLIC -> 2
        else -> 0

    }

}
