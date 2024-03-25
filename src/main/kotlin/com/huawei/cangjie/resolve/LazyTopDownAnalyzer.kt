package com.huawei.cangjie.resolve

import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.psi.CjVisitorVoid
import com.intellij.psi.PsiElement

class LazyTopDownAnalyzer {


    fun analyzeDeclarations(
        topDownAnalysisMode: TopDownAnalysisMode,
        declarations: Collection<PsiElement>,
//        outerDataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
//        localContext: ExpressionTypingContext? = null
    ): TopDownAnalysisContext{


//        val variable = ArrayList<CjVariable>()

//        val properties = ArrayList<CjProperty>()
        val functions = ArrayList<CjNamedFunction>()
//        val typeAliases = ArrayList<CjTypeAlias>()
//        val destructuringDeclarations = ArrayList<CjDestructuringDeclaration>()


        // 填充上下文
        for (declaration in declarations) {
            //  在内部使用‘VIRECTOR’变量
            var visitor: CjVisitorVoid? = null
            visitor = ExceptionWrappingCjVisitorVoid(object : CjVisitorVoid() {
                override fun visitNamedFunction(function: CjNamedFunction) {
                    functions.add(function)
                }
            })

            declaration.accept(visitor)
        }

        TODO()
    }
}