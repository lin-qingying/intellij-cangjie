package com.huawei.cangjie.descriptors;

import com.google.common.collect.ImmutableSet;
import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.diagnostics.*;
import com.huawei.cangjie.diagnostics.rendering.DeclarationWithDiagnosticComponents;
import com.huawei.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.huawei.cangjie.lexer.CjModifierKeywordToken;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintPosition;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability;
import com.huawei.cangjie.resolve.calls.tower.WrongResolutionToClassifier;
import com.huawei.cangjie.types.CangJieType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static com.huawei.cangjie.descriptors.PositioningStrategies.*;
import static com.huawei.cangjie.diagnostics.Severity.*;


/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 */
public interface Errors {
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
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, String> DEPRECATED_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, String> REDUNDANT_MODIFIER_FOR_TARGET = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<CjExpression, CangJieType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> UNDERSCORE_IS_RESERVED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjTypeReference, CangJieType, CangJieType> UPPER_BOUND_VIOLATED_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory1<PsiElement, String> INFERRED_INTO_DECLARED_UPPER_BOUNDS = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<CjReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(ERROR, PositioningStrategies.RETURN_WITH_LABEL);
    DiagnosticFactory0<CjExpression> SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<CjSimpleNameExpression, DeclarationDescriptor, DescriptorVisibility, FqName> INVISIBLE_REFERENCE_REEXPORT =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<CjExpression, String, Collection<? extends ResolvedCall<?>>> DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjElement, DeclarationDescriptor> DEPRECATED_ACCESS_BY_SHORT_NAME = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjElement, Integer, DeclarationDescriptor> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory2.create(ERROR);
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
    DiagnosticFactory1<CjElement, CallableDescriptor> COMPATIBILITY_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, CangJieType> UNSAFE_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CangJieType> UNSAFE_IMPLICIT_INVOKE_CALL = DiagnosticFactory1.create(ERROR);

    DiagnosticFactory2<CjExpression, CjExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(ERROR, CALL_EXPRESSION);
    DiagnosticFactory3<CjReferenceExpression, ClassifierDescriptor, WrongResolutionToClassifier, String> RESOLUTION_TO_CLASSIFIER =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_DIAGNOSTIC = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<PsiElement, CandidateApplicability, String> NEW_INFERENCE_UNKNOWN_ERROR = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<PsiElement, PropertyDescriptor, ClassDescriptor> DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY = DiagnosticFactory2.create(WARNING);
    //    DiagnosticFactoryForDeprecation0<LeafPsiElement> NON_VARARG_SPREAD =
//            DiagnosticFactoryForDeprecation0.create(LanguageFeature.ReportNonVarargSpreadOnGenericCalls);
    DiagnosticFactory0<CjConstructorDelegationReferenceExpression> DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR =
            DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<LeafPsiElement> SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(ERROR);
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
    DiagnosticFactory3<PsiElement, CangJieType, CangJieType, ConstraintPosition> TYPE_MISMATCH_IN_CONSTRAINT = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory4<CjElement, Name, Name, CangJieType, CangJieType> UPPER_BOUND_VIOLATION_IN_CONSTRAINT = DiagnosticFactory4.create(ERROR);
    DiagnosticFactory1<CjParameter, CangJieType> EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory2<PsiElement, CangJieType, CangJieType> RECEIVER_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory3<PsiElement, DeclarationDescriptor, DescriptorVisibility, DeclarationDescriptor> INVISIBLE_MEMBER =
            DiagnosticFactory3.create(ERROR, CALL_ELEMENT);
    DiagnosticFactory2<CjExpression, CangJieType, CangJieType> TYPE_MISMATCH_WARNING = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<CjExpression, CangJieType, CangJieType> TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<CjNamedDeclaration, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS =
            DiagnosticFactory1.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory1<CjElement, CangJieType> TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> MISSING_STDLIB = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, String> NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement > ARRAY_LITERAL_TYPE_INFERENCE_FAILED = DiagnosticFactory0.create(ERROR);

    DiagnosticFactoryForDeprecation1<PsiElement, TypeParameterDescriptor> TYPE_INFERENCE_ONLY_INPUT_TYPES =
            DiagnosticFactoryForDeprecation1.create(LanguageFeature.StrictOnlyInputTypesChecks);


    DiagnosticFactory1<PsiElement, String> COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE = DiagnosticFactory1.create(ERROR);

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
    DiagnosticFactory2<CjElement, CallableDescriptor, CangJieType> MEMBER_PROJECTED  = DiagnosticFactory2.create(ERROR);
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
    DiagnosticFactory1<CjElement, String> TYPE_ARGUMENTS_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, Pair<LanguageFeature, LanguageVersionSettings>> UNSUPPORTED_FEATURE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION =
            DiagnosticFactory3.create(ERROR);
    DiagnosticFactory3<CjSuperTypeList, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>>
            INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory0<CjOptionType> NULLABLE_ON_DEFINITELY_NOT_OPTIONAL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjOptionType> REDUNDANT_OPTIONAL = DiagnosticFactory0.create(WARNING, OPTIONAL_TYPE);

    DiagnosticFactory3<CjTypeParameter, TypeParameterDescriptor, ClassDescriptor, Collection<CangJieType>>
            INCONSISTENT_TYPE_PARAMETER_BOUNDS = DiagnosticFactory3.create(ERROR);

