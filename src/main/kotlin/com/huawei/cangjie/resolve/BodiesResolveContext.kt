package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjNamedFunction
import com.huawei.cangjie.resolve.scopes.LexicalScope

interface BodiesResolveContext {
    fun getDeclaringScope(declaration: CjDeclaration): LexicalScope?

    val files: Collection<CjFile>?

//    @get:Mutable
//    val declaredClasses: Map<KtClassOrObject?, ClassDescriptorWithResolutionScopes?>?
//
//    @get:Mutable
//    val anonymousInitializers: Map<Any?, Any?>?
//
//    @get:Mutable
//    val secondaryConstructors: Map<Any?, Any?>?
//
//    @get:Mutable
//    val scripts: Map<Any?, Any?>?
//
//    @get:Mutable
//    val properties: Map<Any?, Any?>?
//

    val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor>
//
//    @get:Mutable
//    val typeAliases: Map<Any?, Any?>?
//
//    @get:Mutable
//    val destructuringDeclarationEntries: Map<Any?, Any?>?
//
//    fun getDeclaringScope(declaration: KtDeclaration): LexicalScope?
//
//    val outerDataFlowInfo: DataFlowInfo
//
//    val topDownAnalysisMode:  TopDownAnalysisMode
//
//    val localContext:  ExpressionTypingContext?
}
