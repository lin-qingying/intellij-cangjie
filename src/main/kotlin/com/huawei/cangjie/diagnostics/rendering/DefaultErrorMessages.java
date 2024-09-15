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
import static com.huawei.cangjie.diagnostics.rendering.CommonRenderers.*;
import static com.huawei.cangjie.diagnostics.rendering.Renderers.*;


public class DefaultErrorMessages {
    private static final List<DiagnosticFactoryToRendererMap> RENDERER_MAPS;
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");

    static {
        RENDERER_MAPS = List.of(MAP);

    }

    static {

        MAP.put(COMPILER_AFFECTED_SYNTAX_ERROR, "Affected by the compiler, this writing will report an error");


/***************声明检查************************************************/
//未定义
        MAP.put(UNSUPPORTED, "Unsupported [{0}]", STRING);
//重复定义
        MAP.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", CommonRenderers.commaSeparated(FQ_NAMES_IN_TYPES));
        MAP.put(PACKAGE_OR_CLASSIFIER_REDECLARATION, "Redeclaration: {0}", STRING);

        MAP.put(REDECLARATION, "Conflicting declarations: {0}", CommonRenderers.commaSeparated(COMPACT_WITH_MODIFIERS));

        MAP.put(FUNCTION_CALL_EXPECTED, "Function invocation ''{0}({1})'' expected", ELEMENT_TEXT,
                (hasValueParameters, context) -> hasValueParameters ? "..." : "");
//        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "The {0} literal does not conform to the expected type {1}", CommonRenderers.STRING, RENDER_TYPE);
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

        MAP.put(OVERRIDING_FINAL_MEMBER, "''{0}'' in ''{1}'' is final and cannot be overridden", NAMED, NAMED);

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
        MAP.put(STRUCT_IN_SUPERTYPE, "Cannot inherit from a struct");
        MAP.put(ENUM_IN_SUPERTYPE, "Cannot inherit from a enum");
        MAP.put(FINAL_SUPERTYPE, "super class ''{0}'' is not inheritable", RENDER_TYPE);
        MAP.put(MANY_CLASSES_IN_SUPERTYPE_LIST, "Only one class may appear in a supertype list");

        MAP.put(EXTEND_CANNOT_INTERFACE, "Extended interfaces are cannot allowed");
        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract base class member {1}",
                RENDER_TYPE_STATMENT, FQ_NAMES_IN_TYPES);

        MAP.put(PARAMETER_NAME_CHANGED_ON_OVERRIDE, "The corresponding parameter in the supertype ''{0}'' is named ''{1}''. " +
                "This may cause problems when calling this function with named arguments.", NAMED, NAMED);
//类型系统
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
        MAP.put(LET_REASSIGNMENT, "Val cannot be reassigned", NAMED);
        MAP.put(USELESS_ELVIS, "Elvis operator (??) always returns the left operand of non-option type {0}", RENDER_TYPE);
        MAP.put(USELESS_ELVIS_RIGHT_IS_NULL, "Right operand of elvis operator (??) is useless if it is Option");
        MAP.put(EXPRESSION_EXPECTED, "{0} is not an expression, and only expressions are allowed here", (expression, context) -> {
            String expressionType = expression.toString();
            return expressionType.charAt(0) +
                    expressionType.substring(1).toLowerCase();
        });
        MAP.put(UNREACHABLE_CODE, "Unreachable code",CommonRenderers. EMPTY,CommonRenderers. EMPTY);
        MAP.put(NAMED_PARAMETER_NOT_FOUND, "Cannot find a parameter with this name: {0}", ELEMENT_TEXT);
        MAP.put(NAMED_PARAMETER_PREFIX_MISSING, "missing argument prefix {0} for named parameter", NAMES_TO_STRING);
        MAP.put(NON_NAMED_PARAMETER_AFTER_NAMED_PARAMETER, "unnamed parameters must come before named parameters" );
        MAP.put(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT, "positional argument cannot appear after named argument");
        MAP.put(ARGUMENT_PASSED_TWICE, "An argument is already passed for this parameter");

        MAP.put(MIXING_NAMED_AND_POSITIONED_ARGUMENTS, "Mixing named and positioned arguments is not allowed");
        MAP.put(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY, "A 'return' expression required in a function with a block body ('{...}')");
        MAP.put(EXPECTED_TYPE_MISMATCH, "Expected a value of type {0}", RENDER_TYPE);
        MAP.put(DECLARATION_IN_ILLEGAL_CONTEXT, "Declarations are not allowed in this position");
        MAP.put(MAIN_FUNCTION_RETURN_TYPE, "return type of 'main' is not 'Integer' or 'Unit'");
        MAP.put(MAIN_FUNCTION_PARAMETER_COUNT, "'main' method has too many parameters");
        MAP.put(MAIN_FUNCTION_PARAMETER_TYPE, "'main' cannot be defined with parameter whose type is not 'Array<String>'");

        MAP.put(MAIN_FUNCTION_NUMBER_ERROR, "Multiple 'main' methods are defined in the module");

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
        @SuppressWarnings("deprecation")
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
