package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.builtins.PlatformToCangJieClassMapper;
import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.contracts.EffectSystem;
import com.linqingying.cangjie.descriptors.ModuleDescriptor;
import com.linqingying.cangjie.extensions.TypeResolutionInterceptor;
import com.linqingying.cangjie.resolve.*;
import com.linqingying.cangjie.resolve.calls.CallExpressionResolver;
import com.linqingying.cangjie.resolve.calls.CallResolver;
import com.linqingying.cangjie.resolve.calls.checkers.AssignmentChecker;
import com.linqingying.cangjie.resolve.calls.checkers.CallChecker;
import com.linqingying.cangjie.resolve.calls.checkers.RttiExpressionChecker;
import com.linqingying.cangjie.resolve.calls.model.CangJieCallComponents;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver;
import com.linqingying.cangjie.types.checker.NewCangJieTypeChecker;

import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

public class ExpressionTypingComponents {
    public CallExpressionResolver callExpressionResolver;
    public ConstantExpressionEvaluator constantExpressionEvaluator;
    public LanguageVersionSettings languageVersionSettings;
    public ModuleDescriptor moduleDescriptor;
    public FunctionDescriptorResolver functionDescriptorResolver;
    public CollectionLiteralResolver collectionLiteralResolver;
    public PlatformToCangJieClassMapper platformToCangJieClassMapper;
    public Iterable<CallChecker> callCheckers;
    public DestructuringDeclarationResolver destructuringDeclarationResolver;

    public RangeLiteralResolver rangeLiteralResolver;
    public SpawnExpressionResolver spawnExpressionResolver;
    public TypeResolver typeResolver;
    public Iterable<RttiExpressionChecker> rttiExpressionCheckers;
    public UnsafeExpressionResolver unsafeExpressionResolver;
    public FunctionReturnResolver functionReturnResolver;
    public CallResolver callResolver;
    public Iterable<AssignmentChecker> assignmentCheckers;
    public MissingSupertypesResolver missingSupertypesResolver;
    public ModifiersChecker modifiersChecker;
    public ControlStructureTypingUtils controlStructureTypingUtils;
    public ForLoopConventionsChecker forLoopConventionsChecker;
    public DescriptorResolver descriptorResolver;

    public LocalVariableResolver localVariableResolver;
    public TypeResolutionInterceptor typeResolutionInterceptor;
    public IdentifierChecker identifierChecker;

    public CangJieBuiltIns builtIns;
    public DataFlowValueFactory dataFlowValueFactory;
    public DeprecationResolver deprecationResolver;

    public ExpressionTypingServices expressionTypingServices;
    public DataFlowAnalyzer dataFlowAnalyzer;

    public NewCangJieTypeChecker cangjieTypeChecker;
    public CangJieCallComponents callComponents;
    public ValueParameterResolver valueParameterResolver;
    public EffectSystem effectSystem;

    public OverloadChecker overloadChecker;
    @Inject
    public void setEffectSystem(@NotNull EffectSystem effectSystem) {
        this.effectSystem = effectSystem;
    }

    @Inject
    public void setPlatformToCangJieClassMap(@NotNull PlatformToCangJieClassMapper platformToCangJieClassMapper) {
        this.platformToCangJieClassMapper = platformToCangJieClassMapper;
    }
    @Inject
    public void setUnsafeExpressionResolver(@NotNull UnsafeExpressionResolver unsafeExpressionResolver) {
        this.unsafeExpressionResolver = unsafeExpressionResolver;
    }
    @Inject
    public void setCallCheckers(@NotNull Iterable<CallChecker> callCheckers) {
        this.callCheckers = callCheckers;
    }
    @Inject
    public void setDestructuringDeclarationResolver(DestructuringDeclarationResolver destructuringDeclarationResolver) {
        this.destructuringDeclarationResolver = destructuringDeclarationResolver;
    }

    @Inject
    public void setRttiExpressionCheckers(@NotNull Iterable<RttiExpressionChecker> rttiExpressionCheckers) {
        this.rttiExpressionCheckers = rttiExpressionCheckers;
    }

    @Inject
    public void setSpawnExpressionResolver(@NotNull SpawnExpressionResolver spawnExpressionResolver) {
        this.spawnExpressionResolver = spawnExpressionResolver;
    }

