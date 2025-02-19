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

package com.linqingying.cangjie.diagnostics.rendering;

import com.linqingying.cangjie.diagnostics.UnboundDiagnostic;
import com.linqingying.cangjie.types.CangJieTypeKt;
import com.linqingying.cangjie.utils.AddToStdlibKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.linqingying.cangjie.diagnostics.Errors.*;
import static com.linqingying.cangjie.diagnostics.rendering.CommonRenderers.STRING;
import static com.linqingying.cangjie.diagnostics.rendering.CommonRenderers.THROWABLE;
import static com.linqingying.cangjie.diagnostics.rendering.Renderers.*;


public enum DefaultErrorMessages {
    ;
    private static final List<DiagnosticFactoryToRendererMap> RENDERER_MAPS;
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");

    static {
        RENDERER_MAPS = List.of(MAP);

    }

    static {
        MAP.put(RESOLUTION_TO_CLASSIFIER, () -> {
            return "{2}";
        }, NAMED, TO_STRING, STRING);

        MAP.put(MESSAGE_ERROR, () -> {
//            "{0}"

            return CangJieDiagnosisBundle.rawMessage(MESSAGE_ERROR);
        }, STRING);

        MAP.put(COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE, () -> {
//            "Affected by the compiler, this writing will report an error : {0}"
            return CangJieDiagnosisBundle.rawMessage(COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE);
        }, STRING);

        MAP.put(COMPILER_AFFECTED_SYNTAX_ERROR, () -> {

//            "Affected by the compiler, this writing will report an error"
            return CangJieDiagnosisBundle.rawMessage(COMPILER_AFFECTED_SYNTAX_ERROR);
        });


/**************声明检查************************************************/
//未定义

        MAP.put(UNSUPPORTED, () -> {
            // "Unsupported [{0}]"
            return CangJieDiagnosisBundle.rawMessage(UNSUPPORTED);
        }, STRING);

        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, () -> {
            // "Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: {0}"
            return CangJieDiagnosisBundle.rawMessage(UNRESOLVED_REFERENCE_WRONG_RECEIVER);
        }, AMBIGUOUS_CALLS);

        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, () -> {
            // "Unresolved reference. "
            return CangJieDiagnosisBundle.rawMessage(UNRESOLVED_REFERENCE_WRONG_RECEIVER);
        }, AMBIGUOUS_CALLS);

//重复定义

        MAP.put(CONFLICTING_OVERLOADS, () -> {
            // "Conflicting overloads: {0}"
            return CangJieDiagnosisBundle.rawMessage(CONFLICTING_OVERLOADS);
        }, CommonRenderers.commaSeparated(FQ_NAMES_IN_TYPES));
        MAP.put(CONFLICTING_STATIC, () -> {
            // "Conflicting overloads: {0}"
            return CangJieDiagnosisBundle.rawMessage(CONFLICTING_STATIC);
        }, CommonRenderers.commaSeparated(FQ_NAMES_IN_TYPES), STRING);

        MAP.put(PACKAGE_OR_CLASSIFIER_REDECLARATION, () -> {
            // "Redeclaration: {0}"
            return CangJieDiagnosisBundle.rawMessage(PACKAGE_OR_CLASSIFIER_REDECLARATION);
        }, STRING);

        MAP.put(REDECLARATION, () -> {
            // "Conflicting declarations: {0}"
            return CangJieDiagnosisBundle.rawMessage(REDECLARATION);
        }, CommonRenderers.commaSeparated(COMPACT_WITH_MODIFIERS));
        MAP.put(INCONSISTENT_MACRO_PACKAGE_NAME, () -> {
            // "Enumeration constructor parameters are the same: {0}"
            return CangJieDiagnosisBundle.rawMessage(INCONSISTENT_MACRO_PACKAGE_NAME);
        });
        MAP.put(ENUM_REDECLARATION, () -> {
            // "Enumeration constructor parameters are the same: {0}"
            return CangJieDiagnosisBundle.rawMessage(ENUM_REDECLARATION);
        }, STRING);

        MAP.put(FUNCTION_CALL_EXPECTED, () -> {
                    // "Function invocation ''{0}({1})'' expected"
                    return CangJieDiagnosisBundle.rawMessage(FUNCTION_CALL_EXPECTED);
                }, ELEMENT_TEXT,
                (hasValueParameters, context) -> hasValueParameters ? "..." : "");

        MAP.put(NO_VALUE_FOR_PARAMETER, () -> {
            // "No value passed for parameter ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(NO_VALUE_FOR_PARAMETER);
        }, NAMED);

        MAP.put(UNRESOLVED_REFERENCE, () -> {
            // "Reference not found: {0}"
            return CangJieDiagnosisBundle.rawMessage(UNRESOLVED_REFERENCE);
        }, ELEMENT_IDENTIFIER_TEXT);

//函数
        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, () -> {
            // "Overload resolution ambiguity: {0}"
            return CangJieDiagnosisBundle.rawMessage(OVERLOAD_RESOLUTION_AMBIGUITY);
        }, AMBIGUOUS_CALLS);

// 二进制重载
        MAP.put(INVALID_BINARY_OPERATOR, () -> {
            // "Invalid binary operator ''{0}'' on type ''{1}'' and ''{2}'', you may want to implement 'operator func '{3}'(right: '{4}')' for type ''{5}''"
            return CangJieDiagnosisBundle.rawMessage(INVALID_BINARY_OPERATOR);
        }, object -> {
            RenderingContext context = RenderingContext.of(object.getOperatorString(), object.getLeftType(), object.getRightType(), object.getOperatorString(), object.getRightType(), object.getLeftType());
            return new String[]{
                    STRING.render(object.getOperatorString(), context),
                    RENDER_TYPE.render(object.getLeftType(), context),
                    RENDER_TYPE.render(object.getRightType(), context),
                    STRING.render(object.getOperatorString(), context),
                    RENDER_TYPE.render(object.getRightType(), context),
                    RENDER_TYPE.render(object.getLeftType(), context)
            };
        });


