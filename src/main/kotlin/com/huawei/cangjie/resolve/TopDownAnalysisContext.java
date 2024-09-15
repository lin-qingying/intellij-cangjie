package com.huawei.cangjie.resolve;

import com.google.common.collect.Maps;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.lazy.DeclarationScopeProvider;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.types.expressions.ExpressionTypingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TopDownAnalysisContext implements BodiesResolveContext {
    private final Set<CjFile> files = new LinkedHashSet<>();
    private final Map<CjMainFunction, SimpleFunctionDescriptor> mainFunctions = Maps.newLinkedHashMap();

    private final Map<CjNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<CjVariable, VariableDescriptor> variables = Maps.newLinkedHashMap();

    private final Map<CjProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
//    private final Map<CjParameter, PropertyDescriptor> primaryConstructorParameterProperties = new HashMap<>();
    private final Map<CjTypeAlias, TypeAliasDescriptor> typeAliases = Maps.newLinkedHashMap();
    private final DataFlowInfo outerDataFlowInfo;
    private final Map<CjTypeStatement, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();
    private final Map<CjSecondaryConstructor, ClassConstructorDescriptor> secondaryConstructors = Maps.newLinkedHashMap();
    private final Map<CjPrimaryConstructor, ClassConstructorDescriptor> primaryConstructor = Maps.newLinkedHashMap();

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
    public @NotNull Map<CjVariable, VariableDescriptor> getVariables() {
        return variables;
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
