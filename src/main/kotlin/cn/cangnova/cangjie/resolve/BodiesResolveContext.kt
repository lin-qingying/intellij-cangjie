/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext

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