//        常量检查
        // 常量检查
        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, () -> {
            // "The {0} literal does not conform to the expected type {1}"
            return CangJieDiagnosisBundle.rawMessage(CONSTANT_EXPECTED_TYPE_MISMATCH);
        }, STRING, RENDER_TYPE);

        MAP.put(WRONG_MODIFIER_TARGET, () -> {
            // "Modifier ''{0}'' is not applicable to ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(WRONG_MODIFIER_TARGET);
        }, TO_STRING, TO_STRING);

        MAP.put(INT_LITERAL_OUT_OF_RANGE, () -> {
            // "The value is out of range"
            return CangJieDiagnosisBundle.rawMessage(INT_LITERAL_OUT_OF_RANGE);
        });
        MAP.put(CANNOT_CHECK_FOR_ERASED, () -> {

//"Cannot check for instance of erased type: {0}"


            return CangJieDiagnosisBundle.rawMessage(CANNOT_CHECK_FOR_ERASED);
        }, RENDER_TYPE);
        MAP.put(INAPPLICABLE_OPERATOR_MODIFIER, () -> {

//            "''operator'' modifier is inapplicable on this function: {0}"
            return CangJieDiagnosisBundle.rawMessage(INAPPLICABLE_OPERATOR_MODIFIER);
        }, STRING);

        MAP.put(FLOAT_LITERAL_OUT_OF_RANGE, () -> {
            // "The value is out of range"
            return CangJieDiagnosisBundle.rawMessage(FLOAT_LITERAL_OUT_OF_RANGE);
        });
        MAP.put(VARRAY_SIZE_MISMATCH, () -> {
            // "mismatch 'VArray' type's size"
            return CangJieDiagnosisBundle.rawMessage(VARRAY_SIZE_MISMATCH);
        });

        MAP.put(INCORRECT_CHARACTER_LITERAL, () -> {
            // "Incorrect character literal"
            return CangJieDiagnosisBundle.rawMessage(INCORRECT_CHARACTER_LITERAL);
        });

        MAP.put(EMPTY_CHARACTER_LITERAL, () -> {
            // "Empty character literal"
            return CangJieDiagnosisBundle.rawMessage(EMPTY_CHARACTER_LITERAL);
        });

        MAP.put(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL, () -> {
            // "Too many characters in a character literal ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL);
        }, TO_STRING);


        MAP.put(INT_LITERAL_OUT_OF_RANGE_BY_TYPE, () -> {
            // "the number ''{0}'' exceeds the value range of type ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(INT_LITERAL_OUT_OF_RANGE_BY_TYPE);
        }, TO_STRING, RENDER_TYPE);

// 包导入
        MAP.put(PACKAGE_CANNOT_BE_IMPORTED, () -> {
            // "Packages cannot be imported"
            return CangJieDiagnosisBundle.rawMessage(PACKAGE_CANNOT_BE_IMPORTED);
        });

        MAP.put(MODULE_PACKAGE_CANNOT_BE_IMPORTED, () -> {
            // "Module name cannot be imported"
            return CangJieDiagnosisBundle.rawMessage(MODULE_PACKAGE_CANNOT_BE_IMPORTED);
        });

        MAP.put(SELF_IMPORT_NOT_ALLOWED, () -> {
            // "Package ''{0}'' should not import itself"
            return CangJieDiagnosisBundle.rawMessage(SELF_IMPORT_NOT_ALLOWED);
        }, FQNAME);

        MAP.put(IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED, () -> {
            // "Imported package name ''{0}'' cannot be modified by ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED);
        }, FQNAME, VISIBILITY);

        MAP.put(CYCLIC_IMPORT, () -> {
            // "packages ''{0}'' ''{1}'' are in circular dependencies."
            return CangJieDiagnosisBundle.rawMessage(CYCLIC_IMPORT);
        }, FQNAMES);

        MAP.put(PACKAGE_ACCESS_VIOLATION, () -> {
            // "The access level of child package ''{0}'' cannot be higher than that of parent package ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(PACKAGE_ACCESS_VIOLATION);
        }, FQNAME, FQNAME);


//        访问控制
        MAP.put(INVISIBLE_REFERENCE_REEXPORT, () -> {
            // "Cannot access ''{0}'': it is {1} in {2}"
            return CangJieDiagnosisBundle.rawMessage(INVISIBLE_REFERENCE_REEXPORT);
        }, NAMED, VISIBILITY, FQNAME);

        MAP.put(INVISIBLE_REFERENCE, () -> {
            // "Cannot access ''{0}'': it is {1} in {2}"
            return CangJieDiagnosisBundle.rawMessage(INVISIBLE_REFERENCE);
        }, NAMED, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);

        MAP.put(INCONSISTENT_PACKAGE_MODIFIERS, () -> {
            // "Inconsistent modifiers for package ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(INCONSISTENT_PACKAGE_MODIFIERS);
        }, FQNAME);

        MAP.put(INCONSISTENT_PACKAGE_MACOR, () -> {
            // "Inconsistent macro package declarations"
            return CangJieDiagnosisBundle.rawMessage(INCONSISTENT_PACKAGE_MACOR);
        });

        MAP.put(OVERRIDING_FINAL_MEMBER, () -> {
            // "''{0}'' cannot be referenced from a static context"
            return CangJieDiagnosisBundle.rawMessage(OVERRIDING_FINAL_MEMBER);
        }, NAMED, NAMED);

        MAP.put(NO_CALL_OPERATOR, () -> {
            // "Calls are not allowed {0}"
            return CangJieDiagnosisBundle.rawMessage(NO_CALL_OPERATOR);
        }, NAMED_ADN_PARAMETER);
        MAP.put(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY, () -> {
//            "Callable reference resolution ambiguity: {0}"
            return CangJieDiagnosisBundle.rawMessage(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY);
        }, AMBIGUOUS_CALLABLE_REFERENCES);


        MAP.put(STATIC_CONTEXT_REFERENCE_ERROR, () -> {
            // "Non-static {0} ''{1}'' cannot be referenced from a static context"
            return CangJieDiagnosisBundle.rawMessage(STATIC_CONTEXT_REFERENCE_ERROR);
        }, DESCRIPTOR_KIND_NAME, NAMED_ADN_PARAMETER);

        MAP.put(INSTANCE_ACCESS_STATIC_MEMBER_ERROR, () -> {
            // "Static {0} ''{1}'' accessed via instance reference"
            return CangJieDiagnosisBundle.rawMessage(INSTANCE_ACCESS_STATIC_MEMBER_ERROR);
        }, DESCRIPTOR_KIND_NAME, NAMED_ADN_PARAMETER);

        MAP.put(STATIC_INSTANCE_ACCESS, () -> {
            // "''{0}'' cannot be referenced from a static context"
            return CangJieDiagnosisBundle.rawMessage(STATIC_INSTANCE_ACCESS);
        }, EXPRESSION_TYPE_TEXT);
