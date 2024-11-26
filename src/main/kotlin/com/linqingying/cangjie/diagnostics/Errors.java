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

package com.linqingying.cangjie.diagnostics;

import com.google.common.collect.ImmutableSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.linqingying.cangjie.config.LanguageFeature;
import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.diagnostics.rendering.DeclarationWithDiagnosticComponents;
import com.linqingying.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.linqingying.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import com.linqingying.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.linqingying.cangjie.lexer.CjKeywordToken;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.resolve.calls.components.DescriptorKind;
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintPosition;
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall;
import com.linqingying.cangjie.resolve.calls.tower.CandidateApplicability;
import com.linqingying.cangjie.resolve.calls.tower.WrongResolutionToClassifier;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.expressions.match.Pattern;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.linqingying.cangjie.descriptors.PositioningStrategies.*;
import static com.linqingying.cangjie.diagnostics.Severity.*;


/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 */
public interface Errors {
    DiagnosticFactoryForDeprecation0<CjExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ForbidRecursiveDelegateExpressions);
    DiagnosticFactoryForDeprecation0<CjExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT =
            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ReportErrorsOnRecursiveTypeInsidePlusAssignment);

    DiagnosticFactory0<CjExpression> INTEGER_OVERFLOW = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjExpression> DIVISION_BY_ZERO = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjCallExpression> ENUM_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjCallExpression> SEALED_CLASS_CONSTRUCTOR_CALL = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> EXPLICIT_DELEGATION_CALL_REQUIRED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> NONE_APPLICABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> CANNOT_COMPLETE_RESOLVE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> OVERLOAD_RESOLUTION_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    //Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
    //"INVISIBLE_REFERENCE" is used for invisible classes references and references in import
    DiagnosticFactory3<CjSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_REFERENCE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> ILLEGAL_UNDERSCORE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CYCLIC_GENERIC_UPPER_BOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<PsiElement, CjKeywordToken, String> DEPRECATED_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, CjKeywordToken, String> REDUNDANT_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<CjExpression, CangJieType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> UNDERSCORE_IS_RESERVED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> UPPER_BOUND_VIOLATED_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, String> INFERRED_INTO_DECLARED_UPPER_BOUNDS = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<CjReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(WARNING, PositioningStrategies.RETURN_WITH_LABEL);
    DiagnosticFactory0<CjExpression> SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameterBase> TYPE_PARAMETER_IN_CATCH_CLAUSE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CjKeywordToken> LET_OR_VAR_ON_CATCH_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, CjKeywordToken> LET_OR_VAR_ON_LOOP_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjExpression> ITERATOR_MISSING = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjExpression, Name, CangJieType> COMPONENT_FUNCTION_MISSING = DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> ITERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjExpression> ITERATOR_ON_NULLABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjExpression, Name, Collection<? extends ResolvedCall<?>>> COMPONENT_FUNCTION_AMBIGUITY =
            DiagnosticFactory2.create(ERROR, DEFAULT);
    DiagnosticFactory1<CjExpression, Name> COMPONENT_FUNCTION_ON_NULLABLE = DiagnosticFactory1.create(ERROR, DEFAULT);
    DiagnosticFactory0<CjDeclarationWithBody> NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);
    DiagnosticFactory2<CjSimpleNameExpression, String, String> LABEL_RESOLVE_WILL_CHANGE = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<CjSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<CjSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<CjTypeReference, CangJieType> QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjSuperExpression> SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjThisExpression> NO_THIS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> COMMA_IN_MATCH_CONDITION_WITHOUT_ARGUMENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NOT_ENUM_ENTRY_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NOT_ENUM_MATCH = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, Integer> ENUM_CONSTRUCTOR_MISMATCH = DiagnosticFactory1.create(ERROR);


    DiagnosticFactory2<CjTypeReference, CangJieType, String> TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_FROM_PRIVATE_IN_FILE =
            DiagnosticFactory3.create(WARNING);
    DiagnosticFactory3<PsiElement, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility> EXPOSED_TYPEALIAS_EXPANDED_TYPE =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<CjTypeParameter, TypeParameterDescriptor, CangJieType> UNUSED_TYPEALIAS_PARAMETER =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<CjElement, CangJieType, CangJieType> INCOMPATIBLE_ENUM_COMPARISON =
            DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<CjElement, CangJieType, CangJieType> INCOMPATIBLE_ENUM_COMPARISON_ERROR =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjTypeReference> INVALID_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<CjTypeReference> OPTIONAL_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpressionWithLabel, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjExpressionWithLabel> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpressionWithLabel> BREAK_OR_CONTINUE_IN_WHEN = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeReference, Integer, String> NO_TYPE_ARGUMENTS_ON_RHS = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjTypeReference> IS_ENUM_ENTRY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> DYNAMIC_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjOptionType> USELESS_NULLABLE_CHECK = DiagnosticFactory0.create(WARNING, OPTIONAL_TYPE);
    DiagnosticFactory1<CjElement, Boolean> USELESS_IS_CHECK = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<CjElement, CangJieType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjElement> SENSELESS_NULL_IN_MATCH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory2<CjElement, CangJieType, CangJieType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<CjExpression, CangJieType, CangJieType> IMPLICIT_CAST_TO_ANY = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<CjMatchExpression, List<MatchMissingCase>> NO_ELSE_IN_MATCH = DiagnosticFactory1.create(ERROR, MATCH_EXPRESSION);

    DiagnosticFactory1<CjMatchExpression, List<Pattern>> NO_ELSE_IN_MATCH_BY_PATTERN = DiagnosticFactory1.create(ERROR, MATCH_EXPRESSION);

    DiagnosticFactory1<CjMatchExpression, String> EXPECT_TYPE_IN_MATCH_WITHOUT_ELSE = DiagnosticFactory1.create(ERROR, MATCH_EXPRESSION);
    DiagnosticFactory0<CjMatchEntry> REDUNDANT_ELSE_IN_MATCH = DiagnosticFactory0.create(WARNING, ELSE_ENTRY);
    DiagnosticFactory0<PsiElement> DUPLICATE_LABEL_IN_MATCH = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<PsiElement, InvalidBinaryData> INVALID_BINARY_OPERATOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjTypeReference> INLETID_TYPE_OF_ANNOTATION_MEMBER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INLETID_IF_AS_EXPRESSION_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<CjMatchExpression, List<MatchMissingCase>> NO_ELSE_IN_MATCH_WARNING =
            DiagnosticFactory1.create(WARNING, MATCH_EXPRESSION);
    DiagnosticFactory0<CjThisType> INVALID_THIS_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<CjSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, FqName> INVISIBLE_REFERENCE_REEXPORT =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjConstructorDelegationReferenceExpression> INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<CjExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjElement, DeclarationDescriptor> DEPRECATED_ACCESS_BY_SHORT_NAME = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjElement, Integer, DeclarationDescriptor> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<CjElement, DeclarationDescriptor, DeclarationDescriptor> TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY = DiagnosticFactory2.create(ERROR);


    DiagnosticFactory1<PsiElement, BadNamedArgumentsTarget> NAMED_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<CjExpression, String, CangJieType, String> DELEGATE_SPECIAL_FUNCTION_MISSING = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjElement, ValueParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(ERROR, VALUE_ARGUMENTS);
    DiagnosticFactory1<PsiElement, String> YIELD_IS_RESERVED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjReferenceExpression> NAME_FOR_AMBIGUOUS_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjReferenceExpression, CjReferenceExpression> NAMED_PARAMETER_NOT_FOUND =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory0<CjExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<CjExpression, CangJieType, String, String> SMARTCAST_IMPOSSIBLE = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends CallableDescriptor>> CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjExpression> CALLABLE_REFERENCE_LHS_NOT_A_CLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<CjElement, CallableDescriptor> COMPATIBILITY_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, CangJieType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CangJieType> UNSAFE_IMPLICIT_INVOKE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, InvalidBinaryData> INLETID_BINARY_OPERATOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjExpression, CjExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjParameterBase> CATCH_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjElement, Set<Name>> NAMED_PARAMETER_PREFIX_MISSING =
            DiagnosticFactory1.create(ERROR);


    DiagnosticFactory1<PsiElement, String> MESSAGE_ERROR = DiagnosticFactory1.create(ERROR);


    DiagnosticFactory2<CjExpression, CjExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);
    DiagnosticFactory3<CjReferenceExpression, ClassifierDescriptor, WrongResolutionToClassifier, String> RESOLUTION_TO_CLASSIFIER =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_DIAGNOSTIC = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<PsiElement, CandidateApplicability, String> NEW_INFERENCE_UNKNOWN_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, PropertyDescriptor, ClassDescriptor> DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY = DiagnosticFactory2.create(WARNING);

    DiagnosticFactory0<CjConstructorDelegationReferenceExpression> DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> UNEXPECTED_FINALIZER_IN_BODY_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, String> UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjExpression> NOT_A_CLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR_WARNING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);
    DiagnosticFactory0<PsiElement> PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM =
            DiagnosticFactory0.create(WARNING, PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL);
    DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> CONFLICTING_OVERLOADS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<CjLambdaExpression> UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> MANY_LAMBDA_EXPRESSION_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeProjection> PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT = DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>> REDECLARATION =
            DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);
    DiagnosticFactory1<PsiElement, String> ENUM_REDECLARATION =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<PsiElement, CangJieType, CangJieType, ConstraintPosition> TYPE_MISMATCH_IN_CONSTRAINT = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory4<CjElement, Name, Name, CangJieType, CangJieType> UPPER_BOUND_VIOLATION_IN_CONSTRAINT = DiagnosticFactory4.create(ERROR);
    DiagnosticFactory1<CjParameter, CangJieType> EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<PsiElement, CangJieType, CangJieType> RECEIVER_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_MEMBER =
            DiagnosticFactory3.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory2<CjExpression, CangJieType, CangJieType> TYPE_MISMATCH_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<CjExpression, CangJieType, CangJieType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<CjElement, CangJieType> TUPLE_PATTERN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory1<PsiElement, List<CangJieType>> TYPE_MISMATCH_MULTIPLE_SUPERTYPES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjNamedDeclaration, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<CjElement, CangJieType> TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_STDLIB = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, String, @Nullable DeclarationDescriptor> NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> ARRAY_LITERAL_TYPE_INFERENCE_FAILED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameter> NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjElement> POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjElement> TUPLE_ARGS_TOO_FEW = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjElement, Integer, Integer> TUPLE_ARGS_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> VARRAY_SIZE_MISMATCH = DiagnosticFactory0.create(ERROR);

    DiagnosticFactoryForDeprecation1<PsiElement, TypeParameterDescriptor> TYPE_INFERENCE_ONLY_INPUT_TYPES =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.StrictOnlyInputTypesChecks);
    DiagnosticFactory2<CjElement, Set<CjElement>, Set<CjElement>> UNREACHABLE_CODE = DiagnosticFactory2.create(
            WARNING, ClassicPositioningStrategies.UNREACHABLE_CODE);
    DiagnosticFactory0<CjSimpleNameExpression> ENUM_ENTRY_AS_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjMatchEntry> ELSE_MISPLACED_IN_MATCH = DiagnosticFactory0.create(WARNING, ELSE_ENTRY);
    DiagnosticFactory0<CjCasePattern> VARIABLE_INTRODUCTION_CONFLICT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NOT_ENUM_PARAMETER_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ENUM_ENTRY_CONSTRUCTOR_REQUIER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> LET_EXPRESSION_NO_TYPE_PATTERN = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjElement> BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeParameter> MISPLACED_TYPE_PARAMETER_CONSTRAINTS = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjTypeParameterList> DEPRECATED_TYPE_PARAMETER_SYNTAX = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjFunction, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<PsiElement> RETURN_TYPE_NOT_SPECIFIED_ERROR =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<PsiElement, Modality, DescriptorKind, List<DescriptorVisibility>> ABSTRACT_MEMBER_VISIBILITY_ERROR =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<CjFunction, SimpleFunctionDescriptor> ABSTRACT_FUNCTION_WITH_BODY =
            DiagnosticFactory1.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory0<CjModifierListOwner> REDUNDANT_OPEN_IN_INTERFACE = DiagnosticFactory0.create(WARNING, OPEN_MODIFIER);
    DiagnosticFactory0<PsiElement> EXPECTED_PRIVATE_DECLARATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjDeclaration> EXPECTED_DECLARATION_WITH_BODY = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_PROPERTY_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> IMPLICIT_NOTHING_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CangJieType> IMPLICIT_INTERSECTION_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> ABBREVIATED_NOTHING_PROPERTY_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ABBREVIATED_NOTHING_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactoryForDeprecation4<PsiElement, String, Collection<CangJieType>, String, String> INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION =
            DiagnosticFactoryForDeprecation4.create(LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection);
    DiagnosticFactory4<PsiElement, String, Collection<CangJieType>, String, String> INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION =
            DiagnosticFactory4.create(WARNING);
    DiagnosticFactory2<CjModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS =
            DiagnosticFactory2.create(ERROR, ABSTRACT_MODIFIER);
    DiagnosticFactory1<PsiElement, String> COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjFunction, SimpleFunctionDescriptor> PRIVATE_FUNCTION_WITH_NO_BODY =
            DiagnosticFactory1.create(ERROR, PRIVATE_MODIFIER);
    DiagnosticFactory1<CjDeclaration, Collection<CangJieType>> AMBIGUOUS_ANONYMOUS_TYPE_INFERRED =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<CjDeclaration, CangJieType> APPROXIMATED_LOCAL_TYPE_WILL_BECOME_FLEXIBLE =
            DiagnosticFactory1.create(WARNING, DECLARATION_SIGNATURE);
    DiagnosticFactory1<CjDeclaration, CangJieType> APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE =
            DiagnosticFactory1.create(WARNING, DECLARATION_SIGNATURE);
    DiagnosticFactory1<CjSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_ON_LHS_OF_DOT = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Throwable> EXCEPTION_FROM_ANALYZER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjParameter> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameter> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjParameter, CangJieType> EXPECTED_PARAMETER_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjModifierList> MODIFIER_LIST_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjTypeParameter> VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, VARIANCE_MODIFIER);
    DiagnosticFactory0<CjFunction> FUNCTION_DECLARATION_WITH_NO_NAME = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> ACCESSOR_PARAMETER_NAME_SHADOWING = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<CjSimpleNameExpression, ClassDescriptor> CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>> UNRESOLVED_REFERENCE_WRONG_RECEIVER =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjVariableDeclaration> VARIABLE_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<CjTypeReference> LOCAL_EXTENSION_VARIABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpression, CangJieType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NAME_SHADOWING = DiagnosticFactory1.create(WARNING, PositioningStrategies.FOR_REDECLARATION);
    DiagnosticFactory2<CjElement, CallableDescriptor, CangJieType> MEMBER_PROJECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<CjElement, TypeMismatchDueToTypeProjectionsData> TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjReferenceExpression, CjReferenceExpression> UNRESOLVED_REFERENCE =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory0<CjExpression> NO_RECEIVER_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjExpression, CjExpression, CangJieType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjExpression> NON_CONST_LET_USED_IN_CONSTANT_EXPRESSION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjTypeProjection, ClassifierDescriptor> CONFLICTING_PROJECTION =
            DiagnosticFactory1.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<CjElement, CangJieType> CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjElement, ClassifierDescriptor> RECURSIVE_TYPEALIAS_EXPANSION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjAnnotationEntry> REPEATED_ANNOTATION = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeStatement, ClassDescriptor, Collection<CallableMemberDescriptor>> CONFLICTING_INHERITED_MEMBERS_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<CjThisType> INLETID_THIS_TYPE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<CjElement, String> TYPE_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> UNSUPPORTED_FEATURE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjDeclaration> TYPE_PARAMETERS_NOT_ALLOWED
            = DiagnosticFactory0.create(ERROR, TYPE_PARAMETERS_OR_DECLARATION_SIGNATURE);
    DiagnosticFactory0<PsiElement> ANONYMOUS_FUNCTION_WITH_NAME = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameter>
            ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);
    DiagnosticFactory0<CjParameter> USELESS_VARARG_ON_PARAMETER = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory1<CjDeclaration, CallableMemberDescriptor> EXTENSION_SHADOWED_BY_MEMBER =
            DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);
    DiagnosticFactory1<CjBinaryExpression, CangJieType> USELESS_ELVIS =
            DiagnosticFactory1.create(WARNING, PositioningStrategies.USELESS_ELVIS);

    DiagnosticFactory0<CjBinaryExpression> USELESS_ELVIS_RIGHT_IS_NULL =
            DiagnosticFactory0.create(WARNING, PositioningStrategies.USELESS_ELVIS);
    DiagnosticFactory0<PsiElement> IRREFUTABLE_PATTERN_FOR_IN_ERROR =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> IRREFUTABLE_PATTERN_ERROR =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjArrayAccessExpression> NO_GET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<CjArrayAccessExpression> NO_SET_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory1<PsiElement, String> INAPPLICABLE_OPERATOR_MODIFIER = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<CjArrayAccessExpression> NO_GET_FOR_TUPLE_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<CjArrayAccessExpression> NO_SET_FOR_TUPLE_METHOD = DiagnosticFactory0.create(ERROR, ARRAY_ACCESS);
    DiagnosticFactory0<PsiElement> NON_INTEGER_TUPLE_INDEX = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> TUPLE_INDEX_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<CjSimpleNameExpression, DeclarationDescriptor, CjSimpleNameExpression> ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<CjSuperTypeList, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjOptionType> NULLABLE_ON_DEFINITELY_NOT_OPTIONAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjOptionType> REDUNDANT_OPTIONAL = DiagnosticFactory0.create(WARNING, OPTIONAL_TYPE);
    DiagnosticFactory0<CjSimpleNameExpression> INC_DEC_SHOULD_NOT_RETURN_UNIT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory3<CjExpression, String, CangJieType, CangJieType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjExpression> VARIABLE_EXPECTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjBinaryExpression, CangJieType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>
            ASSIGN_OPERATOR_AMBIGUITY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjExpression, DeclarationDescriptor> LET_REASSIGNMENT = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory3<CjTypeParameter, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>>
            INCONSISTENT_TYPE_PARAMETER_BOUNDS = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjPropertyAccessor> ABSTRACT_PROPERTY_WITH_SETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> CONTEXT_RECEIVERS_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> EXTENSION_VARIABLE_WITH_BACKING_FIELD = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> VARIABLE_INITIALIZER_IN_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> EXPECTED_VARIABLE_INITIALIZER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjVariable> MUST_BE_INITIALIZED = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<CjVariable> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<CjVariable> MUST_BE_INITIALIZED_OR_BE_FINAL = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<CjVariable> MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);

    DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING =
            DiagnosticFactory3.create(WARNING);
    DiagnosticFactory0<CjTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> SUPERTYPE_NOT_A_CLASS_OR_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> STRUCT_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> ENUM_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CLASS_IN_SUPERTYPE_FOR_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INTERFACE_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> STRUCT_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> EXTEND_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjTypeReference, CangJieType> FINAL_SUPERTYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjTypeReference> EXTEND_CANNOT_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_MULTILINE_NEWLINE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<CjElement> EXPRESSION_EXPECTED_PACKAGE_FOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpression, ClassifierDescriptorWithTypeParameters> NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> UNEXPECTED_SAFE_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CangJieType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<CjExpression> ILLEGAL_SELECTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjQualifiedExpression> SAFE_CALL_WILL_CHANGE_NULLABILITY = DiagnosticFactory0.create(WARNING, PositioningStrategies.CALL_ELEMENT_WITH_DOT);
    DiagnosticFactory1<PsiElement, String> COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<PsiElement> COMPILER_AFFECTED_SYNTAX_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpression, TypeParameterDescriptor> TYPE_PARAMETER_IS_NOT_AN_EXPRESSION =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjExpression, ClassifierDescriptor> EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjTypeArgumentList> TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjTypeReference, CangJieType> TYPEALIAS_SHOULD_EXPAND_TO_CLASS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjSimpleNameExpression, CjTypeConstraint, CjTypeParameterListOwner> NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjDeclaration> CONSTRUCTOR_IN_INTERFACE = DiagnosticFactory0.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory0<CjTypeReference> DYNAMIC_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjSimpleNameExpression> PACKAGE_CANNOT_BE_IMPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjSimpleNameExpression> MODULE_PACKAGE_CANNOT_BE_IMPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjPackageDirective> INCONSISTENT_PACKAGE_MACOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> INT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, String> PACKAGE_OR_CLASSIFIER_REDECLARATION =
            DiagnosticFactory1.create(ERROR, FOR_REDECLARATION);
    DiagnosticFactory0<CjDeclarationWithBody>
            NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION = DiagnosticFactory0.create(ERROR, DECLARATION_WITH_BODY);
    DiagnosticFactory0<CjModifierListOwner>
            ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS = DiagnosticFactory0.create(ERROR, ABSTRACT_MODIFIER);


    DiagnosticFactory0<CjReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY =
            DiagnosticFactory0.create(ERROR, PositioningStrategies.RETURN_WITH_LABEL);
    DiagnosticFactory2<CjConstantExpression, Long, CangJieType> INT_LITERAL_OUT_OF_RANGE_BY_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> FLOAT_LITERAL_OUT_OF_RANGE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> INCORRECT_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> EMPTY_CHARACTER_LITERAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjConstantExpression, String> TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjElement, CjElement> ILLEGAL_ESCAPE = DiagnosticFactory1.create(ERROR, CUT_CHAR_QUOTES);
    DiagnosticFactory1<CjParameter, VariableDescriptor> UNUSED_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<PsiElement, DeclarationDescriptor, String> DEPRECATION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<CjParameter> DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE = DiagnosticFactory0.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<CjElement> SUBTYPING_BETWEEN_CONTEXT_RECEIVERS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<CjParameter, VariableDescriptor> UNUSED_ANONYMOUS_PARAMETER = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    ImmutableSet<? extends DiagnosticFactory<?>> UNUSED_ELEMENT_DIAGNOSTICS = ImmutableSet.of(
            UNUSED_PARAMETER/*, UNUSED_VARIABLE,  ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, VARIABLE_WITH_REDUNDANT_INITIALIZER,
            UNUSED_LAMBDA_EXPRESSION, USELESS_CAST, UNUSED_VALUE, USELESS_ELVIS, UNNECESSARY_LATEINIT, REDUNDANT_ELSE_IN_WHEN*/);
    DiagnosticFactory1<CjDestructuringDeclarationEntry, VariableDescriptor> UNUSED_DESTRUCTURED_PARAMETER_ENTRY =
            DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    // Error sets
    ImmutableSet<? extends DiagnosticFactory<?>> UNRESOLVED_REFERENCE_DIAGNOSTICS = ImmutableSet.of(
            UNRESOLVED_REFERENCE, NAMED_PARAMETER_NOT_FOUND, UNRESOLVED_REFERENCE_WRONG_RECEIVER);


    DiagnosticFactory1<CjPackageDirective, FqName> INCONSISTENT_PACKAGE_MODIFIERS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, CjKeywordToken, String> WRONG_MODIFIER_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_INFO = DiagnosticFactory1.create(INFO);
    DiagnosticFactory1<CjCallExpression, DeclarationDescriptor> NO_CALL_OPERATOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, String, CangJieType> INVALID_MACRO_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> EXCESSIVE_MACRO_PARAMS = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<PsiElement, PsiElement> STATIC_INSTANCE_ACCESS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, DescriptorKind, DeclarationDescriptor> STATIC_CONTEXT_REFERENCE_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, DescriptorKind, DeclarationDescriptor> INSTANCE_ACCESS_STATIC_MEMBER_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjTypeReference> WRONG_SETTER_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjPropertyAccessor> LET_WITH_SETTER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory1<CjImportDirective, FqName> SELF_IMPORT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjImportDirective, FqName, DescriptorVisibility> IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> FLOAT_LITERAL_CONFORMS_INFINITY = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjConstantExpression> FLOAT_LITERAL_CONFORMS_ZERO = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement> MAIN_FUNCTION_RETURN_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameterList> MAIN_FUNCTION_PARAMETER_COUNT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjParameter> MAIN_FUNCTION_PARAMETER_TYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjMainFunction> MAIN_FUNCTION_NUMBER_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjVariableDeclaration> INTERFACE_BODY_NO_VARIABLES = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory2<PsiElement, FqName, FqName> MISSING_DEPENDENCY_SUPERCLASS = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory1<CjSuperTypeEntry, CangJieType> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> INCOMPATIBLE_MODIFIERS =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, Name> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CjKeywordToken> REPEATED_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, CjKeywordToken, CjKeywordToken> DEPRECATED_MODIFIER_PAIR =
            DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<CjElement> EXPLICIT_BACKING_FIELDS_UNSUPPORTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjPropertyAccessor> ABSTRACT_PROPERTY_WITH_GETTER = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjExpression> ABSTRACT_PROPERTY_WITH_INITIALIZER = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<PsiElement> SEALED_ABSTRACT =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeProjection> PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE =
            DiagnosticFactory0.create(ERROR, VARIANCE_IN_PROJECTION);
    DiagnosticFactory1<CjTypeElement, CangJieType> EXPANDED_TYPE_CANNOT_BE_INHERITED = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<CjTypeStatement, ClassDescriptor, Collection<CallableMemberDescriptor>> CONFLICTING_INHERITED_MEMBERS =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<CjTypeReference> REPEATED_BOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> ONLY_ONE_CLASS_BOUND_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> VAR_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<PsiElement, ClassDescriptor> INACCESSIBLE_OUTER_CLASS_EXPRESSION =
            DiagnosticFactory1.create(ERROR, SECONDARY_CONSTRUCTOR_DELEGATION_CALL);
    DiagnosticFactory2<CjConstantExpression, String, CangJieType> CONSTANT_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_INHERITANCE =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory0<CjTypeReference> BOUND_ON_TYPE_ALIAS_PARAMETER_NOT_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);
    DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> VAR_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> PROPERTY_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory0<CjConstructorDelegationReferenceExpression> CYCLIC_CONSTRUCTOR_DELEGATION_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjNamedDeclaration, VariableDescriptor> UNUSED_VARIABLE = DiagnosticFactory1.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory1<CjElement, CjElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<CjSimpleNameExpression, ValueParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, ClassDescriptor> UNINITIALIZED_ENUM_ENTRY = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjElement, VariableDescriptor> IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION = DiagnosticFactory1.create(ERROR);


    DiagnosticFactory3<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor>
            VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(WARNING, DECLARATION_NAME);

    DiagnosticFactoryForDeprecation2<CjTypeStatement, ClassDescriptor, Collection<CallableMemberDescriptor>>
            INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER =
            DiagnosticFactoryForDeprecation2.create(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses, DECLARATION_NAME);
    DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, DeclarationWithDiagnosticComponents> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    DiagnosticFactory2<CjNamedDeclaration, PropertyDescriptor, PropertyDescriptor> LET_OVERRIDDEN_BY_VAR =
            DiagnosticFactory2.create(ERROR, LET_OR_VAR_NODE);
    DiagnosticFactory2<CjNamedDeclaration, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_LET =
            DiagnosticFactory2.create(ERROR, LET_OR_VAR_NODE);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> VAR_OVERRIDDEN_BY_LET_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_SETTER =
            DiagnosticFactory3.create(ERROR);

    DiagnosticFactory1<CjExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<CjTypeStatement, Collection<? extends CallableMemberDescriptor>, Integer>
            DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> OVERRIDING_FINAL_MEMBER_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<CjTypeStatement, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_MATCH_NO_EXPLICIT_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<CjModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE =
            DiagnosticFactory1.create(ERROR, OVERRIDE_MODIFIER);
    DiagnosticFactory2<CjModifierListOwner, CallableMemberDescriptor, CallableDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER =
            DiagnosticFactory2.create(ERROR, OVERRIDE_MODIFIER);
    DiagnosticFactory3<CjModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor>
            CANNOT_CHANGE_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory1<CjParameter, ValueParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> CYCLIC_SCOPES_WITH_COMPANION = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpression, DeclarationDescriptor> CAPTURED_MEMBER_LET_INITIALIZATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjExpression, DeclarationDescriptor> CAPTURED_LET_INITIALIZATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactoryForDeprecation1<CjExpression, DeclarationDescriptor> LET_REASSIGNMENT_VIA_BACKING_FIELD =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.RestrictionOfLetReassignmentViaBackingField);

    DiagnosticFactory1<CjDeclaration, CallableMemberDescriptor> CANNOT_INFER_VISIBILITY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjPackageDirective, FqName, FqName> PACKAGE_ACCESS_VIOLATION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjParameterList> UNSAFE_EXPRESSION_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjPackageDirective> INCONSISTENT_MACRO_PACKAGE_NAME = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<CjModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor>
            CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory0<CjParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);
    DiagnosticFactory2<CjImportDirective, FqName, FqName> CYCLIC_IMPORT = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<CjParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

    DiagnosticFactory0<PsiElement> CONSTRUCTOR_NAME_INCONSISTENCY =
            DiagnosticFactory0.create(ERROR);


    DiagnosticFactory1<CjPrimaryConstructor, ClassDescriptor> MULTIPLE_PRIMARY_CONSTRUCTORS =
            DiagnosticFactory1.create(ERROR);

    DiagnosticFactory0<CjConstructorDelegationReferenceExpression> INLETID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);


    ImmutableSet<? extends DiagnosticFactory<?>> MUST_BE_INITIALIZED_DIAGNOSTICS = ImmutableSet.of(
            MUST_BE_INITIALIZED, MUST_BE_INITIALIZED_OR_BE_ABSTRACT
    );


    /*************恶搞警告***********************/
    DiagnosticFactory0<CjOptionType> NESTING_DOLL_OPTINOTYPE = DiagnosticFactory0.create(WARNING, OPTIONAL_TYPE);
    /************************************/

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("UnusedDeclaration")
    Initializer __initializer = Initializer.INSTANCE;

    enum BadNamedArgumentsTarget {
        NON_CANGJIE_FUNCTION, // a function provided by non-CangJie artifact, ex: Java function
        INTEROP_FUNCTION, // deserialized CangJie function that serves as a bridge to a function written in another language, ex: Obj-C
        INVOKE_ON_FUNCTION_TYPE,
        EXPECTED_CLASS_MEMBER,
    }

    class Initializer {
        private static final String WARNING = "_WARNING";
        private static final String ERROR = "_ERROR";
        private static final Initializer INSTANCE = new Initializer();

        static {
            initializeFactoryNames(Errors.class);
        }

        private Initializer() {
        }

        public static void initializeFactoryNames(@NotNull Class<?> aClass) {
            initializeFactoryNamesAndDefaultErrorMessages(aClass, DiagnosticFactoryToRendererMap::new);
        }

        public static void initializeFactoryNamesAndDefaultErrorMessages(
                @NotNull Class<?> aClass,
                @NotNull DefaultErrorMessages.Extension defaultErrorMessages
        ) {
            DiagnosticFactoryToRendererMap diagnosticToRendererMap = defaultErrorMessages.getMap();
            for (Field field : aClass.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof DiagnosticFactory) {
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName(), (DiagnosticFactory<?>) value);
                        }
                        if (value instanceof DiagnosticFactoryForDeprecation<?, ?, ?> factory) {
                            String errorName = field.getName();
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName() + ERROR, factory.getErrorFactory());
                            initializeNameAndRenderer(diagnosticToRendererMap, field.getName() + WARNING, factory.getWarningFactory());
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static void initializeNameAndRenderer(
                DiagnosticFactoryToRendererMap diagnosticToRendererMap,
                String name,
                DiagnosticFactory<?> factory
        ) {
            factory.initializeName(name);
            factory.setDefaultRenderer((DiagnosticRenderer) diagnosticToRendererMap.get(factory));
        }
    }
}
