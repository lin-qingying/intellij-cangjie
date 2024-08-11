package com.huawei.cangjie.resolve;

import com.google.common.collect.Maps;
import com.huawei.cangjie.descriptors.ClassDescriptorWithResolutionScopes;
import com.huawei.cangjie.descriptors.PropertyDescriptor;
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor;
import com.huawei.cangjie.descriptors.TypeAliasDescriptor;
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

    private final Map<CjNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();

    private final Map<CjProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<CjParameter, PropertyDescriptor> primaryConstructorParameterProperties = new HashMap<>();
    private final Map<CjTypeAlias, TypeAliasDescriptor> typeAliases = Maps.newLinkedHashMap();
    private final DataFlowInfo outerDataFlowInfo;
    private final Map<CjClassOrStruct, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();

    private final TopDownAnalysisMode topDownAnalysisMode;
    private final DeclarationScopeProvider declarationScopeProvider;
    private final ExpressionTypingContext localContext;

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
    public void addFile(@NotNull CjFile file) {
        files.add(file);
    }

    @Nullable
    @Override
    public LexicalScope getDeclaringScope(@NotNull CjDeclaration declaration) {
        return declarationScopeProvider.getResolutionScopeForDeclaration(declaration);
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


    @Nullable
    @Override
    public Collection<CjFile> getFiles() {
        return files;

    }

    @NotNull
    @Override
    public Map<CjNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;

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
    public Map<CjClassOrStruct, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
        return classes;

    }

    @NotNull
    @Override
    public Map<CjProperty, PropertyDescriptor> getProperties() {
        return properties;

    }

    @NotNull
    @Override
    public Map<CjTypeAlias, TypeAliasDescriptor> getTypeAliases() {
        return typeAliases;

    }
}