//宏声明检查
        MAP.put(INVALID_MACRO_TYPE, () -> {
            // "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''"
            return CangJieDiagnosisBundle.rawMessage(INVALID_MACRO_TYPE);
        }, TO_STRING, RENDER_TYPE);
        MAP.put(EXCESSIVE_MACRO_PARAMS, () -> {
            // "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''"
            return CangJieDiagnosisBundle.rawMessage(EXCESSIVE_MACRO_PARAMS);
        });
//        类型检查

        MAP.put(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM,

                () -> {
//     "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"
                    return CangJieDiagnosisBundle.rawMessage("TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM");
                }
        );
        MAP.put(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT,
                () -> {
//            "Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly"
                    return CangJieDiagnosisBundle.rawMessage("TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT");

                }

        );

        MAP.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, () -> {
            // "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''"
            return CangJieDiagnosisBundle.rawMessage(CANNOT_WEAKEN_ACCESS_PRIVILEGE);
        }, VISIBILITY, NAMED, NAMED);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, () -> {
            // "{0} is not abstract and does not implement abstract member {1}"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_MEMBER_NOT_IMPLEMENTED);
        }, RENDER_TYPE_STATMENT, FQ_NAMES_IN_TYPES);

        MAP.put(VIRTUAL_MEMBER_HIDDEN, () -> {
            // "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier"
            return CangJieDiagnosisBundle.rawMessage(VIRTUAL_MEMBER_HIDDEN);
        }, NAMED, NAMED, NAMED, TO_STRING);
        MAP.put(REDEF_INSTANCE_ERROR, () -> {
            // "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier"
            return CangJieDiagnosisBundle.rawMessage(REDEF_INSTANCE_ERROR);
        }, PSI_NAMED_TYPE_NAM);
        MAP.put(OVERRIDE_STATIC_ERROR, () -> {
            // "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier"
            return CangJieDiagnosisBundle.rawMessage(OVERRIDE_STATIC_ERROR);
        }, PSI_NAMED_TYPE_NAM);
        MAP.put(NOTHING_TO_OVERRIDE, () -> {
            // "''{0}'' overrides nothing"
            return CangJieDiagnosisBundle.rawMessage(NOTHING_TO_OVERRIDE);
        }, NAMED);
        MAP.put(REDEF_NOTHING_TO_OVERRIDE, () -> {
            // "''{0}'' overrides nothing"
            return CangJieDiagnosisBundle.rawMessage(REDEF_NOTHING_TO_OVERRIDE);
        }, NAMED);
        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, () -> {
            // "Return type of ''{0}'' is not a subtype of the return type of the overridden member ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(RETURN_TYPE_MISMATCH_ON_OVERRIDE);
        }, NAMED, FQ_NAMES_IN_TYPES_ANNOTATIONS_WHITELIST);
        MAP.put(RETURN_TYPE_MISMATCH,
                () -> {

//                    "This function must return a value of type {0}"
                    return CangJieDiagnosisBundle.rawMessage(RETURN_TYPE_MISMATCH);
                }
                , RENDER_TYPE);

        String wrongNumberOfTypeArguments = "{0,choice,0#No type arguments|1#One type argument|1<{0,number,integer} type arguments} expected";
        MAP.put(TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY, () -> {
            // "type arguments cannot appear after ''{0}'' when enum type ''{1}'' is give"
            return CangJieDiagnosisBundle.rawMessage(TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY);
        }, NAMED, NAMED);

        MAP.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, () -> {
            // "wrongNumberOfTypeArguments + ' for {1}'"
            return CangJieDiagnosisBundle.rawMessage(WRONG_NUMBER_OF_TYPE_ARGUMENTS);
        }, null, COMPACT_WITHOUT_SUPERTYPES);

        MAP.put(MEMBER_PROJECTED, () -> {
            // "type ''{1}'' prohibits the use of ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(MEMBER_PROJECTED);
        }, FQ_NAMES_IN_TYPES, RENDER_TYPE);

        MAP.put(UPPER_BOUND_VIOLATED, () -> {
            // "Type argument is not within its bounds: should be subtype of ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(UPPER_BOUND_VIOLATED);
        }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(SUPERTYPE_APPEARS_TWICE, () -> {
            // "A supertype appears twice"
            return CangJieDiagnosisBundle.rawMessage(SUPERTYPE_APPEARS_TWICE);
        });

        MAP.put(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE, () -> {
            // "Only classes and interfaces may serve as supertypes"
            return CangJieDiagnosisBundle.rawMessage(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE);
        });

        MAP.put(CLASS_IN_SUPERTYPE_FOR_ENUM, () -> {
            // "Enum cannot inherit from classes"
            return CangJieDiagnosisBundle.rawMessage(CLASS_IN_SUPERTYPE_FOR_ENUM);
        });

        MAP.put(INTERFACE_WITH_SUPERCLASS, () -> {
            // "An interface can only inherit from a interface"
            return CangJieDiagnosisBundle.rawMessage(INTERFACE_WITH_SUPERCLASS);
        });

        MAP.put(STRUCT_WITH_SUPERCLASS, () -> {
            // "An struct can only inherit from a interface"
            return CangJieDiagnosisBundle.rawMessage(STRUCT_WITH_SUPERCLASS);
        });

        MAP.put(EXTEND_WITH_SUPERCLASS, () -> {
            // "An extend can only inherit from a interface"
            return CangJieDiagnosisBundle.rawMessage(EXTEND_WITH_SUPERCLASS);
        });

        MAP.put(CYCLIC_INHERITANCE_HIERARCHY, () -> {
            // "There's a cycle in the inheritance hierarchy for this type"
            return CangJieDiagnosisBundle.rawMessage(CYCLIC_INHERITANCE_HIERARCHY);
        });

        MAP.put(STRUCT_IN_SUPERTYPE, () -> {
            // "Cannot inherit from a struct"
            return CangJieDiagnosisBundle.rawMessage(STRUCT_IN_SUPERTYPE);
        });

        MAP.put(ENUM_IN_SUPERTYPE, () -> {
            // "Cannot inherit from a enum"
            return CangJieDiagnosisBundle.rawMessage(ENUM_IN_SUPERTYPE);
        });

        MAP.put(FINAL_SUPERTYPE, () -> {
            // "super class ''{0}'' is not inheritable"
            return CangJieDiagnosisBundle.rawMessage(FINAL_SUPERTYPE);
        }, RENDER_TYPE);

        MAP.put(MANY_CLASSES_IN_SUPERTYPE_LIST, () -> {
            // "Only one class may appear in a supertype list"
            return CangJieDiagnosisBundle.rawMessage(MANY_CLASSES_IN_SUPERTYPE_LIST);
        });

        MAP.put(EXTEND_CANNOT_INTERFACE, () -> {
            // "Extended interfaces are cannot allowed"
            return CangJieDiagnosisBundle.rawMessage(EXTEND_CANNOT_INTERFACE);
        });

        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, () -> {
            // "{0} is not abstract and does not implement abstract base class member {1}"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED);
        }, RENDER_TYPE_STATMENT, FQ_NAMES_IN_TYPES);

        MAP.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, () -> {
            // "The corresponding parameter in the supertype ''{0}'' is named ''{1}''. This may cause problems when calling this function with named arguments."
            return CangJieDiagnosisBundle.rawMessage(PARAMETER_NAME_CHANGED_ON_OVERRIDE);
        }, NAMED, NAMED);

