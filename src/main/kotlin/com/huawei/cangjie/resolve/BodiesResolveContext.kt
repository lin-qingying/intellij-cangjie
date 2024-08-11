package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.expressions.ExpressionTypingContext

interface BodiesResolveContext {
    fun getDeclaringScope(declaration: CjDeclaration): LexicalScope?

    val files: Collection<CjFile>?


    val declaredClasses: MutableMap<CjClassOrStruct, ClassDescriptorWithResolutionScopes>

//    @get:Mutable
//    val anonymousInitializers: Map<Any?, Any?>?
//
//    @get:Mutable
//    val secondaryConstructors: Map<Any?, Any?>?
//


    val properties: Map<CjProperty, PropertyDescriptor>
//

    val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor>
//

    val typeAliases: MutableMap<CjTypeAlias, TypeAliasDescriptor>
//
//    @get:Mutable
//    val destructuringDeclarationEntries: Map<Any?, Any?>?
//
//    fun getDeclaringScope(declaration: KtDeclaration): LexicalScope?
//
  fun getLocalContext(): ExpressionTypingContext?

    fun getOuterDataFlowInfo(): DataFlowInfo

    fun     getTopDownAnalysisMode(): TopDownAnalysisMode
//    val outerDataFlowInfo: DataFlowInfo
//
//    val topDownAnalysisMode:  TopDownAnalysisMode
//
//    val localContext:  ExpressionTypingContext?
}