    DiagnosticFactory3<CjElement, CangJieType, CangJieType, ClassifierDescriptor> UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING =
            DiagnosticFactory3.create(WARNING);
    DiagnosticFactory0<CjTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> SUPERTYPE_NOT_A_CLASS_OR_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> MANY_CLASSES_IN_SUPERTYPE_LIST = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> STRUCT_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjTypeReference> ENUM_IN_SUPERTYPE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> CLASS_IN_SUPERTYPE_FOR_ENUM = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> INTERFACE_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> EXTEND_WITH_SUPERCLASS = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjTypeReference, CangJieType> FINAL_SUPERTYPE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<CjTypeReference> EXTEND_CANNOT_INTERFACE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> NO_MULTILINE_NEWLINE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory0<CjSimpleNameExpression> EXPRESSION_EXPECTED_PACKAGE_FOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjExpression, ClassifierDescriptorWithTypeParameters> NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<PsiElement> UNEXPECTED_SAFE_CALL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<PsiElement, CangJieType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory0<CjExpression> ILLEGAL_SELECTOR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<CjQualifiedExpression> SAFE_CALL_WILL_CHANGE_NULLABILITY = DiagnosticFactory0.create(WARNING, PositioningStrategies.CALL_ELEMENT_WITH_DOT);

    DiagnosticFactory0<PsiElement> COMPILER_AFFECTED_SYNTAX_ERROR = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, TypeParameterDescriptor> TYPE_PARAMETER_IS_NOT_AN_EXPRESSION =
            DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, ClassifierDescriptor> EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE = DiagnosticFactory1.create(ERROR);
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
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, String> WRONG_MODIFIER_TARGET = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_ERROR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_WARNING = DiagnosticFactory1.create(WARNING);
    DiagnosticFactory1<PsiElement, RenderedDiagnostic<?>> PLUGIN_INFO = DiagnosticFactory1.create(INFO);

    DiagnosticFactory1<CjImportDirective, FqName> SELF_IMPORT_NOT_ALLOWED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<CjImportDirective, FqName, DescriptorVisibility> IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<CjConstantExpression> FLOAT_LITERAL_CONFORMS_INFINITY = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<CjConstantExpression> FLOAT_LITERAL_CONFORMS_ZERO = DiagnosticFactory0.create(WARNING);

    DiagnosticFactory2<PsiElement, FqName, FqName> MISSING_DEPENDENCY_SUPERCLASS = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory0<CjSuperTypeEntry> SUPERTYPE_NOT_INITIALIZED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, CjModifierKeywordToken> INCOMPATIBLE_MODIFIERS =
            DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<CjSimpleNameExpression, Name> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<PsiElement, CjModifierKeywordToken> REPEATED_MODIFIER = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, CjModifierKeywordToken> REDUNDANT_MODIFIER = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory2<PsiElement, CjModifierKeywordToken, CjModifierKeywordToken> DEPRECATED_MODIFIER_PAIR =
            DiagnosticFactory2.create(WARNING);

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


    DiagnosticFactory3<CjNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor>
            VIRTUAL_MEMBER_HIDDEN =
            DiagnosticFactory3.create(WARNING, DECLARATION_NAME);

    DiagnosticFactoryForDeprecation2<CjTypeStatement, ClassDescriptor, Collection<CallableMemberDescriptor>>
            INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER =
            DiagnosticFactoryForDeprecation2.create(LanguageFeature.ProhibitInvisibleAbstractMethodsInSuperclasses, DECLARATION_NAME);
    DiagnosticFactory2<CjNamedDeclaration, CallableMemberDescriptor, DeclarationWithDiagnosticComponents> RETURN_TYPE_MISMATCH_ON_OVERRIDE =
            DiagnosticFactory2.create(ERROR, DECLARATION_RETURN_TYPE);
    //    DiagnosticFactory2<CjNamedDeclaration, PropertyDescriptor, PropertyDescriptor> VAR_OVERRIDDEN_BY_LET =
//            DiagnosticFactory2.create(ERROR, LET_OR_VAR_NODE);
    DiagnosticFactory2<CjNamedDeclaration, VariableDescriptor, VariableDescriptor> VAR_OVERRIDDEN_BY_LET =
            DiagnosticFactory2.create(ERROR, LET_OR_VAR_NODE);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CallableMemberDescriptor, CallableMemberDescriptor> VAR_OVERRIDDEN_BY_LET_BY_DELEGATION =
            DiagnosticFactory2.create(ERROR, DECLARATION_NAME);
    DiagnosticFactory2<CjTypeStatement, CjTypeStatement, CallableMemberDescriptor> MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);

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

    DiagnosticFactory1<CjDeclaration, CallableMemberDescriptor> CANNOT_INFER_VISIBILITY =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);
    DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjPackageDirective, FqName, FqName> PACKAGE_ACCESS_VIOLATION = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory3<CjModifierListOwner, DescriptorVisibility, CallableMemberDescriptor, DeclarationDescriptor>
            CANNOT_WEAKEN_ACCESS_PRIVILEGE =
            DiagnosticFactory3.create(ERROR, VISIBILITY_MODIFIER);
    DiagnosticFactory0<CjParameter> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(ERROR, PARAMETER_DEFAULT_VALUE);
    DiagnosticFactory2<CjImportDirective, FqName, FqName> CYCLIC_IMPORT = DiagnosticFactory2.create(ERROR);

    DiagnosticFactory2<CjParameter, ClassDescriptor, ValueParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE =
            DiagnosticFactory2.create(WARNING, DECLARATION_NAME);














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
