package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.container.StorageComponentContainer
import com.linqingying.cangjie.container.useInstance
import com.linqingying.cangjie.container.useInstanceIfNotNull

fun StorageComponentContainer.configureDefaultCheckers() {
//    DEFAULT_DECLARATION_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CALL_CHECKERS.forEach { useInstance(it) }
//    DEFAULT_TYPE_CHECKERS.forEach { useInstance(it) }
//    DEFAULT_CLASSIFIER_USAGE_CHECKERS.forEach { useInstance(it) }
//    DEFAULT_ANNOTATION_CHECKERS.forEach { useInstance(it) }
//    DEFAULT_CLASH_RESOLVERS.forEach { useClashResolver(it) }
}

private val DEFAULT_CALL_CHECKERS = listOf(
//    CapturingInClosureChecker(),
//    InlineCheckerWrapper(),
//    SynchronizedByValueChecker(),
//    SafeCallChecker(),
//    TrailingCommaCallChecker,
//    DeprecatedCallChecker,
//    CallReturnsArrayOfNothingChecker(),
//    InfixCallChecker(),
//    OperatorCallChecker(),
//    ConstructorHeaderCallChecker,
//    ProtectedConstructorCallChecker,
//    ApiVersionCallChecker,
//    CoroutineSuspendCallChecker,
//    BuilderFunctionsCallChecker,
//    DslScopeViolationCallChecker,
//    MissingDependencyClassChecker,
//    CallableReferenceCompatibilityChecker(),
    UnderscoreUsageChecker,
//    AssigningNamedArgumentToVarargChecker(),
//    ImplicitNothingAsTypeParameterCallChecker,
//    PrimitiveNumericComparisonCallChecker,
//    LambdaWithSuspendModifierCallChecker,
    UselessElvisCallChecker(),
//    ResultTypeWithNullableOperatorsChecker(),
//    NullableVarargArgumentCallChecker,
//    NamedFunAsExpressionChecker,
//    ContractNotAllowedCallChecker,
//    ReifiedTypeParameterSubstitutionChecker(),
//    MissingDependencySupertypeChecker.ForCalls,
//    AbstractClassInstantiationChecker,
//    SuspendConversionCallChecker,
//    UnitConversionCallChecker,
//    FunInterfaceConstructorReferenceChecker,
//    NullableExtensionOperatorWithSafeCallChecker,
//    ReferencingToUnderscoreNamedParameterOfCatchBlockChecker,
//    VarargWrongExecutionOrderChecker,
//    SelfCallInNestedObjectConstructorChecker,
    NewSchemeOfIntegerOperatorResolutionChecker,
//    EnumEntryVsCompanionPriorityCallChecker,
//    CompanionInParenthesesLHSCallChecker,
//    ResolutionToPrivateConstructorOfSealedClassChecker,
//    EqualityCallChecker,
//    UnsupportedUntilOperatorChecker,
//    BuilderInferenceAssignmentChecker,
//    IncorrectCapturedApproximationCallChecker,
//    CompanionIncorrectlyUnboundedWhenUsedAsLHSCallChecker,
//    CustomEnumEntriesMigrationCallChecker,
//    EnumEntriesUnsupportedChecker,
)

