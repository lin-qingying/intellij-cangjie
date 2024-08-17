package com.huawei.cangjie.config


import com.huawei.cangjie.config.CangJieVersion.Companion.MAX_COMPONENT_VALUE
import com.huawei.cangjie.config.LanguageFeature.Kind.*
import com.huawei.cangjie.config.LanguageVersion.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DescriptionAware {
    val description: String
}

enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.CANGJIE_0_53_4,
    val hintUrl: String? = null,
    internal val isEnabledWithWarning: Boolean = false,
    val kind: Kind = OTHER // NB: default value OTHER doesn't force pre-releaseness (see KDoc)
) {
    // Note: names of these entries are also used in diagnostic tests and in user-visible messages (see presentableText below)



    TypeAliases(CANGJIE_0_53_4),
    OverloadResolutionByLambdaReturnType(CANGJIE_0_53_4),
    RefinedSamAdaptersPriority(CANGJIE_0_53_4),
    ProhibitConstructorCallOnFunctionalSupertype(CANGJIE_0_53_4, kind = BUG_FIX), // KT-46344

//    BoundCallableReferences(CANGJIE_0_53_4, ApiVersion.CANGJIE_0_53_4),
//    LocalDelegatedProperties(CANGJIE_0_53_4, ApiVersion.CANGJIE_0_53_4),
//    TopLevelSealedInheritance(CANGJIE_0_53_4),
//    AdditionalBuiltInsMembers(CANGJIE_0_53_4),
//    DataClassInheritance(CANGJIE_0_53_4),
//    InlineProperties(CANGJIE_0_53_4),
//    DestructuringLambdaParameters(CANGJIE_0_53_4),
//    SingleUnderscoreForParameterName(CANGJIE_0_53_4),
//    DslMarkersSupport(CANGJIE_0_53_4),
//    UnderscoresInNumericLiterals(CANGJIE_0_53_4),
//    DivisionByZeroInConstantExpressions(CANGJIE_0_53_4),
//    InlineConstVals(CANGJIE_0_53_4),
//    OperatorRem(CANGJIE_0_53_4),
//    OperatorProvideDelegate(CANGJIE_0_53_4),
//    ShortSyntaxForPropertyGetters(CANGJIE_0_53_4),
//    RefinedSamAdaptersPriority(CANGJIE_0_53_4),
//    SafeCallBoundSmartCasts(CANGJIE_0_53_4),
//    TypeInferenceOnGenericsForCallableReferences(CANGJIE_0_53_4),
//    NoDelegationToJavaDefaultInterfaceMembers(CANGJIE_0_53_4),
    DefaultImportOfPackageCangJieComparisons(CANGJIE_0_53_4),
//    Coroutines(
//        CANGJIE_0_53_4, ApiVersion.CANGJIE_0_53_4,
////        "https://kotlinlang.org/docs/diagnostics/experimental-coroutines",
//        isEnabledWithWarning = true
//    ),
//
//    // 1.2
ImprovedCapturedTypeApproximationInInference(CANGJIE_0_53_4, kind = OTHER), // KT-64515

//    InlineDefaultFunctionalParameters(CANGJIE_0_53_4),
//    SoundSmartCastsAfterTry(CANGJIE_0_53_4),
//    DeprecatedFieldForInvisibleCompanionObject(CANGJIE_0_53_4),
//    NullabilityAssertionOnExtensionReceiver(CANGJIE_0_53_4),
//    SafeCastCheckBoundSmartCasts(CANGJIE_0_53_4),
//    CapturedInClosureSmartCasts(CANGJIE_0_53_4),
//    LateinitTopLevelProperties(CANGJIE_0_53_4),
//    LateinitLocalVariables(CANGJIE_0_53_4),
//    InnerClassInEnumEntryClass(CANGJIE_0_53_4),
//    CallableReferencesToClassMembersWithEmptyLHS(CANGJIE_0_53_4),
//    ThrowNpeOnExplicitEqualsForBoxedNull(CANGJIE_0_53_4),
//    JvmPackageName(CANGJIE_0_53_4),
//    AssigningArraysToVarargsInNamedFormInAnnotations(CANGJIE_0_53_4),
//    ExpectedTypeFromCast(CANGJIE_0_53_4),
//    DefaultMethodsCallFromJava6TargetError(CANGJIE_0_53_4),
//
//    // 1.3
//
//    RestrictionOfValReassignmentViaBackingField(CANGJIE_0_53_4, kind = BUG_FIX),
//    NestedClassesInEnumEntryShouldBeInner(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitDataClassesOverridingCopy(CANGJIE_0_53_4, kind = BUG_FIX),
//    RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitInnerClassesOfGenericClassExtendingThrowable(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProperForInArrayLoopRangeVariableAssignmentSemantic(CANGJIE_0_53_4, kind = BUG_FIX),
//    NestedClassesInAnnotations(CANGJIE_0_53_4),
//    JvmStaticInInterface(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE),
//    JvmFieldInInterface(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE),
    ProhibitVisibilityOfNestedClassifiersFromSupertypes(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitNonConstValuesAsVarargsInAnnotations(CANGJIE_0_53_4, kind = BUG_FIX),
//    ReleaseCoroutines(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE),
//    ReadDeserializedContracts(CANGJIE_0_53_4),
//    UseReturnsEffect(CANGJIE_0_53_4),
//    UseCallsInPlaceEffect(CANGJIE_0_53_4),
//    AllowContractsForCustomFunctions(CANGJIE_0_53_4),
//    VariableDeclarationInWhenSubject(CANGJIE_0_53_4),
//    ProhibitLocalAnnotations(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitSmartcastsOnLocalDelegatedProperty(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitOperatorMod(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitAssigningSingleElementsToVarargsInNamedForm(CANGJIE_0_53_4, kind = BUG_FIX),
//    FunctionTypesWithBigArity(CANGJIE_0_53_4, sinceApiVersion = ApiVersion.CANGJIE_0_53_4),
//    RestrictRetentionForExpressionAnnotations(CANGJIE_0_53_4, kind = BUG_FIX),
//    NormalizeConstructorCalls(CANGJIE_0_53_4),
//    StrictJavaNullabilityAssertions(CANGJIE_0_53_4, kind = BUG_FIX),
//    SoundSmartcastForEnumEntries(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets(CANGJIE_0_53_4, kind = BUG_FIX),
//    NewCapturedReceiverFieldNamingConvention(CANGJIE_0_53_4, kind = BUG_FIX),
//    ExtendedMainConvention(CANGJIE_0_53_4),
//    ExperimentalBuilderInference(CANGJIE_0_53_4),
//    InlineClasses(CANGJIE_0_53_4, isEnabledWithWarning = true, kind = UNSTABLE_FEATURE),
//
//    // 1.4
//
//    DslMarkerOnFunctionTypeReceiver(CANGJIE_0_53_4, kind = BUG_FIX),
//    RestrictReturnStatementTarget(CANGJIE_0_53_4, kind = BUG_FIX),
//    NoConstantValueAttributeForNonConstVals(CANGJIE_0_53_4, kind = BUG_FIX),
//    WarningOnMainUnusedParameter(CANGJIE_0_53_4),
//    PolymorphicSignature(CANGJIE_0_53_4),
//    ProhibitConcurrentHashMapContains(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitTypeParametersForLocalVariables(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitTypeParametersInAnonymousObjects(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProperInlineFromHigherPlatformDiagnostic(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitRepeatedUseSiteTargetAnnotations(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitUseSiteTargetAnnotationsOnSuperTypes(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitTypeParametersInClassLiteralsInAnnotationArguments(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitComparisonOfIncompatibleEnums(CANGJIE_0_53_4, kind = BUG_FIX),
//    BareArrayClassLiteral(CANGJIE_0_53_4),
//    ProhibitGenericArrayClassLiteral(CANGJIE_0_53_4),
//    NonParenthesizedAnnotationsOnFunctionalTypes(CANGJIE_0_53_4),
//    UseGetterNameForPropertyAnnotationsMethodOnJvm(CANGJIE_0_53_4),
//    AllowBreakAndContinueInsideWhen(CANGJIE_0_53_4),
    MixedNamedArgumentsInTheirOwnPosition(CANGJIE_0_53_4),
//    ProhibitTailrecOnVirtualMember(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProperComputationOrderOfTailrecDefaultParameters(CANGJIE_0_53_4),
    TrailingCommas(CANGJIE_0_53_4),
//    ProhibitProtectedCallFromInline(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProperFinally(CANGJIE_0_53_4, kind = BUG_FIX),
//    AllowAssigningArrayElementsToVarargsInNamedFormForFunctions(CANGJIE_0_53_4),
//    AllowNullOperatorsForResult(CANGJIE_0_53_4),
//    PreferJavaFieldOverload(CANGJIE_0_53_4),
//    AllowContractsForNonOverridableMembers(CANGJIE_0_53_4),
//    AllowReifiedGenericsInContracts(CANGJIE_0_53_4),
//    ProperVisibilityForCompanionObjectInstanceField(CANGJIE_0_53_4, kind = BUG_FIX),
//    DoNotGenerateThrowsForDelegatedCangJieMembers(CANGJIE_0_53_4),
//    ProperIeee754Comparisons(CANGJIE_0_53_4, kind = BUG_FIX),
//    FunctionalInterfaceConversion(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE),
//    GenerateJvmOverloadsAsFinal(CANGJIE_0_53_4),
//    MangleClassMembersReturningInlineClasses(CANGJIE_0_53_4),
//    ImproveReportingDiagnosticsOnProtectedMembersOfBaseClass(CANGJIE_0_53_4, kind = BUG_FIX),
//
    NewInference(CANGJIE_0_53_4),
//
//    // In the next block, features can be enabled only along with new inference
//    // v----------------------------------------------------------------------v
//    SamConversionForCangJieFunctions(CANGJIE_0_53_4),
//    SamConversionPerArgument(CANGJIE_0_53_4),
//    FunctionReferenceWithDefaultValueAsOtherType(CANGJIE_0_53_4),
//    OverloadResolutionByLambdaReturnType(CANGJIE_0_53_4),
//    ContractsOnCallsWithImplicitReceiver(CANGJIE_0_53_4),
//    // ^----------------------------------------------------------------------^
//
//    // 1.5
//
//    ProhibitSpreadOnSignaturePolymorphicCall(CANGJIE_0_53_4, kind = BUG_FIX),
    ProhibitInvisibleAbstractMethodsInSuperclasses(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitNonReifiedArraysAsReifiedTypeArguments(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitVarargAsArrayAfterSamArgument(CANGJIE_0_53_4, kind = BUG_FIX),
//    CorrectSourceMappingSyntax(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE),
//    ProperArrayConventionSetterWithDefaultCalls(CANGJIE_0_53_4, kind = OTHER),
//    AdaptedCallableReferenceAgainstReflectiveType(null),
//    InferenceCompatibility(CANGJIE_0_53_4, kind = BUG_FIX),
    RequiredPrimaryConstructorDelegationCallInEnums(CANGJIE_0_53_4, kind = BUG_FIX),
//    ApproximateAnonymousReturnTypesInPrivateInlineFunctions(CANGJIE_0_53_4, kind = BUG_FIX),
//    ForbidReferencingToUnderscoreNamedParameterOfCatchBlock(CANGJIE_0_53_4, kind = BUG_FIX),
    UseCorrectExecutionOrderForVarargArguments(CANGJIE_0_53_4, kind = BUG_FIX),
//    JvmRecordSupport(CANGJIE_0_53_4),
//    AllowNullOperatorsForResultAndResultReturnTypeByDefault(CANGJIE_0_53_4),
    AllowSealedInheritorsInDifferentFilesOfSamePackage(CANGJIE_0_53_4),
//    SealedInterfaces(CANGJIE_0_53_4),
//    JvmIrEnabledByDefault(CANGJIE_0_53_4),
//    JvmInlineValueClasses(CANGJIE_0_53_4, kind = OTHER),
//    SuspendFunctionsInFunInterfaces(CANGJIE_0_53_4, kind = OTHER),
//    SamWrapperClassesAreSynthetic(CANGJIE_0_53_4, kind = BUG_FIX),
//    StrictOnlyInputTypesChecks(CANGJIE_0_53_4),
//
//    // 1.6
//
//    ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor(CANGJIE_0_53_4, kind = BUG_FIX),
//    PrivateInFileEffectiveVisibility(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitSelfCallsInNestedObjects(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProperCheckAnnotationsTargetInTypeUsePositions(CANGJIE_0_53_4, kind = BUG_FIX),
//    SuspendFunctionAsSupertype(CANGJIE_0_53_4),
//    UnrestrictedBuilderInference(CANGJIE_0_53_4),
//    ClassTypeParameterAnnotations(CANGJIE_0_53_4),
    TypeInferenceOnCallsWithSelfTypes(CANGJIE_0_53_4),
//    WarnAboutNonExhaustiveWhenOnAlgebraicTypes(CANGJIE_0_53_4, kind = BUG_FIX),
//    InstantiationOfAnnotationClasses(CANGJIE_0_53_4),
//    OptInContagiousSignatures(CANGJIE_0_53_4, kind = BUG_FIX),
//    RepeatableAnnotations(CANGJIE_0_53_4),
//    RepeatableAnnotationContainerConstraints(CANGJIE_0_53_4, kind = BUG_FIX),
//    UseBuilderInferenceOnlyIfNeeded(CANGJIE_0_53_4),
//    SuspendConversion(CANGJIE_0_53_4),
//    ProhibitSuperCallsFromPublicInline(CANGJIE_0_53_4),
//    ProhibitProtectedConstructorCallFromPublicInline(CANGJIE_0_53_4),
//
//    // 1.7
//
//    /*
//     * Improvements include the following:
//     *  - taking into account for type enhancement freshly supported type use annotations: KT-11454
//     *  - use annotations in the type parameter position to enhance corresponding types: KT-11454
//     *  - proper support of the type enhancement of the annotated java arrays: KT-24392
//     *  - proper support of the type enhancement of the annotated java varargs' elements: KT-18768
//     *  - type enhancement based on annotated bounds of type parameters
//     *  - type enhancement within type arguments of the base classes and interfaces
//     *  - support type enhancement based on type use annotations on java fields
//     *  - preference of a type use annotation to annotation of another type: KT-24392
//     *      (if @NotNull has TYPE_USE and METHOD target, then `@NotNull Integer []` -> `Array<Int>..Array<out Int>?` instead of `Array<Int>..Array<out Int>`)
//     */
//    TypeEnhancementImprovementsInStrictMode(CANGJIE_0_53_4),
//    OptInRelease(CANGJIE_0_53_4),
//    ProhibitNonExhaustiveWhenOnAlgebraicTypes(CANGJIE_0_53_4, kind = BUG_FIX),
//    UseBuilderInferenceWithoutAnnotation(CANGJIE_0_53_4),
//    ProhibitSmartcastsOnPropertyFromAlienBaseClass(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitInvalidCharsInNativeIdentifiers(CANGJIE_0_53_4, kind = BUG_FIX),
    DefinitelyNonNullableTypes(CANGJIE_0_53_4),
//    ProhibitSimplificationOfNonTrivialConstBooleanExpressions(CANGJIE_0_53_4),
//    SafeCallsAreAlwaysNullable(CANGJIE_0_53_4),
//    JvmPermittedSubclassesAttributeForSealed(CANGJIE_0_53_4),
    ProperTypeInferenceConstraintsProcessing(CANGJIE_0_53_4, kind = BUG_FIX),
//    ForbidExposingTypesInPrimaryConstructorProperties(CANGJIE_0_53_4, kind = BUG_FIX),
//    PartiallySpecifiedTypeArguments(CANGJIE_0_53_4),
    EliminateAmbiguitiesWithExternalTypeParameters(CANGJIE_0_53_4),
    EliminateAmbiguitiesOnInheritedSamInterfaces(CANGJIE_0_53_4),
//    ConsiderExtensionReceiverFromConstrainsInLambda(CANGJIE_0_53_4, kind = BUG_FIX), // KT-49832
    ProperInternalVisibilityCheckInImportingScope(CANGJIE_0_53_4, kind = BUG_FIX),
//    InlineClassImplementationByDelegation(CANGJIE_0_53_4),
//    QualifiedSupertypeMayBeExtendedByOtherSupertype(CANGJIE_0_53_4),
    YieldIsNoMoreReserved(CANGJIE_0_53_4),
//    NoDeprecationOnDeprecatedEnumEntries(CANGJIE_0_53_4), // KT-37975
//    ProhibitQualifiedAccessToUninitializedEnumEntry(CANGJIE_0_53_4, kind = BUG_FIX), // KT-41124
//    ForbidRecursiveDelegateExpressions(CANGJIE_0_53_4, kind = BUG_FIX),
//    CangJieFunInterfaceConstructorReference(CANGJIE_0_53_4),
//    SuspendOnlySamConversions(CANGJIE_0_53_4),
//
//    // 1.8
//
//    DontLoseDiagnosticsDuringOverloadResolutionByReturnType(CANGJIE_0_53_4),
//    ProhibitConfusingSyntaxInWhenBranches(CANGJIE_0_53_4, kind = BUG_FIX), // KT-48385
    UseConsistentRulesForPrivateConstructorsOfSealedClasses(sinceVersion = CANGJIE_0_53_4, kind = BUG_FIX), // KT-44866
//    ProgressionsChangingResolve(CANGJIE_0_53_4), // KT-49276
    AbstractClassMemberNotImplementedWithIntermediateAbstractClass(CANGJIE_0_53_4, kind = BUG_FIX), // KT-45508
//    ForbidSuperDelegationToAbstractAnyMethod(CANGJIE_0_53_4, kind = BUG_FIX), // KT-38078
//    ProperEqualityChecksInBuilderInferenceCalls(CANGJIE_0_53_4, kind = BUG_FIX),
//    ProhibitNonExhaustiveIfInRhsOfElvis(CANGJIE_0_53_4, kind = BUG_FIX), // KT-44705
//    ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes(CANGJIE_0_53_4, kind = BUG_FIX), // KT-29168
//    ForbidUsingExtensionPropertyTypeParameterInDelegate(CANGJIE_0_53_4, kind = BUG_FIX), // KT-24643
//    SynchronizedSuspendError(CANGJIE_0_53_4, kind = BUG_FIX), // KT-48516
//    ReportNonVarargSpreadOnGenericCalls(CANGJIE_0_53_4, kind = BUG_FIX), // KT-48162
//    RangeUntilOperator(CANGJIE_0_53_4), // KT-15613
//    GenericInlineClassParameter(sinceVersion = CANGJIE_0_53_4, kind = UNSTABLE_FEATURE), // KT-32162
//
//    // 1.9
//
//    ProhibitIllegalValueParameterUsageInDefaultArguments(CANGJIE_0_53_4, kind = BUG_FIX), // KT-25694
//    ProhibitConstructorCallOnFunctionalSupertype(CANGJIE_0_53_4, kind = BUG_FIX), // KT-46344
//    ProhibitArrayLiteralsInCompanionOfAnnotation(CANGJIE_0_53_4, kind = BUG_FIX), // KT-39041
//    ProhibitCyclesInAnnotations(CANGJIE_0_53_4, kind = BUG_FIX), // KT-47932
//    ForbidExtensionFunctionTypeOnNonFunctionTypes(CANGJIE_0_53_4, kind = BUG_FIX), // related to KT-43527
//    ProhibitEnumDeclaringClass(CANGJIE_0_53_4, kind = BUG_FIX), // KT-49653
//    StopPropagatingDeprecationThroughOverrides(CANGJIE_0_53_4, kind = BUG_FIX), // KT-47902
//    ReportTypeVarianceConflictOnQualifierArguments(CANGJIE_0_53_4, kind = BUG_FIX), // KT-50947
//    ReportErrorsOnRecursiveTypeInsidePlusAssignment(CANGJIE_0_53_4, kind = BUG_FIX), // KT-48546
//    ForbidExtensionCallsOnInlineFunctionalParameters(CANGJIE_0_53_4, kind = BUG_FIX), // KT-52502
//    SkipStandaloneScriptsInSourceRoots(CANGJIE_0_53_4, kind = OTHER), // KT-52525
//    ModifierNonBuiltinSuspendFunError(CANGJIE_0_53_4, kind = BUG_FIX), // KT-49264
//    EnumEntries(CANGJIE_0_53_4, sinceApiVersion = ApiVersion.CANGJIE_0_53_4, kind = UNSTABLE_FEATURE), // KT-48872
//    ForbidSuperDelegationToAbstractFakeOverride(CANGJIE_0_53_4, kind = BUG_FIX), // KT-49017
//    DataObjects(CANGJIE_0_53_4), // KT-4107
//    ProhibitAccessToEnumCompanionMembersInEnumConstructorCall(CANGJIE_0_53_4, kind = BUG_FIX), // KT-49110
//    RefineTypeCheckingOnAssignmentsToJavaFields(CANGJIE_0_53_4, kind = BUG_FIX), // KT-46727
//    ValueClassesSecondaryConstructorWithBody(sinceVersion = CANGJIE_0_53_4, kind = UNSTABLE_FEATURE), // KT-55333
//    NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess(CANGJIE_0_53_4, kind = BUG_FIX), // KT-27002
//    TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck(CANGJIE_0_53_4, kind = OTHER), // KT-58587
//    ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated(sinceVersion = CANGJIE_0_53_4), // KT-36770
//    NoSourceCodeInNotNullAssertionExceptions(
//        CANGJIE_0_53_4,
//        sinceApiVersion = ApiVersion.CANGJIE_0_53_4,
//        kind = OTHER
//    ), // KT-57570
//
//    // 1.9.20 KMP stabilization. Unfortunately, we don't have 1.9.20 LV. So LV=1.9 is the best we can do.
//    // At least there won't be false positives for 1.8 users
//    MultiplatformRestrictions(CANGJIE_0_53_4, kind = BUG_FIX), // KT-61668
//
//    // End of 1.* language features --------------------------------------------------
//
//    // 2.0
//
//    EnhanceNullabilityOfPrimitiveArrays(CANGJIE_0_53_4, kind = BUG_FIX), // KT-54521
//
//    /**
//     * This feature is highly related to ForbidInferringTypeVariablesIntoEmptyIntersection and while they belong to the same LV,
//     * they might be used interchangeably.
//     *
//     * But there might be the case that we may postpone ForbidInferringTypeVariablesIntoEmptyIntersection but leave AllowEmptyIntersectionsInResultTypeResolver in 2.0.
//     * In that case, we would stick to the simple behavior of just inferring empty intersection (without complicated logic of filtering out expected constraints),
//     * but we would report a warning instead of an error (until ForbidInferringTypeVariablesIntoEmptyIntersection is enabled).
//     */
    AllowEmptyIntersectionsInResultTypeResolver(CANGJIE_0_53_4, kind = OTHER), // KT-51221
//    ProhibitSmartcastsOnPropertyFromAlienBaseClassInheritedInInvisibleClass(CANGJIE_0_53_4, kind = BUG_FIX), // KT-57290
//    ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound(CANGJIE_0_53_4, kind = BUG_FIX), // KT-47986
//    ProhibitUseSiteGetTargetAnnotations(CANGJIE_0_53_4, kind = BUG_FIX), // KT-15470
//    KeepNullabilityWhenApproximatingLocalType(CANGJIE_0_53_4, kind = BUG_FIX), // KT-53982
//    ProhibitAccessToInvisibleSetterFromDerivedClass(CANGJIE_0_53_4, kind = BUG_FIX), // KT-56662
//    ProhibitOpenValDeferredInitialization(CANGJIE_0_53_4, kind = BUG_FIX), // KT-57553
//    SupportEffectivelyFinalInExpectActualVisibilityCheck(CANGJIE_0_53_4, kind = BUG_FIX), // KT-61955
//    ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor(CANGJIE_0_53_4, kind = BUG_FIX), // KT-58472
//    MangleCallsToJavaMethodsWithValueClasses(CANGJIE_0_53_4, kind = OTHER), // KT-55945
//    ForbidInferringTypeVariablesIntoEmptyIntersection(CANGJIE_0_53_4, kind = BUG_FIX), // KT-51221
//    ProhibitDefaultArgumentsInExpectActualizedByFakeOverride(CANGJIE_0_53_4, kind = BUG_FIX), // KT-62036
//    DisableCompatibilityModeForNewInference(
//        CANGJIE_0_53_4,
//        kind = OTHER
//    ), // KT-63558 (umbrella), KT-64306, KT-64307, KT-64308
//    DfaBooleanVariables(CANGJIE_0_53_4), // KT-25747
//
//    // 2.1
//
//    ReferencesToSyntheticJavaProperties(CANGJIE_0_53_4), // KT-8575
//    ProhibitImplementingVarByInheritedVal(CANGJIE_0_53_4, kind = BUG_FIX), // KT-56779
//    PrioritizedEnumEntries(CANGJIE_0_53_4, kind = UNSTABLE_FEATURE), // KT-58920
//    ProhibitInlineModifierOnPrimaryConstructorParameters(CANGJIE_0_53_4, kind = BUG_FIX), // KT-59664
//    ProhibitSingleNamedFunctionAsExpression(CANGJIE_0_53_4, kind = BUG_FIX), // KT-62573
//    ForbidLambdaParameterWithMissingDependencyType(CANGJIE_0_53_4, kind = BUG_FIX), // KT-64266
//    JsAllowInvalidCharsIdentifiersEscaping(CANGJIE_0_53_4, kind = OTHER), // KT-31799
//    SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced(CANGJIE_0_53_4, kind = BUG_FIX), // KT-63209
//    ProhibitPrivateOperatorCallInInline(CANGJIE_0_53_4, kind = BUG_FIX), // KT-65494
//
//    // End of 2.* language features --------------------------------------------------
//
//    ExpectActualClasses(sinceVersion = null), // KT-62885
//
//    // Disabled for indefinite time. See KT-53751
//    IgnoreNullabilityForErasedValueParameters(sinceVersion = null, kind = BUG_FIX),
//
//    // Disabled for indefinite time. Disables restrictions of builder inference without annotation
//    // Note: In 1.7.0, builder inference without annotation was introduced.
//    // However, later we encountered various situations when it works incorrectly, and decided to forbid them.
//    // When this feature is disabled, various errors are reported which are related to these incorrect situations.
//    // When this feature is enabled, no such errors are reported.
//    NoBuilderInferenceWithoutAnnotationRestriction(sinceVersion = null, kind = OTHER),
//
//    // Disabled for indefinite time. Forces K2 report errors (instead of warnings) for incompatible
//    // equality & identity operators in cases where K1 would report warnings or would not report anything.
//    ReportErrorsForComparisonOperators(sinceVersion = null, kind = BUG_FIX),
//
//    // Disabled for indefinite time.
//    // Disables reporting of new errors (see KT-55055, KT-55056, KT-55079) in DiagnosticReporterByTrackingStrategy.
//    // All these errors are "lost" errors which existed always, but wasn't reported before 1.9.0.
//    // When this feature is disabled, all these "lost" errors are reported properly.
//    // When this feature is enabled, no such errors are reported.
    NoAdditionalErrorsInDiagnosticReporter(sinceVersion = null, kind = OTHER),
//
//    // top-level script inner classes never made any sense, but used for some time to overcome the capturing logic limitations
//    // Now capturing logic works properly, therefore the warning is reported in K2
//    // this feature will eventually switch this warning to an error
//    ProhibitScriptTopLevelInnerClasses(sinceVersion = null, kind = OTHER),
//
//    // Experimental features
//
//    BreakContinueInInlineLambdas(null), // KT-1436
//    LightweightLambdas(null),
//    JsEnableExtensionFunctionInExternals(null, kind = OTHER),
//    PackagePrivateFileClassesWithAllPrivateMembers(null), // Disabled until the breaking change is approved by the committee, see KT-10884.
//    BooleanElvisBoundSmartCasts(null), // see KT-26357 for details
//    NewDataFlowForTryExpressions(null),
//    AllowResultInReturnType(null),
//    MultiPlatformProjects(sinceVersion = null),
//    ProhibitComparisonOfIncompatibleClasses(sinceVersion = null, kind = BUG_FIX),
//    ProhibitAllMultipleDefaultsInheritedFromSupertypes(sinceVersion = null, kind = BUG_FIX),
//    ExplicitBackingFields(sinceVersion = null, kind = UNSTABLE_FEATURE),
//    FunctionalTypeWithExtensionAsSupertype(sinceVersion = null),
//    JsAllowValueClassesInExternals(sinceVersion = null, kind = OTHER),
    ContextReceivers(sinceVersion = null),
//    ValueClasses(sinceVersion = null, kind = UNSTABLE_FEATURE),
//    JavaSamConversionEqualsHashCode(sinceVersion = null, kind = UNSTABLE_FEATURE),
//    UnitConversionsOnArbitraryExpressions(sinceVersion = null),
//    JsAllowImplementingFunctionInterface(sinceVersion = null, kind = OTHER),
//    CustomEqualsInValueClasses(sinceVersion = null, kind = OTHER), // KT-24874
//    InlineLateinit(sinceVersion = null, kind = OTHER), // KT-23814
//    EnableDfaWarningsInK2(sinceVersion = null, kind = OTHER), // KT-50965
//    ContractSyntaxV2(sinceVersion = null, kind = UNSTABLE_FEATURE), // KT-56127
//    ImplicitSignedToUnsignedIntegerConversion(sinceVersion = null), // KT-56583
//    IntrinsicConstEvaluation(sinceVersion = null, kind = UNSTABLE_FEATURE), // KT-49303
//    DisableCheckingChangedProgressionsResolve(sinceVersion = null, kind = OTHER), // KT-49276
//    ContextSensitiveEnumResolutionInWhen(sinceVersion = null, kind = UNSTABLE_FEATURE), // KT-52774
//    ForbidSyntheticPropertiesWithoutBaseJavaGetter(sinceVersion = null, kind = OTHER), // KT-64358
    ;

    init {
        if (sinceVersion == null && isEnabledWithWarning) {
            error("$this: '${::isEnabledWithWarning.name}' has no effect if the feature is disabled by default")
        }
    }

    val presentableName: String
        // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::lowercase)

    val presentableText get() = if (hintUrl == null) presentableName else "$presentableName (See: $hintUrl)"

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        ENABLED_WITH_WARNING("Enabled with warning"),
        DISABLED("Disabled");
    }

    /**
     * If 'true', then this feature will be automatically enabled under '-progressive' mode.
     *
     * Please, see `canBeEnabledInProgressiveMode` in [Kind] for more details.
     */
    val enabledInProgressiveMode: Boolean get() = kind.canBeEnabledInProgressiveMode && sinceVersion != null

    /**
     * # [forcesPreReleaseBinaries]
     * If 'true', then enabling this feature (e.g. by '-XXLanguage:', or dedicated '-X'-flag)
     * will force generation of pre-release binaries (given that [sinceVersion] > [LanguageVersion.LATEST_STABLE]).
     * Use it for features that involve generation of non-trivial low-level code with non-finalized design.
     *
     * Note that [forcesPreReleaseBinaries] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE].
     *
     * Please, DO NOT use features that force pre-release binaries in the CangJie project, as that would
     * generate 'kotlin-compiler' as pre-release.
     *
     *
     * # [canBeEnabledInProgressiveMode]
     * If 'true', then this feature will be automatically enabled under '-progressive' mode if `sinceCangJie` is set.
     *
     * Restrictions for using this flag for particular feature follow from restrictions of the progressive mode:
     * - enabling it *must not* break compatibility with non-progressive compiler, i.e. code written under progressive
     *   should compile successfully by non-progressive compiler with the same language version settings.
     *   Example: making some "red" code "green" is not fine, because non-progressive compilers won't be able to compile
     *   such code
     *
     * - changes in language semantics should not be "silent": user must receive some message from the compiler
     *   about all affected code. Exceptions are possible on case-by-case basis.
     *   Example: silently changing semantics of generated low-level code is not fine, but deprecating some language
     *   construction immediately instead of a going through complete deprecation cycle is fine.
     *
     * NB: Currently, [canBeEnabledInProgressiveMode] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE]
     */
    enum class Kind(val canBeEnabledInProgressiveMode: Boolean, val forcesPreReleaseBinaries: Boolean) {
        /**
         * Simple bug fix which just forbids some language constructions.
         * Rule of thumb: it turns "green code" into "red".
         *
         * Note that, some actual bug fixes can affect overload resolution/inference, silently changing semantics of
         * users' code -- DO NOT use Kind.BUG_FIX for them!
         */
        BUG_FIX(true, false),

        /**
         * Enables support of some new and *unstable* construction in language.
         * Rule of thumb: it turns "red" code into "green", and we want to strongly demotivate people from manually enabling
         * that feature in production.
         */
        UNSTABLE_FEATURE(false, true),

        /**
         * A new feature in the language which has no impact on the binary output of the compiler, and therefore
         * does not cause pre-release binaries to be generated.
         * Rule of thumb: it turns "red" code into "green" and the old compilers can correctly use the binaries
         * produced by the new compiler.
         *
         * NB. OTHER is not a conservative fallback, as it doesn't imply generation of pre-release binaries
         */
        OTHER(false, false),
    }

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val major: Int, val minor: Int, val patch: Int) : DescriptionAware, LanguageOrApiVersion {

    CANGJIE_0_53_4(0, 53, 4),
    ;

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override val versionString: String = "$major.$minor"

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String?) = values().find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        // Version status
        //            1.0..1.3        1.4..1.6           1.7..2.0    2.1
        // Language:  UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL
        // API:       UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL

        @JvmField
        val FIRST_API_SUPPORTED = CANGJIE_0_53_4

        @JvmField
        val FIRST_SUPPORTED = CANGJIE_0_53_4

        @JvmField
        val FIRST_NON_DEPRECATED = CANGJIE_0_53_4

        @JvmField
        val LATEST_STABLE = CANGJIE_0_53_4
    }
}

