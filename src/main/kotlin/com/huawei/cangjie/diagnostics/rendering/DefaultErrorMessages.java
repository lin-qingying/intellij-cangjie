package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.diagnostics.UnboundDiagnostic;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.utils.AddToStdlibKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.huawei.cangjie.diagnostics.Errors.*;
import static com.huawei.cangjie.diagnostics.rendering.CommonRenderers.STRING;
import static com.huawei.cangjie.diagnostics.rendering.CommonRenderers.THROWABLE;
import static com.huawei.cangjie.diagnostics.rendering.Renderers.*;


public class DefaultErrorMessages {
    private static final List<DiagnosticFactoryToRendererMap> RENDERER_MAPS;
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");

    static {
        RENDERER_MAPS = List.of(MAP);

    }

    static {

        MAP.put(COMPILER_AFFECTED_SYNTAX_ERROR, "Affected by the compiler, this writing will report an error");


/**************声明检查************************************************/
//未定义
        MAP.put(UNSUPPORTED, "Unsupported [{0}]", STRING);
//重复定义
        MAP.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", CommonRenderers.commaSeparated(FQ_NAMES_IN_TYPES));
        MAP.put(PACKAGE_OR_CLASSIFIER_REDECLARATION, "Redeclaration: {0}", STRING);

        MAP.put(REDECLARATION, "Conflicting declarations: {0}", CommonRenderers.commaSeparated(COMPACT_WITH_MODIFIERS));

        MAP.put(FUNCTION_CALL_EXPECTED, "Function invocation ''{0}({1})'' expected", ELEMENT_TEXT,
                (hasValueParameters, context) -> hasValueParameters ? "..." : "");

        MAP.put(NO_VALUE_FOR_PARAMETER, "No value passed for parameter ''{0}''", NAMED);

//        MAP.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", ELEMENT_TEXT);
        MAP.put(UNRESOLVED_REFERENCE, "Reference not found: {0}", ELEMENT_TEXT);

//函数
        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "Overload resolution ambiguity: {0}", AMBIGUOUS_CALLS);

// 二进制重载
        MAP.put(INVALID_BINARY_OPERATOR, "Invalid binary operator ''{0}'' on type ''{1}'' and ''{2}'', you may want to implement 'operator func '{3}'(right: '{4}')' for type ''{5}''", object -> {
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
        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "The {0} literal does not conform to the expected type {1}", STRING, RENDER_TYPE);

        MAP.put(WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''", TO_STRING, TO_STRING);
        MAP.put(INT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(FLOAT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(INCORRECT_CHARACTER_LITERAL, "Incorrect character literal");
        MAP.put(EMPTY_CHARACTER_LITERAL, "Empty character literal");
        MAP.put(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL, "Too many characters in a character literal ''{0}''", TO_STRING);
        MAP.put(INT_LITERAL_OUT_OF_RANGE_BY_TYPE, "the number ''{0}'' exceeds the value range of type ''{1}''", TO_STRING, RENDER_TYPE);


//        包导入

        MAP.put(PACKAGE_CANNOT_BE_IMPORTED, "Packages cannot be imported");
        MAP.put(MODULE_PACKAGE_CANNOT_BE_IMPORTED, "Module name cannot be imported");
        MAP.put(SELF_IMPORT_NOT_ALLOWED, "Package ''{0}'' should not import itself", FQNAME);
        MAP.put(IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED, "Imported package name ''{0}'' cannot be modified by ''{1}''", FQNAME, VISIBILITY);
        MAP.put(CYCLIC_IMPORT, "packages ''{0}'' ''{1}'' are in circular dependencies.", FQNAME, FQNAME);
        MAP.put(PACKAGE_ACCESS_VIOLATION, "The access level of child package ''{0}'' cannot be higher than that of parent package ''{1}''", FQNAME, FQNAME);


//        访问控制
        MAP.put(INVISIBLE_REFERENCE_REEXPORT, "Cannot access ''{0}'': it is {1} in {2}", NAMED, VISIBILITY, FQNAME);

        MAP.put(INVISIBLE_REFERENCE, "Cannot access ''{0}'': it is {1} in {2}", NAMED, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);
        MAP.put(INCONSISTENT_PACKAGE_MODIFIERS, "Inconsistent modifiers for package ''{0}''", FQNAME);
        MAP.put(INCONSISTENT_PACKAGE_MACOR, "Inconsistent macro package declarations");
        MAP.put(OVERRIDING_FINAL_MEMBER, "''{0}'' cannot be referenced from a static context", NAMED, NAMED);

        MAP.put(STATIC_CONTEXT_REFERENCE_ERROR, "Non-static {0} ''{1}'' cannot be referenced from a static context", DESCRIPTOR_KIND_NAME, NAMED_ADN_PARAMETER);
        MAP.put(INSTANCE_ACCESS_STATIC_MEMBER_ERROR, "Static {0} ''{1}'' accessed via instance reference", DESCRIPTOR_KIND_NAME, NAMED_ADN_PARAMETER);

//        类型检查

        MAP.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''", VISIBILITY, NAMED, NAMED);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract member {1}", RENDER_TYPE_STATMENT,
                FQ_NAMES_IN_TYPES);
        MAP.put(VIRTUAL_MEMBER_HIDDEN, "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier", NAMED, NAMED, NAMED);
        MAP.put(NOTHING_TO_OVERRIDE, "''{0}'' overrides nothing", NAMED);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "Return type of ''{0}'' is not a subtype of the return type of the overridden member ''{1}''",
                NAMED, FQ_NAMES_IN_TYPES_ANNOTATIONS_WHITELIST);
        String wrongNumberOfTypeArguments = "{0,choice,0#No type arguments|1#One type argument|1<{0,number,integer} type arguments} expected";

        MAP.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, wrongNumberOfTypeArguments + " for {1}", null, COMPACT_WITHOUT_SUPERTYPES);
        MAP.put(MEMBER_PROJECTED, "type ''{1}'' prohibits the use of ''{0}''", FQ_NAMES_IN_TYPES, RENDER_TYPE);

        MAP.put(UPPER_BOUND_VIOLATED, "Type argument is not within its bounds: should be subtype of ''{0}''", RENDER_TYPE, RENDER_TYPE);
        MAP.put(SUPERTYPE_APPEARS_TWICE, "A supertype appears twice");
        MAP.put(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE, "Only classes and interfaces may serve as supertypes");
        MAP.put(CLASS_IN_SUPERTYPE_FOR_ENUM, "Enum  cannot inherit from classes");
        MAP.put(INTERFACE_WITH_SUPERCLASS, "An interface can only inherit from a interface");
        MAP.put(EXTEND_WITH_SUPERCLASS, "An extend can only inherit from a interface");
        MAP.put(CYCLIC_INHERITANCE_HIERARCHY, "There's a cycle in the inheritance hierarchy for this type");
        MAP.put(OBJECT_IN_SUPERTYPE, "Cannot inherit from a struct");
        MAP.put(ENUM_IN_SUPERTYPE, "Cannot inherit from a enum");
        MAP.put(FINAL_SUPERTYPE, "super class ''{0}'' is not inheritable", RENDER_TYPE);
        MAP.put(MANY_CLASSES_IN_SUPERTYPE_LIST, "Only one class may appear in a supertype list");

        MAP.put(EXTEND_CANNOT_INTERFACE, "Extended interfaces are cannot allowed");
        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract base class member {1}",
                RENDER_TYPE_STATMENT, FQ_NAMES_IN_TYPES);

