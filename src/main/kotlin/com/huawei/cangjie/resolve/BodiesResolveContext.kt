package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.types.expressions.ExpressionTypingContext

interface BodiesResolveContext {
    fun getDeclaringScope(declaration: CjDeclaration): LexicalScope?

    val files: Collection<CjFile>
    val primaryConstructors: MutableMap<CjPrimaryConstructor, ClassConstructorDescriptor>

    val secondaryConstructors: MutableMap<CjSecondaryConstructor, ClassConstructorDescriptor>

    val declaredClasses: MutableMap<CjTypeStatement, ClassDescriptorWithResolutionScopes>

//    @get:Mutable
//    val anonymousInitializers: Map<Any?, Any?>?
//
//    @get:Mutable
//    val secondaryConstructors: Map<Any?, Any?>?
//


    val properties: MutableMap<CjProperty, PropertyDescriptor>
    val variables: MutableMap<CjVariable, VariableDescriptor>

    val mainFunctions: MutableMap<CjMainFunction, SimpleFunctionDescriptor>

    val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor>


    val typeAliases: MutableMap<CjTypeAlias, TypeAliasDescriptor>

    //
//    @get:Mutable
//    val destructuringDeclarationEntries: Map<Any?, Any?>?
//
//    fun getDeclaringScope(declaration: CjDeclaration): LexicalScope?
//
    fun getLocalContext(): ExpressionTypingContext?

    fun getOuterDataFlowInfo(): DataFlowInfo

    fun getTopDownAnalysisMode(): TopDownAnalysisMode
//    val outerDataFlowInfo: DataFlowInfo
//
//    val topDownAnalysisMode:  TopDownAnalysisMode
//
//    val localContext:  ExpressionTypingContext?
}