interface LanguageOrApiVersion : DescriptionAware {
    val versionString: String

    val isStable: Boolean

    val isDeprecated: Boolean

    val isUnsupported: Boolean

    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }
}

fun LanguageVersion.toCangJieVersion() = CangJieVersion(major, minor)

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature).let {
            it == LanguageFeature.State.ENABLED ||
                    it == LanguageFeature.State.ENABLED_WITH_WARNING
        }

    fun isPreRelease(): Boolean

    fun <T> getFlag(flag: AnalysisFlag<T>): T

    val apiVersion: ApiVersion

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion

    companion object {
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> =
        Collections.unmodifiableMap(specificFeatures)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        specificFeatures[feature]?.let { return it }

        val since = feature.sinceVersion
        if (since != null && languageVersion >= since && apiVersion >= feature.sinceApiVersion) {
            return if (feature.isEnabledWithWarning) LanguageFeature.State.ENABLED_WITH_WARNING else LanguageFeature.State.ENABLED
        }

        return LanguageFeature.State.DISABLED
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.entries.sortedBy { (feature, _) -> feature.ordinal }.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.ENABLED_WITH_WARNING -> '~'
                LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
        analysisFlags.entries.sortedBy { (flag, _) -> flag.toString() }.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }

    override fun isPreRelease(): Boolean = languageVersion.isPreRelease() ||
            specificFeatures.any { (feature, state) ->
                state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()
            }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageVersion.isPreRelease(): Boolean {
    if (!isStable) return true

    return CangJieCompilerVersion.isPreRelease && this == LanguageVersion.LATEST_STABLE
}

fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && kind.forcesPreReleaseBinaries
}


class ApiVersion private constructor(
    val version: String,
    override val versionString: String
) : Comparable<ApiVersion>, DescriptionAware, LanguageOrApiVersion {

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override fun compareTo(other: ApiVersion): Int =
        version.compareTo(other.version)

    override fun equals(other: Any?) =
        (other as? ApiVersion)?.version == version

    override fun hashCode() =
        version.hashCode()

    override fun toString() = versionString

    companion object {

        @JvmField
        val CANGJIE_0_53_4 = createByLanguageVersion(LanguageVersion.CANGJIE_0_53_4)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.entries.last())

        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        @JvmField
        val FIRST_SUPPORTED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_API_SUPPORTED)

        @JvmField
        val FIRST_NON_DEPRECATED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_NON_DEPRECATED)

        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(versionString, versionString)
        } catch (e: Exception) {
            null
        }
    }
}

class AnalysisFlag<out T> internal constructor(
    private val name: String,
    val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    object Delegates {
        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, false)
        }

        object ApiModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, ExplicitApiMode.DISABLED)
        }

        object ListOfStrings {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, emptyList<String>())
        }
    }
}

enum class ExplicitApiMode(val state: String) {
    DISABLED("disable"),
    STRICT("strict"),
    WARNING("warning");

