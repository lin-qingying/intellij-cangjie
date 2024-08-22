package com.huawei.cangjie.resolve

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.Errors.*
import com.huawei.cangjie.ide.stubindex.CangJieExactPackagesIndex

import com.huawei.cangjie.ide.stubindex.CangJieImportFqNameForPackageNameIndex
import com.huawei.cangjie.incremental.CangJieLookupLocation
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.lazy.DeclarationScopeProvider
import com.huawei.cangjie.resolve.lazy.FileScopeProvider
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.lazy.LazyDeclarationResolver
import com.huawei.cangjie.resolve.lazy.descriptors.LazyClassDescriptor
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.util.BackgroundTaskUtil.executeOnPooledThread
import com.intellij.psi.PsiElement

class LazyTopDownAnalyzer(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val overrideResolver: OverrideResolver,
    private val overloadResolver: OverloadResolver,
    private val fileScopeProvider: FileScopeProvider,

    private val bodyResolver: BodyResolver,
    private val identifierChecker: IdentifierChecker,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val moduleDescriptor: ModuleDescriptor,

    private val declarationScopeProvider: DeclarationScopeProvider,
    private val filePreprocessor: FilePreprocessor
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

//                    TODO 修改该语句，添加重导出回调，返回包名映射
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

                override fun visitClass(cclass: CjClass) {
                    visitTypeStatement(cclass)
//                    registerPrimaryConstructorParameters(cclass)
                }
// TODO 注册主构造函数
//                private fun registerPrimaryConstructorParameters(klass: CjClass) {
//                    for (cjParameter in klass.primaryConstructorParameters) {
//                        if (cjParameter.hasValOrVar()) {
//                            c.primaryConstructorParameterProperties.put(
//                                cjParameter,
//                                lazyDeclarationResolver.resolveToDescriptor(cjParameter) as PropertyDescriptor
//                            )
//                        }
//                    }
//                }

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

                override fun visitPackageDirective(directive: CjPackageDirective) {
                    directive.packageNames.forEach { identifierChecker.checkIdentifier(it, trace) }
                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)



                    checkPackagelevel(directive)
                }
            })

            declaration.accept(visitor)
        }
        createFunctionDescriptors(c, functions)
//        createPropertyDescriptors(c, topLevelFqNames, properties)

        createVariableDescriptors(c, topLevelFqNames, variables)
        createTypeAliasDescriptors(c, topLevelFqNames, typeAliases)
        createReexportsDescriptors(c, topLevelFqNames, reexports)


        resolveAllHeadersInClasses(c)

        overrideResolver.check(c)


        overloadResolver.checkOverloads(c)

        bodyResolver.resolveBodies(c)
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

    //    检查循环导入
    private fun checkForCycles(file: CjFile) {
//        操作是非常耗时的操作，在后台执行
        executeOnPooledThread(object : Disposable {
            override fun dispose() {

            }
        }) {


//            流程
//            1 获取该包所有导入语句
//            2 获取被导入语句的包的导入语句
//            3 检查是否包含该包名称
            runReadAction {
                val packageFqname = file.packageFqName

                for (importDirective in file.importDirectives) {
                    val result =
                        importDirective.importedFqName?.asString()
                            ?.let { CangJieImportFqNameForPackageNameIndex.contains(packageFqname, it, file.project) }


                    if (result != null) {
                        if (result.first) {

                            importDirective.importedFqName?.let {
                                trace.report(CYCLIC_IMPORT.on(importDirective, packageFqname, it))
                            }


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
        for (classDescriptor in c.allClasses) {
            (classDescriptor as LazyClassDescriptor).resolveMemberHeaders()
        }
    }

    private fun createReexportsDescriptors(
        c: TopDownAnalysisContext,
        topLevelFqNames: Multimap<FqName, CjElement>,
        reexports: List<CjImportDirective>
    ) {
        for (reexport in reexports) {
//            val descriptor = lazyDeclarationResolver.resolveToDescriptor(typeAlias) as TypeAliasDescriptor
//
//            c.reexports[typeAlias] = descriptor
//            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
//            registerTopLevelFqName(topLevelFqNames, typeAlias, descriptor)
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
        variables: MutableList<CjProperty>
    ) {
        for (property in variables) {
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
        for (property in variables) {
            val descriptor = lazyDeclarationResolver.resolveToDescriptor(property) as VariableDescriptor

            c.variables[property] = descriptor
            registerTopLevelFqName(topLevelFqNames, property, descriptor)
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
