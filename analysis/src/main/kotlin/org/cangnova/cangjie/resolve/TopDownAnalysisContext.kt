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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BodiesResolveContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.lazy.DeclarationScopeProvider
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

class TopDownAnalysisContext(
    override val topDownAnalysisMode: TopDownAnalysisMode,
    override val outerDataFlowInfo: DataFlowInfo,
    private val declarationScopeProvider: DeclarationScopeProvider,
    override val localContext: ExpressionTypingContext? = null
) : BodiesResolveContext {

    override val files = LinkedHashSet<CjFile>()
    override val mainFunctions = LinkedHashMap<CjMainFunction, SimpleFunctionDescriptor>()
    override val macros = LinkedHashMap<CjMacroDeclaration, MacroDescriptor>()
    override val functions = LinkedHashMap<CjNamedFunction, SimpleFunctionDescriptor>()
    override val variables: MutableMap<CjVariable<*>, List<VariableDescriptor>> = LinkedHashMap<CjVariable<*>, List<VariableDescriptor>>()
    override val properties = LinkedHashMap<CjProperty, PropertyDescriptor>()
    override val typeAliases = LinkedHashMap<CjTypeAlias, TypeAliasDescriptor>()
    override val declaredClasses = LinkedHashMap<CjTypeStatement, ClassDescriptorWithResolutionScopes>()
    override val secondaryConstructors = LinkedHashMap<CjSecondaryConstructor, ClassConstructorDescriptor>()

    override val primaryConstructors = LinkedHashMap<CjPrimaryConstructor, ClassConstructorDescriptor>()
    override val endSecondaryConstructors = LinkedHashMap<CjEndSecondaryConstructor, ClassConstructorDescriptor>()


    fun addFile(file: CjFile) {
        files.add(file)
    }

    val members get() = functions + properties


    override fun getDeclaringScope(declaration: CjDeclaration): LexicalScope =
        declarationScopeProvider.getResolutionScopeForDeclaration(declaration)



}