    companion object {
        fun fromString(string: String): ExplicitApiMode? = values().find { it.state == string }

        fun availableValues() = values().joinToString(prefix = "{", postfix = "}") { it.state }
    }
}


object CangJieCompilerVersion {
    const val VERSION_FILE_PATH: String = "/META-INF/compiler.version"
    var VERSION: String? = null

    // True if the latest stable language version supported by this compiler has not yet been released.
    // Binaries produced by this compiler with that language version (or any future language version) are going to be marked
    // as "pre-release" and will not be loaded by release versions of the compiler.
    // Change this value before and after every major release
    private const val IS_PRE_RELEASE = true

    const val TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY: String = "kotlin.test.is.pre.release"

    val isPreRelease: Boolean
        get() {
            val overridden = System.getProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
            if (overridden != null) {
                return overridden.toBoolean()
            }

            return IS_PRE_RELEASE
        }

    val version: String?
        /**
         * @return version of this compiler, or `null` if it isn't known (if VERSION is "@snapshot@")
         */
        get() = if (VERSION == "@snapshot@") null else VERSION

    @Throws(IOException::class)
    private fun loadCangJieCompilerVersion(): String {
        val versionReader = BufferedReader(
            InputStreamReader(CangJieCompilerVersion::class.java.getResourceAsStream(VERSION_FILE_PATH))
        )
        try {
            return versionReader.readLine()
        } finally {
            versionReader.close()
        }
    }