        MAP.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, "The corresponding parameter in the supertype ''{0}'' is named ''{1}''. " +
                "This may cause problems when calling this function with named arguments.", NAMED, NAMED);
//类型系统
        MAP.put(INVALID_THIS_TYPE, "'This' type is not allowed, 'This' type can only be used as the return type of an instance member function in class");
        MAP.put(TYPE_MISMATCH_MULTIPLE_SUPERTYPES, "Type inference failed: multiple smallest common supertypes found {0}", RENDER_COLLECTION_OF_TYPES);
        MAP.put(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
                "Type mismatch: inferred type is {1} but {0} was expected. Projected type {2} restricts use of {3}",
                object -> {
                    RenderingContext context = RenderingContext.of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(), object.getCallableDescriptor());
                    return new String[]{
                            RENDER_TYPE.render(object.getExpectedType(), context),
                            RENDER_TYPE.render(object.getExpressionType(), context),
                            RENDER_TYPE.render(object.getReceiverType(), context),
                            FQ_NAMES_IN_TYPES.render(object.getCallableDescriptor(), context)
                    };
                });

        MAP.put(TYPE_MISMATCH, "Type mismatch: inferred type is {1} but {0} was expected", RENDER_TYPE, RENDER_TYPE);
// 调用

        MAP.put(TOO_MANY_ARGUMENTS, "Too many arguments for {0}", FQ_NAMES_IN_TYPES);

