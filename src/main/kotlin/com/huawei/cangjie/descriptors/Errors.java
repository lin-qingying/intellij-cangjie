package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.diagnostics.*;
import com.huawei.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintPosition;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability;
import com.huawei.cangjie.resolve.calls.tower.WrongResolutionToClassifier;
import com.huawei.cangjie.types.CangJieType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import static com.huawei.cangjie.descriptors.PositioningStrategies.*;
import static com.huawei.cangjie.diagnostics.Severity.ERROR;
import static com.huawei.cangjie.diagnostics.Severity.WARNING;

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
    DiagnosticFactory1<CjReferenceExpression, CjReferenceExpression> UNRESOLVED_REFERENCE =
            DiagnosticFactory1.create(ERROR, FOR_UNRESOLVED_REFERENCE);
    DiagnosticFactory0<CjExpression> NO_RECEIVER_ALLOWED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<CjExpression, CjExpression, CangJieType> FUNCTION_EXPECTED = DiagnosticFactory2.create(ERROR);





    DiagnosticFactory2<CjConstantExpression, String, CangJieType> CONSTANT_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(ERROR);



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
