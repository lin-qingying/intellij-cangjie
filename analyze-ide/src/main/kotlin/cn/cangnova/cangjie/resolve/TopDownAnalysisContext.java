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

package cn.cangnova.cangjie.resolve;

import com.google.common.collect.Maps;
import cn.cangnova.cangjie.descriptors.*;
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor;
import cn.cangnova.cangjie.psi.*;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.lazy.DeclarationScopeProvider;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import cn.cangnova.cangjie.types.expressions.ExpressionTypingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TopDownAnalysisContext implements BodiesResolveContext {
    private final Set<CjFile> files = new LinkedHashSet<>();
    private final Map<CjMainFunction, SimpleFunctionDescriptor> mainFunctions = Maps.newLinkedHashMap();
    private final Map<CjMacroDeclaration, MacroDescriptor> macros = Maps.newLinkedHashMap();

    private final Map<CjNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<CjVariable, VariableDescriptor> variables = Maps.newLinkedHashMap();
    private final Map<CjVariable, List<VariableDescriptor>> variablesByPattern = Maps.newLinkedHashMap();

    private final Map<CjProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    //    private final Map<CjParameter, PropertyDescriptor> primaryConstructorParameterProperties = new HashMap<>();
    private final Map<CjTypeAlias, TypeAliasDescriptor> typeAliases = Maps.newLinkedHashMap();
    private final DataFlowInfo outerDataFlowInfo;
    private final Map<CjTypeStatement, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();
    private final Map<CjSecondaryConstructor, ClassConstructorDescriptor> secondaryConstructors = Maps.newLinkedHashMap();
    private final Map<CjPrimaryConstructor, ClassConstructorDescriptor> primaryConstructor = Maps.newLinkedHashMap();
    private final Map<CjEndSecondaryConstructor, ClassConstructorDescriptor> endSecondaryConstructors = Maps.newLinkedHashMap();

    private final TopDownAnalysisMode topDownAnalysisMode;
    private final DeclarationScopeProvider declarationScopeProvider;
    private final ExpressionTypingContext localContext;
    private Map<CjCallableDeclaration, CallableMemberDescriptor> members = null;

    public TopDownAnalysisContext(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull DeclarationScopeProvider declarationScopeProvider
    ) {
        this.topDownAnalysisMode = topDownAnalysisMode;
        this.outerDataFlowInfo = outerDataFlowInfo;
        this.declarationScopeProvider = declarationScopeProvider;
        this.localContext = null;
    }

    public TopDownAnalysisContext(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull DeclarationScopeProvider declarationScopeProvider,
            @Nullable ExpressionTypingContext localContext
    ) {
        this.topDownAnalysisMode = topDownAnalysisMode;
        this.outerDataFlowInfo = outerDataFlowInfo;
        this.declarationScopeProvider = declarationScopeProvider;
        this.localContext = localContext;
    }

    public void addFile(@NotNull CjFile file) {
        files.add(file);
    }

    @NotNull
    public Map<CjCallableDeclaration, CallableMemberDescriptor> getMembers() {
        if (members == null) {
            members = Maps.newLinkedHashMap();
            members.putAll(functions);
            members.putAll(properties);
//            members.putAll(primaryConstructorParameterProperties);
        }
        return members;
    }

    @Nullable
    @Override
    public LexicalScope getDeclaringScope(@NotNull CjDeclaration declaration) {
        return declarationScopeProvider.getResolutionScopeForDeclaration(declaration);
    }
//    public Map<CjParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
//        return primaryConstructorParameterProperties;
//    }

    @NotNull
    @Override
    public Collection<CjFile> getFiles() {
        return files;

    }

    @Override
    public @NotNull Map<CjPrimaryConstructor, ClassConstructorDescriptor> getPrimaryConstructors() {
        return primaryConstructor;
    }

    @Override
    public @NotNull Map<CjSecondaryConstructor, ClassConstructorDescriptor> getSecondaryConstructors() {
        return secondaryConstructors;
    }

    @Override
    @NotNull
    public Map<CjEndSecondaryConstructor, ClassConstructorDescriptor> getEndSecondaryConstructors() {
        return endSecondaryConstructors;
    }

    @Override
    public @NotNull Map<CjVariable, List<VariableDescriptor>> getVariablesByPattern() {
        return variablesByPattern;
    }

    @Override
    public @NotNull Map<CjVariable, VariableDescriptor> getVariables() {
        return variables;
    }

    @NotNull
    @Override
    public Map<CjMacroDeclaration, MacroDescriptor> getMacros() {
        return macros;
    }

    @NotNull
    @Override
    public Map<CjNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    @NotNull
    @Override
    public Map<CjMainFunction, SimpleFunctionDescriptor> getMainFunctions() {
        return mainFunctions;
    }


    @NotNull
    @Override
    public TopDownAnalysisMode getTopDownAnalysisMode() {
        return topDownAnalysisMode;

    }

    @Override
    @Nullable
    public ExpressionTypingContext getLocalContext() {
        return localContext;
    }

    @NotNull
    @Override
    public DataFlowInfo getOuterDataFlowInfo() {
        return outerDataFlowInfo;

    }

    @NotNull
    @Override
    public Map<CjTypeStatement, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
        return classes;

    }

    @NotNull
    @Override
    public Map<CjProperty, PropertyDescriptor> getProperties() {
        return properties;

    }

    @NotNull
    public Collection<ClassDescriptorWithResolutionScopes> getAllClasses() {
        return getDeclaredClasses().values();
//        return CollectionsKt.plus(getDeclaredClasses().values(), getScripts().values());
    }

    @NotNull
    @Override
    public Map<CjTypeAlias, TypeAliasDescriptor> getTypeAliases() {
        return typeAliases;

    }
}
