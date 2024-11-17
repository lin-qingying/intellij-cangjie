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
    StaticContextChecker,
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