    @Inject
    public void setFunctionReturnResolver(@NotNull FunctionReturnResolver functionReturnResolver) {
        this.functionReturnResolver = functionReturnResolver;
    }

    @Inject
    public void setMissingSupertypesResolver(@NotNull MissingSupertypesResolver missingSupertypesResolver) {
        this.missingSupertypesResolver = missingSupertypesResolver;
    }

    //    @Inject
//    public void setDeclarationsCheckerBuilder(@NotNull DeclarationsCheckerBuilder declarationsCheckerBuilder) {
//        this.declarationsCheckerBuilder = declarationsCheckerBuilder;
//    }
    @Inject
    public void setForLoopConventionsChecker(@NotNull ForLoopConventionsChecker forLoopConventionsChecker) {
        this.forLoopConventionsChecker = forLoopConventionsChecker;
    }

    @Inject
    public void setIdentifierChecker(@NotNull IdentifierChecker identifierChecker) {
        this.identifierChecker = identifierChecker;
    }

    @Inject
    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setValueParameterResolver(ValueParameterResolver valueParameterResolver) {
        this.valueParameterResolver = valueParameterResolver;
    }

    @Inject
    public void setControlStructureTypingUtils(@NotNull ControlStructureTypingUtils controlStructureTypingUtils) {
        this.controlStructureTypingUtils = controlStructureTypingUtils;
    }

    @Inject
    public void setDeprecationResolver(DeprecationResolver deprecationResolver) {
        this.deprecationResolver = deprecationResolver;
    }

    @Inject
    public void setModifiersChecker(@NotNull ModifiersChecker modifiersChecker) {
        this.modifiersChecker = modifiersChecker;
    }

    @Inject
    public void setDataFlowAnalyzer(@NotNull DataFlowAnalyzer dataFlowAnalyzer) {
        this.dataFlowAnalyzer = dataFlowAnalyzer;
    }

    @Inject
    public void setRangeLiteralResolver(RangeLiteralResolver rangeLiteralResolver) {
        this.rangeLiteralResolver = rangeLiteralResolver;
    }

    @Inject
    public void setCollectionLiteralResolver(CollectionLiteralResolver collectionLiteralResolver) {
        this.collectionLiteralResolver = collectionLiteralResolver;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setLocalVariableResolver(@NotNull LocalVariableResolver localVariableResolver) {
        this.localVariableResolver = localVariableResolver;
    }

    @Inject
    public void setTypeResolutionInterceptor(@NotNull TypeResolutionInterceptor typeResolutionInterceptor) {
        this.typeResolutionInterceptor = typeResolutionInterceptor;
    }

    @Inject
    public void setConstantExpressionEvaluator(@NotNull ConstantExpressionEvaluator constantExpressionEvaluator) {
        this.constantExpressionEvaluator = constantExpressionEvaluator;
    }

    @Inject
    public void setCallExpressionResolver(CallExpressionResolver callExpressionResolver) {
        this.callExpressionResolver = callExpressionResolver;
    }

    @Inject
    public void setCangJieTypeChecker(@NotNull NewCangJieTypeChecker cangjieTypeChecker) {
        this.cangjieTypeChecker = cangjieTypeChecker;
    }

    @Inject
    public void setLanguageVersionSettings(@NotNull LanguageVersionSettings languageVersionSettings) {
        this.languageVersionSettings = languageVersionSettings;
    }

    @Inject
    public void setDataFlowValueFactory(@NotNull DataFlowValueFactory dataFlowValueFactory) {
        this.dataFlowValueFactory = dataFlowValueFactory;
    }

    @Inject
    public void setAssignmentCheckers(@NotNull Iterable<AssignmentChecker> assignmentCheckers) {
        this.assignmentCheckers = assignmentCheckers;
    }

    @Inject
    public void setBuiltIns(@NotNull CangJieBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setOverloadChecker(OverloadChecker overloadChecker) {
        this.overloadChecker = overloadChecker;
    }

    @Inject
    public void setCallComponents(@NotNull CangJieCallComponents callComponents) {
        this.callComponents = callComponents;
    }

    @Inject
    public void setFunctionDescriptorResolver(FunctionDescriptorResolver functionDescriptorResolver) {
        this.functionDescriptorResolver = functionDescriptorResolver;
    }

}