    init {
        try {
            VERSION = loadCangJieCompilerVersion()
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read compiler version from " + VERSION_FILE_PATH)
        }

        check(!(VERSION != "@snapshot@" && !VERSION!!.contains("-") && IS_PRE_RELEASE)) {
            """
                IS_PRE_RELEASE cannot be true for a compiler without '-' in its version.
                Please change IS_PRE_RELEASE to false, commit and push this change to master
                """.trimIndent()
        }
    }
}

/**
 * Represents a version of the CangJie standard library.
 *
 * [major], [minor] and [patch] are integer components of a version,
 * they must be non-negative and not greater than 255 ([MAX_COMPONENT_VALUE]).
 *
 * @constructor Creates a version from all three components.
 */

class CangJieVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<CangJieVersion> {
    /**
     * Creates a version from [major] and [minor] components, leaving [patch] component zero.
     */
    constructor(major: Int, minor: Int) : this(major, minor, 0)

    private val version = versionOf(major, minor, patch)

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    /**
     * Returns the string representation of this version
     */
    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherVersion = (other as? CangJieVersion) ?: return false
        return this.version == otherVersion.version
    }

    override fun hashCode(): Int = version

    override fun compareTo(other: CangJieVersion): Int = version - other.version

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major] and [minor] components.
     */
    fun isAtLeast(major: Int, minor: Int): Boolean = // this.version >= versionOf(major, minor, 0)
        this.major > major || (this.major == major &&
                this.minor >= minor)

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major], [minor] and [patch] components.
     */
    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
        // this.version >= versionOf(major, minor, patch)
        this.major > major || (this.major == major &&
                (this.minor > minor || this.minor == minor &&
                        this.patch >= patch))

    companion object {
        /**
         * Maximum value a version component can have, a constant value 255.
         */
        // NOTE: Must be placed before CURRENT because its initialization requires this field being initialized in JS
        const val MAX_COMPONENT_VALUE = 255

        /**
         * Returns the current version of the CangJie standard library.
         */
        @JvmField
        val CURRENT: CangJieVersion = CangJieVersionCurrentValue.get()
    }
}

// this class is ignored during classpath normalization when considering whether to recompile dependencies in CangJie build
private object CangJieVersionCurrentValue {
    @JvmStatic
    fun get(): CangJieVersion = CangJieVersion(1, 9, 22) // value is written here automatically during build
}

