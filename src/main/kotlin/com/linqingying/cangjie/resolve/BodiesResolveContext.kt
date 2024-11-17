package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext

interface BodiesResolveContext {
    fun getDeclaringScope(declaration: CjDeclaration): LexicalScope?

    val files: Collection<CjFile>
    val primaryConstructors: MutableMap<CjPrimaryConstructor, ClassConstructorDescriptor>
    val endSecondaryConstructors: MutableMap<CjEndSecondaryConstructor, ClassConstructorDescriptor>

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
    val variablesByPattern: MutableMap<CjVariable, List<VariableDescriptor>> get() = hashMapOf()

    val mainFunctions: MutableMap<CjMainFunction, SimpleFunctionDescriptor>

    val functions: MutableMap<CjNamedFunction, SimpleFunctionDescriptor>

    val macros: MutableMap<CjMacroDeclaration, MacroDescriptor>
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
