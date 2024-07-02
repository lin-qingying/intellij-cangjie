package com.huawei.cangjie.resolve;

import com.google.common.collect.Maps;
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor;
import com.huawei.cangjie.psi.CjDeclaration;
import com.huawei.cangjie.psi.CjFile;
import com.huawei.cangjie.psi.CjNamedFunction;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.lazy.DeclarationScopeProvider;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TopDownAnalysisContext implements BodiesResolveContext {
    private final Set<CjFile> files = new LinkedHashSet<>();

    private final Map<CjNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final DataFlowInfo outerDataFlowInfo;

    private final TopDownAnalysisMode topDownAnalysisMode;
//    private final DeclarationScopeProvider declarationScopeProvider;
//    private final ExpressionTypingContext localContext;

    public TopDownAnalysisContext(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull DataFlowInfo outerDataFlowInfo
//            @NotNull DeclarationScopeProvider declarationScopeProvider
    ) {
        this.topDownAnalysisMode = topDownAnalysisMode;
        this.outerDataFlowInfo = outerDataFlowInfo;
//        this.declarationScopeProvider = declarationScopeProvider;
//        this.localContext = null;
    }
    public void addFile(@NotNull CjFile file) {
        files.add(file);
    }


//    public TopDownAnalysisContext(
//            @NotNull TopDownAnalysisMode topDownAnalysisMode,
//            @NotNull DataFlowInfo outerDataFlowInfo,
//            @NotNull DeclarationScopeProvider declarationScopeProvider,
//            @Nullable ExpressionTypingContext localContext
//    ) {
//        this.topDownAnalysisMode = topDownAnalysisMode;
//        this.outerDataFlowInfo = outerDataFlowInfo;
//        this.declarationScopeProvider = declarationScopeProvider;
//        this.localContext = localContext;
//    }


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

    @Nullable
    @Override
    public LexicalScope getDeclaringScope(@NotNull CjDeclaration declaration) {
//        return declarationScopeProvider.getResolutionScopeForDeclaration(declaration);

        return null;
    }
}