//类型系统
        MAP.put(INVALID_THIS_TYPE, () -> {
            // "'This' type is not allowed, 'This' type can only be used as the return type of an instance member function in class"
            return CangJieDiagnosisBundle.rawMessage(INVALID_THIS_TYPE);
        });

        MAP.put(TYPE_MISMATCH_MULTIPLE_SUPERTYPES, () -> {
            // "Type inference failed: multiple smallest common supertypes found {0}"
            return CangJieDiagnosisBundle.rawMessage(TYPE_MISMATCH_MULTIPLE_SUPERTYPES);
        }, RENDER_COLLECTION_OF_TYPES);

        MAP.put(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS, () -> {
            // "Type mismatch: inferred type is {1} but {0} was expected. Projected type {2} restricts use of {3}"
            return CangJieDiagnosisBundle.rawMessage(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS);
        }, object -> {
            RenderingContext context = RenderingContext.of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(), object.getCallableDescriptor());
            return new String[]{
                    RENDER_TYPE.render(object.getExpectedType(), context),
                    RENDER_TYPE.render(object.getExpressionType(), context),
                    RENDER_TYPE.render(object.getReceiverType(), context),
                    FQ_NAMES_IN_TYPES.render(object.getCallableDescriptor(), context)
            };
        });


        MAP.put(TYPE_MISMATCH, () -> {
            // "Type mismatch: inferred type is {1} but {0} was expected"
            return CangJieDiagnosisBundle.rawMessage(TYPE_MISMATCH);
        }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(TOO_MANY_ARGUMENTS, () -> {
            // "Too many arguments for {0}"
            return CangJieDiagnosisBundle.rawMessage(TOO_MANY_ARGUMENTS);
        }, FQ_NAMES_IN_TYPES);

        MAP.put(ILLEGAL_SELECTOR, () -> {
            // "The expression cannot be a selector (occur after a dot)"
            return CangJieDiagnosisBundle.rawMessage(ILLEGAL_SELECTOR);
        });

        MAP.put(NEW_INFERENCE_UNKNOWN_ERROR, () -> {
            // "Unknown error in new inference with applicability ''{0}'' and target ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(NEW_INFERENCE_UNKNOWN_ERROR);
        }, TO_STRING, STRING);

        MAP.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, () -> {
            // "Right-hand side has anonymous type. Please specify type explicitly"
            return CangJieDiagnosisBundle.rawMessage(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED);
        }, TO_STRING);

        MAP.put(NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, () -> {
            // "Not enough information to infer type variable {0} from {1}"
            return CangJieDiagnosisBundle.rawMessage(NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER);
        }, STRING, TYPE_AND_NAMED);

        MAP.put(ARRAY_LITERAL_TYPE_INFERENCE_FAILED, () -> {
            // "array literal type cannot be inferred"
            return CangJieDiagnosisBundle.rawMessage(ARRAY_LITERAL_TYPE_INFERENCE_FAILED);
        });
        MAP.put(CONFLICTING_IMPORT, () -> {
//            "Conflicting import, imported name ''{0}'' is ambiguous"
            return CangJieDiagnosisBundle.rawMessage(CONFLICTING_IMPORT);
        }, STRING);

        MAP.put(UPPER_BOUND_VIOLATION_IN_CONSTRAINT, () -> {
            // "Upper bound violation for generic parameter `{0}` of `{1}`: {3} is not a subtype of {2}"
            return CangJieDiagnosisBundle.rawMessage(UPPER_BOUND_VIOLATION_IN_CONSTRAINT);
        }, TO_STRING, TO_STRING, RENDER_TYPE, RENDER_TYPE);
        MAP.put(RETURN_NOT_ALLOWED, () -> {
            // "'return' is not allowed here"
            return CangJieDiagnosisBundle.rawMessage(RETURN_NOT_ALLOWED);
        });
        MAP.put(IMPLICIT_INTERSECTION_TYPE, () -> {

//            "Inferred type {0} is an intersection, please specify the required type explicitly"
            return CangJieDiagnosisBundle.rawMessage(IMPLICIT_INTERSECTION_TYPE);
        }, RENDER_TYPE);

        MAP.put(NONE_APPLICABLE, () -> {
            // "None of the following functions can be called with the arguments supplied: {0}"
            return CangJieDiagnosisBundle.rawMessage(NONE_APPLICABLE);
        }, AMBIGUOUS_CALLS);

        MAP.put(EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE, () -> {
            // "Expected member name or constructor call after ''{0}'' type name"
            return CangJieDiagnosisBundle.rawMessage(EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE);
        }, NAMED);

        MAP.put(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, () -> {
            // "Type parameter ''{0}'' is not an expression"
            return CangJieDiagnosisBundle.rawMessage(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION);
        }, NAMED);

        MAP.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, () -> {
            // "Expression expected, but a package name found"
            return CangJieDiagnosisBundle.rawMessage(EXPRESSION_EXPECTED_PACKAGE_FOUND);
        });

        MAP.put(FUNCTION_EXPECTED, () -> {
            // "Expression ''{0}''{1} cannot be invoked as a function. No matching function for operator '()' function call"
            return CangJieDiagnosisBundle.rawMessage(FUNCTION_EXPECTED);
        }, ELEMENT_TEXT, (type, context) -> {
            if (CangJieTypeKt.isError(type)) return "";
            return " of type '" + RENDER_TYPE.render(type, context) + "'";
        });

        MAP.put(INVISIBLE_MEMBER, () -> {
            // "Cannot access ''{0}'': it is {1} in {2}"
            return CangJieDiagnosisBundle.rawMessage(INVISIBLE_MEMBER);
        }, NAMED, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);

        MAP.put(REDUNDANT_MODIFIER, () -> {
            // "Modifier ''{0}'' is redundant because ''{1}'' is present"
            return CangJieDiagnosisBundle.rawMessage(REDUNDANT_MODIFIER);
        }, TO_STRING, TO_STRING);

        MAP.put(SEALED_ABSTRACT, () -> {
            // "'sealed' can only modify abstract class"
            return CangJieDiagnosisBundle.rawMessage(SEALED_ABSTRACT);
        });

        MAP.put(INCOMPATIBLE_MODIFIERS, () -> {
            // "Modifier ''{0}'' is incompatible with ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(INCOMPATIBLE_MODIFIERS);
        }, TO_STRING, TO_STRING);

        MAP.put(WRONG_MODIFIER_TARGET, () -> {
            // "Modifier ''{0}'' is not applicable to ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(WRONG_MODIFIER_TARGET);
        }, TO_STRING, TO_STRING);

        MAP.put(NO_MULTILINE_NEWLINE, () -> {
            // "Multiline strings need to start with newline characters"
            return CangJieDiagnosisBundle.rawMessage(NO_MULTILINE_NEWLINE);
        });


        MAP.put(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, () -> {
            // "This variable must either have a type annotation or be initialized"
            return CangJieDiagnosisBundle.rawMessage(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER);
        });

        MAP.put(EXCEPTION_FROM_ANALYZER, () -> {
            // "Internal Error occurred while analyzing this expression:\n{0}"
            return CangJieDiagnosisBundle.rawMessage(EXCEPTION_FROM_ANALYZER);
        }, THROWABLE);

        MAP.put(REDUNDANT_OPTIONAL, () -> {
            // "Redundant '?'"
            return CangJieDiagnosisBundle.rawMessage(REDUNDANT_OPTIONAL);
        });


        MAP.put(NESTING_DOLL_OPTINOTYPE, () -> {
            Random random = new Random();

            List<String> messages = new ArrayList<>();
            messages.add("套娃套的好，编码没烦恼");
            messages.add("别套了，再套地球都让你套进去了");

            // 计算或构造你的值
            return messages.get(random.nextInt(messages.size()));
        });