//        表达式
        MAP.put(NEW_INFERENCE_UNKNOWN_ERROR, "Unknown error in new inference with applicability ''{0}'' and target ''{1}''", TO_STRING, STRING);
        MAP.put(AMBIGUOUS_ANONYMOUS_TYPE_INFERRED, "Right-hand side has anonymous type. Please specify type explicitly", TO_STRING);
        MAP.put(NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "Not enough information to infer type variable {0}", STRING);
        MAP.put(ARRAY_LITERAL_TYPE_INFERENCE_FAILED, "array literal type cannot be inferred");
        MAP.put(UPPER_BOUND_VIOLATION_IN_CONSTRAINT, "Upper bound violation for generic parameter `{0}` of `{1}`: {3} is not a subtype of {2}", TO_STRING, TO_STRING, RENDER_TYPE, RENDER_TYPE);

        MAP.put(RETURN_NOT_ALLOWED, "'return' is not allowed here");
        MAP.put(NONE_APPLICABLE, "None of the following functions can be called with the arguments supplied: {0}", AMBIGUOUS_CALLS);
        MAP.put(EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE, "Expected member name or constructor call after ''{0}'' type name", NAMED);
        MAP.put(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, "Type parameter ''{0}'' is not an expression", NAMED);
        MAP.put(EXPRESSION_EXPECTED_PACKAGE_FOUND, "Expression expected, but a package name found");

        MAP.put(FUNCTION_EXPECTED, "Expression ''{0}''{1} cannot be invoked as a function. " +
                        "No matching function for operator '()' function call",
                ELEMENT_TEXT, (type, context) -> {
                    if (CangJieTypeKt.isError(type)) return "";
                    return " of type '" + RENDER_TYPE.render(type, context) + "'";
                });


        MAP.put(INVISIBLE_MEMBER, "Cannot access ''{0}'': it is {1} in {2}", NAMED, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);

