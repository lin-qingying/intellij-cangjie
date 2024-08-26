package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.diagnostics.UnboundDiagnostic;
import com.huawei.cangjie.utils.AddToStdlibKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.huawei.cangjie.descriptors.Errors.*;
import static com.huawei.cangjie.diagnostics.rendering.CommonRenderers.THROWABLE;
import static com.huawei.cangjie.diagnostics.rendering.Renderers.*;


public class DefaultErrorMessages {
    private static final List<DiagnosticFactoryToRendererMap> RENDERER_MAPS;
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");

    static {
        RENDERER_MAPS = List.of(MAP);

    }

    static {
/***************声明检查************************************************/
//未定义
        MAP.put(UNSUPPORTED, "Unsupported [{0}]", STRING);
//重复定义
        MAP.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", CommonRenderers.commaSeparated(FQ_NAMES_IN_TYPES));


        MAP.put(FUNCTION_CALL_EXPECTED, "Function invocation ''{0}({1})'' expected", ELEMENT_TEXT,
                (hasValueParameters, context) -> hasValueParameters ? "..." : "");
//        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "The {0} literal does not conform to the expected type {1}", CommonRenderers.STRING, RENDER_TYPE);
        MAP.put(NO_VALUE_FOR_PARAMETER, "No value passed for parameter ''{0}''", NAME);

//        MAP.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", ELEMENT_TEXT);
        MAP.put(UNRESOLVED_REFERENCE, "Reference not found: {0}", ELEMENT_TEXT);


//        常量检查
        MAP.put(CONSTANT_EXPECTED_TYPE_MISMATCH, "The {0} literal does not conform to the expected type {1}", STRING, RENDER_TYPE);

        MAP.put(WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''", TO_STRING, TO_STRING);
        MAP.put(INT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(FLOAT_LITERAL_OUT_OF_RANGE, "The value is out of range");
        MAP.put(INCORRECT_CHARACTER_LITERAL, "Incorrect character literal");
        MAP.put(EMPTY_CHARACTER_LITERAL, "Empty character literal");
        MAP.put(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL, "Too many characters in a character literal ''{0}''", TO_STRING);
        MAP.put(INT_LITERAL_OUT_OF_RANGE_BY_TYPE, "the number ''{0}'' exceeds the value range of type ''{1}''",TO_STRING,RENDER_TYPE);


//        包导入

        MAP.put(PACKAGE_CANNOT_BE_IMPORTED, "Packages cannot be imported");
        MAP.put(MODULE_PACKAGE_CANNOT_BE_IMPORTED, "Module name cannot be imported");
        MAP.put(SELF_IMPORT_NOT_ALLOWED, "Package ''{0}'' should not import itself", FQNAME);
        MAP.put(IMPORTED_PACKAGE_MODIFICATION_NOT_ALLOWED, "Imported package name ''{0}'' cannot be modified by ''{1}''", FQNAME, VISIBILITY);
        MAP.put(CYCLIC_IMPORT, "packages ''{0}'' ''{1}'' are in circular dependencies.", FQNAME, FQNAME);
        MAP.put(PACKAGE_ACCESS_VIOLATION, "The access level of child package ''{0}'' cannot be higher than that of parent package ''{1}''", FQNAME, FQNAME);


//        访问控制
        MAP.put(INVISIBLE_REFERENCE_REEXPORT, "Cannot access ''{0}'': it is {1} in {2}", NAME, VISIBILITY, FQNAME);

        MAP.put(INVISIBLE_REFERENCE, "Cannot access ''{0}'': it is {1} in {2}", NAME, VISIBILITY, NAME_OF_CONTAINING_DECLARATION_OR_FILE);
        MAP.put(INCONSISTENT_PACKAGE_MODIFIERS, "Inconsistent modifiers for package ''{0}''", FQNAME);
        MAP.put(INCONSISTENT_PACKAGE_MACOR, "Inconsistent macro package declarations");


//        类型检查

        MAP.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, "Cannot weaken access privilege ''{0}'' for ''{1}'' in ''{2}''", VISIBILITY, NAME, NAME);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "{0} is not abstract and does not implement abstract member {1}", RENDER_CLASS ,
                FQ_NAMES_IN_TYPES);
        MAP.put(VIRTUAL_MEMBER_HIDDEN, "''{0}'' hides member of supertype ''{2}'' and needs ''override'' modifier", NAME, NAME, NAME);
        MAP.put(NOTHING_TO_OVERRIDE, "''{0}'' overrides nothing", NAME);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "Return type of ''{0}'' is not a subtype of the return type of the overridden member ''{1}''",
                NAME, FQ_NAMES_IN_TYPES_ANNOTATIONS_WHITELIST);
        String wrongNumberOfTypeArguments = "{0,choice,0#No type arguments|1#One type argument|1<{0,number,integer} type arguments} expected";

        MAP.put(WRONG_NUMBER_OF_TYPE_ARGUMENTS, wrongNumberOfTypeArguments + " for {1}", null, COMPACT_WITHOUT_SUPERTYPES);


        MAP.put(UPPER_BOUND_VIOLATED, "Type argument is not within its bounds: should be subtype of ''{0}''", RENDER_TYPE, RENDER_TYPE);

//        表达式
        MAP.put(EXCEPTION_FROM_ANALYZER, "Internal Error occurred while analyzing this expression:\n{0}", THROWABLE);
        MAP.put(NEW_INFERENCE_UNKNOWN_ERROR, "Unknown error in new inference with applicability ''{0}'' and target ''{1}''", TO_STRING, STRING);

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