//        声明
        MAP.put(FUNCTION_DECLARATION_WITH_NO_NAME, () -> {
            // "Function declaration must have a name"
            return CangJieDiagnosisBundle.rawMessage(FUNCTION_DECLARATION_WITH_NO_NAME);
        });

        MAP.put(ITERATOR_MISSING, () -> {
            // "For-loop range must have an 'iterator()' method"
            return CangJieDiagnosisBundle.rawMessage(ITERATOR_MISSING);
        });

        MAP.put(NAME_SHADOWING, () -> {
            // "Name shadowed: {0}"
            return CangJieDiagnosisBundle.rawMessage(NAME_SHADOWING);
        }, STRING);

        MAP.put(ACCESSOR_PARAMETER_NAME_SHADOWING, () -> {
            // "Accessor parameter name 'field' is shadowed by backing field variable"
            return CangJieDiagnosisBundle.rawMessage(ACCESSOR_PARAMETER_NAME_SHADOWING);
        });


        MAP.put(ASSIGNMENT_IN_EXPRESSION_CONTEXT, () -> {
            // "Assignments are not expressions, and only expressions are allowed in this context"
            return CangJieDiagnosisBundle.rawMessage(ASSIGNMENT_IN_EXPRESSION_CONTEXT);
        });

        MAP.put(VARIABLE_EXPECTED, () -> {
            // "Variable expected"
            return CangJieDiagnosisBundle.rawMessage(VARIABLE_EXPECTED);
        });

        MAP.put(LET_REASSIGNMENT, () -> {
            // "Cannot assign to immutable value {0}"
            return CangJieDiagnosisBundle.rawMessage(LET_REASSIGNMENT);
        }, NAMED);

        MAP.put(USELESS_ELVIS, () -> {
            // "Elvis operator (??) always returns the left operand of non-option type {0}"
            return CangJieDiagnosisBundle.rawMessage(USELESS_ELVIS);
        }, RENDER_TYPE);

        MAP.put(USELESS_ELVIS_RIGHT_IS_NULL, () -> {
            // "Right operand of elvis operator (??) is useless if it is Option"
            return CangJieDiagnosisBundle.rawMessage(USELESS_ELVIS_RIGHT_IS_NULL);
        });


        MAP.put(EXPRESSION_EXPECTED, () -> {
//            "{0} is not an expression, and only expressions are allowed here"
            return CangJieDiagnosisBundle.rawMessage(EXPRESSION_EXPECTED);

        }, (expression, context) -> {
            String expressionType = expression.toString();
            return expressionType.charAt(0) +
                    expressionType.substring(1).toLowerCase();
        });
        MAP.put(UNREACHABLE_CODE, () -> {
            // "Unreachable code"
            return CangJieDiagnosisBundle.rawMessage(UNREACHABLE_CODE);
        }, CommonRenderers.EMPTY, CommonRenderers.EMPTY);

        MAP.put(NAMED_PARAMETER_NOT_FOUND, () -> {
            // "Cannot find a parameter with this name: {0}"
            return CangJieDiagnosisBundle.rawMessage(NAMED_PARAMETER_NOT_FOUND);
        }, ELEMENT_TEXT);

        MAP.put(NAMED_PARAMETER_PREFIX_MISSING, () -> {
            // "missing argument prefix {0} for named parameter"
            return CangJieDiagnosisBundle.rawMessage(NAMED_PARAMETER_PREFIX_MISSING);
        }, NAMES_TO_STRING);

        MAP.put(NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER, () -> {
            // "unnamed parameters must come before named parameters"
            return CangJieDiagnosisBundle.rawMessage(NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER);
        });

        MAP.put(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT, () -> {
            // "positional argument cannot appear after named argument"
            return CangJieDiagnosisBundle.rawMessage(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT);
        });

        MAP.put(ARGUMENT_PASSED_TWICE, () -> {
            // "An argument is already passed for this parameter"
            return CangJieDiagnosisBundle.rawMessage(ARGUMENT_PASSED_TWICE);
        });

        MAP.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, () -> {
            // "Mixing named and positioned arguments is not allowed"
            return CangJieDiagnosisBundle.rawMessage(MIXING_NAMED_AND_POSITIONED_ARGUMENTS);
        });

        MAP.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, () -> {
            // "A 'return' expression required in a function with a block body ('{...}')"
            return CangJieDiagnosisBundle.rawMessage(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY);
        });

        MAP.put(EXPECTED_TYPE_MISMATCH, () -> {
            // "Expected a value of type {0}"
            return CangJieDiagnosisBundle.rawMessage(EXPECTED_TYPE_MISMATCH);
        }, RENDER_TYPE);

        MAP.put(DECLARATION_IN_ILLEGAL_CONTEXT, () -> {
            // "Declarations are not allowed in this position"
            return CangJieDiagnosisBundle.rawMessage(DECLARATION_IN_ILLEGAL_CONTEXT);
        });

        MAP.put(MAIN_FUNCTION_RETURN_TYPE, () -> {
            // "return type of 'main' is not 'Integer' or 'Unit'"
            return CangJieDiagnosisBundle.rawMessage(MAIN_FUNCTION_RETURN_TYPE);
        });

        MAP.put(MAIN_FUNCTION_PARAMETER_COUNT, () -> {
            // "'main' method has too many parameters"
            return CangJieDiagnosisBundle.rawMessage(MAIN_FUNCTION_PARAMETER_COUNT);
        });

        MAP.put(MAIN_FUNCTION_PARAMETER_TYPE, () -> {
            // "'main' cannot be defined with parameter whose type is not 'Array<String>'"
            return CangJieDiagnosisBundle.rawMessage(MAIN_FUNCTION_PARAMETER_TYPE);
        });

        MAP.put(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, () -> {
            // "Nested {0} accessed via instance reference"
            return CangJieDiagnosisBundle.rawMessage(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE);
        }, RENDER_CLASS_OR_OBJECT_NAME);

        MAP.put(MAIN_FUNCTION_NUMBER_ERROR, () -> {
            // "Multiple 'main' methods are defined in the module"
            return CangJieDiagnosisBundle.rawMessage(MAIN_FUNCTION_NUMBER_ERROR);
        });

        MAP.put(USELESS_IS_CHECK, () -> {
            // "'Check for instance is always ''{0}'"
            return CangJieDiagnosisBundle.rawMessage(USELESS_IS_CHECK);
        }, TO_STRING);

        MAP.put(INCOMPATIBLE_TYPES, () -> {
            // "Incompatible types: {0} and {1}"
            return CangJieDiagnosisBundle.rawMessage(INCOMPATIBLE_TYPES);
        }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(SUPERTYPE_NOT_INITIALIZED, () -> {
            // "There is no parameterless constructor available in ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(SUPERTYPE_NOT_INITIALIZED);
        }, RENDER_TYPE);

        MAP.put(EXPLICIT_DELEGATION_CALL_REQUIRED, () -> {
            // "Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments"
            return CangJieDiagnosisBundle.rawMessage(EXPLICIT_DELEGATION_CALL_REQUIRED);
        });

        MAP.put(CYCLIC_CONSTRUCTOR_DELEGATION_CALL, () -> {
            // "There's a cycle in the init calls chain"
            return CangJieDiagnosisBundle.rawMessage(CYCLIC_CONSTRUCTOR_DELEGATION_CALL);
        });

        MAP.put(NO_THIS, () -> {
            // "'this' is not defined in this context"
            return CangJieDiagnosisBundle.rawMessage(NO_THIS);
        });

        MAP.put(CONSTRUCTOR_NAME_INCONSISTENCY, () -> {
            // "Primary constructor name is inconsistent with type name"
            return CangJieDiagnosisBundle.rawMessage(CONSTRUCTOR_NAME_INCONSISTENCY);
        });

        MAP.put(MULTIPLE_PRIMARY_CONSTRUCTORS, () -> {
            // "Class ''{0}'' cannot have more than one primary constructor"
            return CangJieDiagnosisBundle.rawMessage(MULTIPLE_PRIMARY_CONSTRUCTORS);
        }, CLASS_NAME);

        MAP.put(INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR, () -> {
            // "Invalid calling 'this' in primary constructor"
            return CangJieDiagnosisBundle.rawMessage(INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR);
        });

        MAP.put(ENUM_ENTRY_AS_TYPE, () -> {
            // "Use of enum entry names as types is not allowed, use enum type instead"
            return CangJieDiagnosisBundle.rawMessage(ENUM_ENTRY_AS_TYPE);
        });

        MAP.put(UNSAFE_EXPRESSION_ERROR, () -> {
            // "'unsafe' is not a lambda expression"
            return CangJieDiagnosisBundle.rawMessage(UNSAFE_EXPRESSION_ERROR);
        });

        MAP.put(CANNOT_INFER_PARAMETER_TYPE, () -> {
            // "Cannot infer a type for this parameter. Please specify it explicitly."
            return CangJieDiagnosisBundle.rawMessage(CANNOT_INFER_PARAMETER_TYPE);
        });
        MAP.put(NO_ELSE_IN_MATCH_BY_PATTERN, () -> {
            // "''match'' expression must be exhaustive, add necessary {0}"
            return CangJieDiagnosisBundle.rawMessage(NO_ELSE_IN_MATCH_BY_PATTERN);
        }, RENDER_MATCH_MISSING_CASES_PATTERN);

        MAP.put(NO_ELSE_IN_MATCH, () -> {
            // "''match'' expression must be exhaustive, add necessary {0}"
            return CangJieDiagnosisBundle.rawMessage(NO_ELSE_IN_MATCH);
        }, RENDER_MATCH_MISSING_CASES);

        MAP.put(CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, () -> {
            // "Cannot import-on-demand from class ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON);
        }, NAMED);

        MAP.put(NOT_ENUM_ENTRY_VALUE, () -> {
            // "Not an enumerated value"
            return CangJieDiagnosisBundle.rawMessage(NOT_ENUM_ENTRY_VALUE);
        });
        MAP.put(INCOMPATIBLE_ENUM_COMPARISON, () -> {
//            "Comparison of incompatible enums ''{0}'' and ''{1}'' is always unsuccessful"
            return CangJieDiagnosisBundle.rawMessage(INCOMPATIBLE_ENUM_COMPARISON);
        }, RENDER_TYPE, RENDER_TYPE);
        MAP.put(INCOMPATIBLE_ENUM_COMPARISON_ERROR,
                () -> {
//"Comparison of incompatible enums ''{0}'' and ''{1}'' is always unsuccessful"
                    return CangJieDiagnosisBundle.rawMessage(INCOMPATIBLE_ENUM_COMPARISON_ERROR);
                }, RENDER_TYPE, RENDER_TYPE);

        MAP.put(NOT_ENUM_MATCH, () -> {
            // "enum pattern is not matched"
            return CangJieDiagnosisBundle.rawMessage(NOT_ENUM_MATCH);
        });
        MAP.put(ENUM_CONSTRUCTOR_MISMATCH, () -> {
            // "No enumeration constructor found with parameter ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(ENUM_CONSTRUCTOR_MISMATCH);
        }, INT);

        MAP.put(TUPLE_ARGS_TOO_FEW, () -> {
            // "The number of arguments for the tuple pattern is less than 2"
            return CangJieDiagnosisBundle.rawMessage(TUPLE_ARGS_TOO_FEW);
        });

        MAP.put(TUPLE_ARGS_MISMATCH, () -> {
            // "The number of arguments to the tuple pattern does not match, ''{0}'' is needed, but ''{1}'' are obtained"
            return CangJieDiagnosisBundle.rawMessage(TUPLE_ARGS_MISMATCH);
        }, INT, INT);

        MAP.put(ENUM_CLASS_CONSTRUCTOR_CALL, () -> {
            // "Enum types cannot be instantiated"
            return CangJieDiagnosisBundle.rawMessage(ENUM_CLASS_CONSTRUCTOR_CALL);
        });

        MAP.put(SEALED_CLASS_CONSTRUCTOR_CALL, () -> {
            // "Sealed types cannot be instantiated"
            return CangJieDiagnosisBundle.rawMessage(SEALED_CLASS_CONSTRUCTOR_CALL);
        });

        MAP.put(DEPRECATED_ACCESS_BY_SHORT_NAME, () -> {
            // "Access to this type by short name is deprecated, and soon is going to be removed. Please, add explicit qualifier or import"
            return CangJieDiagnosisBundle.rawMessage(DEPRECATED_ACCESS_BY_SHORT_NAME);
        }, NAMED);

        MAP.put(TUPLE_PATTERN_TYPE_MISMATCH, () -> {
            // "Type mismatch: inferred type is Tuple but {0} was expected"
            return CangJieDiagnosisBundle.rawMessage(TUPLE_PATTERN_TYPE_MISMATCH);
        }, RENDER_TYPE);

        MAP.put(ELSE_MISPLACED_IN_MATCH, () -> {
            // "'else _' entry must be the last one in a match-expression"
            return CangJieDiagnosisBundle.rawMessage(ELSE_MISPLACED_IN_MATCH);
        });

        MAP.put(VARIABLE_INTRODUCTION_CONFLICT, () -> {
            // "cannot introduce variables in patterns connected by '|'"
            return CangJieDiagnosisBundle.rawMessage(VARIABLE_INTRODUCTION_CONFLICT);
        });

        MAP.put(NOT_ENUM_PARAMETER_CONSTRUCTOR, () -> {
            // "No parameterless constructor"
            return CangJieDiagnosisBundle.rawMessage(NOT_ENUM_PARAMETER_CONSTRUCTOR);
        });

        MAP.put(ENUM_ENTRY_CONSTRUCTOR_REQUIER, () -> {
            // "Enumeration constructor requires parameters"
            return CangJieDiagnosisBundle.rawMessage(ENUM_ENTRY_CONSTRUCTOR_REQUIER);
        });

        MAP.put(LET_EXPRESSION_NO_TYPE_PATTERN, () -> {
            // "type pattern is not allowed in let expression"
            return CangJieDiagnosisBundle.rawMessage(LET_EXPRESSION_NO_TYPE_PATTERN);
        });

        MAP.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, () -> {
            // "Abstract property ''{0}'' in non-abstract class ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS);
        }, STRING, NAMED);

        MAP.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, () -> {
            // "Abstract function ''{0}'' in non-abstract class ''{1}''"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS);
        }, STRING, NAMED);

        MAP.put(REPEATED_MODIFIER, () -> {
            // "Repeated ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(REPEATED_MODIFIER);
        }, TO_STRING);

        MAP.put(LET_WITH_SETTER, () -> {
            // "A immutable property cannot have a setter"
            return CangJieDiagnosisBundle.rawMessage(LET_WITH_SETTER);
        });

        MAP.put(UNINITIALIZED_VARIABLE, () -> {
            // "Variable ''{0}'' must be initialized"
            return CangJieDiagnosisBundle.rawMessage(UNINITIALIZED_VARIABLE);
        }, NAMED);

        MAP.put(IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION, () -> {
            // "Instance member variable ''{0}'' cannot be modified in immutable function"
            return CangJieDiagnosisBundle.rawMessage(IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION);
        }, NAMED);

        MAP.put(MUST_BE_INITIALIZED, () -> {
            // "Variable must be initialized"
            return CangJieDiagnosisBundle.rawMessage(MUST_BE_INITIALIZED);
        });

        MAP.put(VARIABLE_INITIALIZER_IN_INTERFACE, () -> {
            // "Variable initializers are not allowed in interfaces"
            return CangJieDiagnosisBundle.rawMessage(VARIABLE_INITIALIZER_IN_INTERFACE);
        });

        MAP.put(INTERFACE_BODY_NO_VARIABLES, () -> {
            // "Unexpected variable declaration in interface body"
            return CangJieDiagnosisBundle.rawMessage(INTERFACE_BODY_NO_VARIABLES);
        });

        MAP.put(VAR_OVERRIDDEN_BY_LET, () -> {
            // "Mut-property {0} cannot be overridden by  property {1}"
            return CangJieDiagnosisBundle.rawMessage(VAR_OVERRIDDEN_BY_LET);
        }, FQ_NAMES_IN_TYPES, FQ_NAMES_IN_TYPES);

        MAP.put(LET_OVERRIDDEN_BY_VAR, () -> {
            // "property {0} cannot be overridden by Mut-property {1}"
            return CangJieDiagnosisBundle.rawMessage(LET_OVERRIDDEN_BY_VAR);
        }, FQ_NAMES_IN_TYPES, FQ_NAMES_IN_TYPES);

        MAP.put(ABSTRACT_FUNCTION_WITH_BODY, () -> {
            // "A function ''{0}'' with body cannot be abstract"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_FUNCTION_WITH_BODY);
        }, NAMED);
        MAP.put(ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE, () -> {
            // "abstract function must have return type"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE);
        });

        MAP.put(RETURN_TYPE_NOT_SPECIFIED_ERROR, () -> {
            // "For technical reasons, please display the return type of the specified method"
            return CangJieDiagnosisBundle.rawMessage(RETURN_TYPE_NOT_SPECIFIED_ERROR);
        });
        MAP.put(CONST_MEMBER_FUNCTION_REQUIRES_CONST_CONSTRUCTOR, () -> {
            // "The visibility of an ''{0}'' {1} must be {2}"
            return CangJieDiagnosisBundle.rawMessage(CONST_MEMBER_FUNCTION_REQUIRES_CONST_CONSTRUCTOR);
        });

        MAP.put(ABSTRACT_MEMBER_VISIBILITY_ERROR, () -> {
            // "The visibility of an ''{0}'' {1} must be {2}"
            return CangJieDiagnosisBundle.rawMessage(ABSTRACT_MEMBER_VISIBILITY_ERROR);
        }, MODALITY_NAME, DESCRIPTOR_KIND_NAME, VISIBLITYS_NAMES);

        MAP.put(NO_CONSTRUCTOR, () -> {
            // "This class does not have a constructor"
            return CangJieDiagnosisBundle.rawMessage(NO_CONSTRUCTOR);
        });

        MAP.put(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR, () -> {
            // "Unexpected constructor in {0} body"
            return CangJieDiagnosisBundle.rawMessage(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR);
        }, STRING);

        MAP.put(UNEXPECTED_FINALIZER_IN_BODY_ERROR, () -> {
            // "unexpected finalizer in {0} body"
            return CangJieDiagnosisBundle.rawMessage(UNEXPECTED_FINALIZER_IN_BODY_ERROR);
        }, STRING);
        MAP.put(FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR, () -> {
            // "Finalizer cannot have any parameter"
            return CangJieDiagnosisBundle.rawMessage(FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR);
        });

        MAP.put(IMPLICIT_NOTHING_RETURN_TYPE, () -> {
            // "'Nothing' return type needs to be specified explicitly"
            return CangJieDiagnosisBundle.rawMessage(IMPLICIT_NOTHING_RETURN_TYPE);
        });
        MAP.put(IRREFUTABLE_PATTERN_FOR_IN_ERROR, () -> {
            // "The pattern in for-in expression must be irrefutable"
            return CangJieDiagnosisBundle.rawMessage(IRREFUTABLE_PATTERN_FOR_IN_ERROR);
        });
        MAP.put(IRREFUTABLE_PATTERN_ERROR, () -> {
            // "The pattern in for-in expression must be irrefutable"
            return CangJieDiagnosisBundle.rawMessage(IRREFUTABLE_PATTERN_ERROR);
        });

        MAP.put(NO_GET_METHOD, () -> {
            // "No get method providing array access"
            return CangJieDiagnosisBundle.rawMessage(NO_GET_METHOD);
        });

        MAP.put(NO_SET_METHOD, () -> {
            // "No set method providing array access"
            return CangJieDiagnosisBundle.rawMessage(NO_SET_METHOD);
        });

        MAP.put(NO_GET_FOR_TUPLE_METHOD, () -> {
            // "No get method providing array access"
            return CangJieDiagnosisBundle.rawMessage(NO_GET_FOR_TUPLE_METHOD);
        });
        MAP.put(NON_INTEGER_TUPLE_INDEX, () -> {
            // "No set method providing array access"
            return CangJieDiagnosisBundle.rawMessage(NON_INTEGER_TUPLE_INDEX);
        });
        MAP.put(NO_SET_FOR_TUPLE_METHOD, () -> {
            // "No set method providing array access"
            return CangJieDiagnosisBundle.rawMessage(NO_SET_FOR_TUPLE_METHOD);
        });
        MAP.put(DEPRECATED_TYPE_PARAMETER_SYNTAX, () -> {
            // "Type parameters must be placed before the name of the function"
            return CangJieDiagnosisBundle.rawMessage(DEPRECATED_TYPE_PARAMETER_SYNTAX);
        });
        MAP.put(TUPLE_INDEX_OUT_OF_RANGE, () -> {
            // "Type parameters must be placed before the name of the function"
            return CangJieDiagnosisBundle.rawMessage(TUPLE_INDEX_OUT_OF_RANGE);
        });
        MAP.put(FLOAT_LITERAL_CONFORMS_INFINITY, () -> {
            // "Floating point number conforms to infinity"
            return CangJieDiagnosisBundle.rawMessage(FLOAT_LITERAL_CONFORMS_INFINITY);
        });
        MAP.put(
                RECEIVER_TYPE_MISMATCH, () -> {
//                                    "Constraint error in receiver type argument: inferred type is {1} but {0} was expected"
                    return CangJieDiagnosisBundle.rawMessage(RECEIVER_TYPE_MISMATCH);
                }
                ,
                RENDER_TYPE, RENDER_TYPE
        );

        MAP.put(EXTENSION_SHADOWED_BY_MEMBER, () -> {
//            "Extension is shadowed by a member: {0}"
            return CangJieDiagnosisBundle.rawMessage(EXTENSION_SHADOWED_BY_MEMBER);
        }, COMPACT_WITH_MODIFIERS);
        MAP.put(INACCESSIBLE_OUTER_CLASS_EXPRESSION, () -> {
//            "Expression is inaccessible from a nested class ''{0}''"
            return CangJieDiagnosisBundle.rawMessage(INACCESSIBLE_OUTER_CLASS_EXPRESSION);
        }, NAMED);

    }


    @NotNull
    @SuppressWarnings("unchecked")
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = getRendererForDiagnostic(diagnostic);
        if (null != renderer) {
            return renderer.render(diagnostic);
        }
        return diagnostic + " (error: could not render message)";
    }

    @Nullable
    public static DiagnosticRenderer getRendererForDiagnostic(@NotNull UnboundDiagnostic diagnostic) {
        // firstNotNullOfOrNull from stdlib can not be used here because it is InlineOnly function and can not be accessed from Java
        DiagnosticRenderer<?> renderer = AddToStdlibKt.firstNotNullResult(RENDERER_MAPS, map -> map.get(diagnostic.getFactory()));
        if (null != renderer)
            return renderer;
        else
            return diagnostic.getFactory().getDefaultRenderer();
    }


    public interface Extension {
        @NotNull
        DiagnosticFactoryToRendererMap getMap();
    }
}