//        修饰符
        MAP.put(REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present", TO_STRING, TO_STRING);
        MAP.put(SEALED_ABSTRACT, "'sealed' can only modify abstract class");
        MAP.put(INCOMPATIBLE_MODIFIERS, "Modifier ''{0}'' is incompatible with ''{1}''", TO_STRING, TO_STRING);
        MAP.put(WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''", TO_STRING, TO_STRING);
        MAP.put(NO_MULTILINE_NEWLINE, "Multiline strings need to start with newline characters");


        MAP.put(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER, "This variable must either have a type annotation or be initialized");


        MAP.put(EXCEPTION_FROM_ANALYZER, "Internal Error occurred while analyzing this expression:\n{0}", THROWABLE);
        MAP.put(REDUNDANT_OPTIONAL, "Redundant '?'");
//        MAP.put(REDUNDANT_OPTIONAL, AstMsgData.create(
//                new AstMsgData.AstMsgModule(AstMsgType.NORMAL, "Redundant '?'"),
//
//                new AstMsgData.AstMsgModule(AstMsgType.FUNNY, "套娃套的好，编码没烦恼"),
//                new AstMsgData.AstMsgModule(AstMsgType.FUNNY, "别套了，再套地球都让你套进去了")
//        ));


        MAP.put(NESTING_DOLL_OPTINOTYPE, () -> {
            Random random = new Random();

            List<String> messages = new ArrayList<>();
            messages.add("套娃套的好，编码没烦恼");
            messages.add("别套了，再套地球都让你套进去了");

            // 计算或构造你的值
            return messages.get(random.nextInt(messages.size()));
        });


//        声明
        MAP.put(FUNCTION_DECLARATION_WITH_NO_NAME, "Function declaration must have a name");


        MAP.put(ITERATOR_MISSING, "For-loop range must have an 'iterator()' method");
        MAP.put(NAME_SHADOWING, "Name shadowed: {0}", STRING);
        MAP.put(ACCESSOR_PARAMETER_NAME_SHADOWING, "Accessor parameter name 'field' is shadowed by backing field variable");


        MAP.put(ASSIGNMENT_IN_EXPRESSION_CONTEXT, "Assignments are not expressions, and only expressions are allowed in this context");
        MAP.put(VARIABLE_EXPECTED, "Variable expected");
        MAP.put(LET_REASSIGNMENT, "Cannot assign to immutable value {0}", NAMED);
        MAP.put(USELESS_ELVIS, "Elvis operator (??) always returns the left operand of non-option type {0}", RENDER_TYPE);
        MAP.put(USELESS_ELVIS_RIGHT_IS_NULL, "Right operand of elvis operator (??) is useless if it is Option");
        MAP.put(EXPRESSION_EXPECTED, "{0} is not an expression, and only expressions are allowed here", (expression, context) -> {
            String expressionType = expression.toString();
            return expressionType.charAt(0) +
                    expressionType.substring(1).toLowerCase();
        });
        MAP.put(UNREACHABLE_CODE, "Unreachable code", CommonRenderers.EMPTY, CommonRenderers.EMPTY);
        MAP.put(NAMED_PARAMETER_NOT_FOUND, "Cannot find a parameter with this name: {0}", ELEMENT_TEXT);
        MAP.put(NAMED_PARAMETER_PREFIX_MISSING, "missing argument prefix {0} for named parameter", NAMES_TO_STRING);
        MAP.put(NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER, "unnamed parameters must come before named parameters");
        MAP.put(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT, "positional argument cannot appear after named argument");
        MAP.put(ARGUMENT_PASSED_TWICE, "An argument is already passed for this parameter");

        MAP.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, "Mixing named and positioned arguments is not allowed");
        MAP.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, "A 'return' expression required in a function with a block body ('{...}')");
        MAP.put(EXPECTED_TYPE_MISMATCH, "Expected a value of type {0}", RENDER_TYPE);
        MAP.put(DECLARATION_IN_ILLEGAL_CONTEXT, "Declarations are not allowed in this position");
        MAP.put(MAIN_FUNCTION_RETURN_TYPE, "return type of 'main' is not 'Integer' or 'Unit'");
        MAP.put(MAIN_FUNCTION_PARAMETER_COUNT, "'main' method has too many parameters");
        MAP.put(MAIN_FUNCTION_PARAMETER_TYPE, "'main' cannot be defined with parameter whose type is not 'Array<String>'");
        MAP.put(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, "Nested {0} accessed via instance reference", RENDER_CLASS_OR_OBJECT_NAME);

        MAP.put(MAIN_FUNCTION_NUMBER_ERROR, "Multiple 'main' methods are defined in the module");
        MAP.put(USELESS_IS_CHECK, "Check for instance is always ''{0}''", TO_STRING);
        MAP.put(INCOMPATIBLE_TYPES, "Incompatible types: {0} and {1}", RENDER_TYPE, RENDER_TYPE);
        MAP.put(SUPERTYPE_NOT_INITIALIZED, "There is no parameterless constructor available in ''{0}''", RENDER_TYPE);
        MAP.put(EXPLICIT_DELEGATION_CALL_REQUIRED,
                "Explicit 'this' or 'super' call is required. There is no constructor in superclass that can be called without arguments");
        MAP.put(CYCLIC_CONSTRUCTOR_DELEGATION_CALL, "There's a cycle in the init calls chain");
        MAP.put(NO_THIS, "'this' is not defined in this context");

        MAP.put(CONSTRUCTOR_NAME_INCONSISTENCY, "Primary constructor name is inconsistent with type name");

        MAP.put(MULTIPLE_PRIMARY_CONSTRUCTORS, "Class ''{0}'' cannot have more than one primary constructor", CLASS_NAME);

        MAP.put(INVALID_CALLING_THIS_IN_PRIMARY_CONSTRUCTOR, "Invalid calling 'this' in primary constructor");
        MAP.put(ENUM_ENTRY_AS_TYPE, "Use of enum entry names as types is not allowed, use enum type instead");
        MAP.put(UNSAFE_EXPRESSION_ERROR, "'unsafe' is not a lambda expression");
        MAP.put(CANNOT_INFER_PARAMETER_TYPE, "Cannot infer a type for this parameter. Please specify it explicitly.");
        MAP.put(NO_ELSE_IN_MATCH, "''match'' expression must be exhaustive, add necessary {0}", RENDER_MATCH_MISSING_CASES);
        MAP.put(CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON, "Cannot import-on-demand from class ''{0}''", NAMED);
        MAP.put(NOT_ENUM_ENTRY_VALUE, "Not an enumerated value");
        MAP.put(NOT_ENUM_MATCH, "enum pattern is not matched");
        MAP.put(NOT_ENUM_MATCH, "enum pattern is not matched");
        MAP.put(ENUM_CONSTRUCTOR_MISMATCH, "No enumeration constructor found with parameter ''{0}''", INT);
        MAP.put(TUPLE_ARGS_TOO_FEW, "The number of arguments for the tuple pattern is less than 2");
        MAP.put(TUPLE_ARGS_MISMATCH, "The number of arguments to the tuple pattern does not match, ''{0}'' is needed, but ''{1}'' are obtained", INT, INT);

        MAP.put(TUPLE_PATTERN_TYPE_MISMATCH, "Type mismatch: inferred type is Tuple but {0} was expected", RENDER_TYPE);
        MAP.put(ELSE_MISPLACED_IN_MATCH, "'else _' entry must be the last one in a match-expression");
        MAP.put(VARIABLE_INTRODUCTION_CONFLICT, "cannot introduce variables in patterns connected by '|'");
        MAP.put(NOT_ENUM_PARAMETER_CONSTRUCTOR, "No parameterless constructor");
        MAP.put(LET_EXPRESSION_NO_TYPE_PATTERN, "type pattern is not allowed in let expression");
        MAP.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, "Abstract property ''{0}'' in non-abstract class ''{1}''", STRING, NAMED);
        MAP.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, "Abstract function ''{0}'' in non-abstract class ''{1}''", STRING, NAMED);
        MAP.put(REPEATED_MODIFIER, "Repeated ''{0}''", TO_STRING);
        MAP.put(LET_WITH_SETTER, "A immutable  property cannot have a setter");
        MAP.put(UNINITIALIZED_VARIABLE, "Variable ''{0}'' must be initialized", NAMED);
        MAP.put(IMMUTABLE_FUNCTION_INSTANCE_MEMBER_MODIFICATION, "Instance member variable ''{0}'' cannot be modified in immutable function", NAMED);
        MAP.put(MUST_BE_INITIALIZED, "Variable must be initialized");
        MAP.put(VARIABLE_INITIALIZER_IN_INTERFACE, "Variable initializers are not allowed in interfaces");
        MAP.put(INTERFACE_BODY_NO_VARIABLES, "Unexpected variable declaration in interface body");
        MAP.put(VAR_OVERRIDDEN_BY_LET, "Mut-property {0} cannot be overridden by  property {1}", FQ_NAMES_IN_TYPES,
                FQ_NAMES_IN_TYPES);
        MAP.put(LET_OVERRIDDEN_BY_VAR, "property {0} cannot be overridden by  Mut-property {1}", FQ_NAMES_IN_TYPES,
                FQ_NAMES_IN_TYPES);
        MAP.put(ABSTRACT_FUNCTION_WITH_BODY, "A function ''{0}'' with body cannot be abstract", NAMED);


        MAP.put(RETURN_TYPE_NOT_SPECIFIED_ERROR, "For technical reasons, please display the return type of the specified method" );
        MAP.put(ABSTRACT_MEMBER_VISIBILITY_ERROR, "The visibility of an ''{0}'' {1} must be {2}",MODALITY_NAME, DESCRIPTOR_KIND_NAME,VISIBLITYS_NAMES);
        MAP.put(NO_CONSTRUCTOR, "This class does not have a constructor");
        MAP.put(UNEXPECTED_CONSTRUCTOR_IN_BODY_ERROR, "Unexpected constructor in {0} body",STRING);
        MAP.put(UNEXPECTED_FINALIZER_IN_BODY_ERROR, "unexpected finalizer in {0} body",STRING);


        MAP.put(FINALIZER_CANNOT_HAVE_PARAMETERS_ERROR, "Finalizer cannot have any parameter");
        MAP.put(IMPLICIT_NOTHING_RETURN_TYPE, "'Nothing' return type needs to be specified explicitly");
        MAP.put(IRREFUTABLE_PATTERN_ERROR, "The pattern in for-in expression must be irrefutable");


        MAP.put(NO_GET_METHOD, "No get method providing array access");
        MAP.put(NO_SET_METHOD, "No set method providing array access");
        MAP.put(DEPRECATED_TYPE_PARAMETER_SYNTAX, "Type parameters must be placed before the name of the function");

    }


    @NotNull
    @SuppressWarnings("unchecked")
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = getRendererForDiagnostic(diagnostic);
        if (renderer != null) {
            return renderer.render(diagnostic);
        }
        return diagnostic + " (error: could not render message)";
    }

    @Nullable
    public static DiagnosticRenderer getRendererForDiagnostic(@NotNull UnboundDiagnostic diagnostic) {
        // firstNotNullOfOrNull from stdlib can not be used here because it is InlineOnly function and can not be accessed from Java
        DiagnosticRenderer<?> renderer = AddToStdlibKt.firstNotNullResult(RENDERER_MAPS, map -> map.get(diagnostic.getFactory()));
        if (renderer != null)
            return renderer;
        else
            return diagnostic.getFactory().getDefaultRenderer();
    }


    public interface Extension {
        @NotNull
        DiagnosticFactoryToRendererMap getMap();
    }
}
