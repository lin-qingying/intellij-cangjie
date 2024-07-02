package com.huawei.cangjie.resolve

import com.google.common.collect.HashMultimap
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.lazy.DeclarationScopeProvider
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.lazy.LazyDeclarationResolver
import com.intellij.psi.PsiElement

class LazyTopDownAnalyzer(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val bodyResolver: BodyResolver,

//    private val declarationScopeProvider: DeclarationScopeProvider,
    private val filePreprocessor: FilePreprocessor
) {


    fun analyzeDeclarations(
        topDownAnalysisMode: TopDownAnalysisMode,
        declarations: Collection<PsiElement>,
        outerDataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
//        localContext: ExpressionTypingContext? = null
    ): TopDownAnalysisContext{

        val c = TopDownAnalysisContext(topDownAnalysisMode, outerDataFlowInfo /*, declarationScopeProvider,localContext*/)

//        val variable = ArrayList<CjVariable>()

//        val properties = ArrayList<CjProperty>()
        val functions = ArrayList<CjNamedFunction>()
//        val typeAliases = ArrayList<CjTypeAlias>()
//        val destructuringDeclarations = ArrayList<CjDestructuringDeclaration>()

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


                override fun visitCjFile(file: CjFile) {
                    filePreprocessor.preprocessFile(file)
                    registerDeclarations(file.declarations)
                    val packageDirective = file.packageDirective
                    assert(  packageDirective != null) { "No package in a non-script file: $file" }
                    packageDirective?.accept(this)
                    c.addFile(file)
                    if (packageDirective != null) topLevelFqNames.put(file.packageFqName, packageDirective)
                }
                override fun visitNamedFunction(function: CjNamedFunction) {
                    functions.add(function)
                }

//                override fun visitPackageDirective(directive: CjPackageDirective) {
//                    directive.packageNames.forEach { identifierChecker.checkIdentifier(it, trace) }
//                    qualifiedExpressionResolver.resolvePackageHeader(directive, moduleDescriptor, trace)
//                }
            })

            declaration.accept(visitor)
        }
        createFunctionDescriptors(c, functions)

        bodyResolver.resolveBodies(c)

        return c

    }


    private fun createFunctionDescriptors(c: TopDownAnalysisContext, functions: List<CjNamedFunction>) {
        for (function in functions) {
            val simpleFunctionDescriptor = lazyDeclarationResolver.resolveToDescriptor(function) as SimpleFunctionDescriptor
            c.functions[function] = simpleFunctionDescriptor
            ForceResolveUtil.forceResolveAllContents(simpleFunctionDescriptor.annotations)
            for (parameterDescriptor in simpleFunctionDescriptor.valueParameters) {
                ForceResolveUtil.forceResolveAllContents(parameterDescriptor.annotations)
            }
        }
    }
}