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

package cn.cangnova.cangjie.resolve.constants;

import com.google.common.collect.Sets;
import cn.cangnova.cangjie.CjNodeTypes;
import cn.cangnova.cangjie.builtins.CangJieBuiltIns;
import cn.cangnova.cangjie.descriptors.BindingTrace;
import cn.cangnova.cangjie.diagnostics.Diagnostic;
import cn.cangnova.cangjie.diagnostics.DiagnosticFactory;
import cn.cangnova.cangjie.descriptors.ModuleDescriptor;
import cn.cangnova.cangjie.psi.CjConstantExpression;
import cn.cangnova.cangjie.psi.CjElement;
import cn.cangnova.cangjie.resolve.calls.context.ResolutionContext;
import cn.cangnova.cangjie.types.CangJieType;
import cn.cangnova.cangjie.types.CangJieTypeKt;
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker;
import cn.cangnova.cangjie.types.util.TypeUtils;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static cn.cangnova.cangjie.diagnostics.Errors.*;


/**
 * 常量检查
 */
public class CompileTimeConstantChecker {

    private static final Set<DiagnosticFactory<?>> errorsThatDependOnExpectedType =
            Sets.newHashSet(CONSTANT_EXPECTED_TYPE_MISMATCH);

    private final ResolutionContext<?> context;
    private final ModuleDescriptor module;
    private final CangJieBuiltIns builtIns;
    private final boolean checkOnlyErrorsThatDependOnExpectedType;
    private final BindingTrace trace;

    public CompileTimeConstantChecker(
            @NotNull ResolutionContext<?> context,
            @NotNull ModuleDescriptor module,
            boolean checkOnlyErrorsThatDependOnExpectedType
    ) {
        this.context = context;
        this.module = module;
        this.builtIns = module.getBuiltIns();
        this.checkOnlyErrorsThatDependOnExpectedType = checkOnlyErrorsThatDependOnExpectedType;
        this.trace = context.trace;
    }

    //    @NotNull
//    private static CharacterWithDiagnostic parseCharacter(@NotNull CjConstantExpression expression) {
//        String text = expression.getText();
//        // Strip the quotes
//        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
//            return createErrorCharacter(INCORRECT_CHARACTER_LITERAL.on(expression));
//        }
//        text = text.substring(1, text.length() - 1); // now there're no quotes
//
//        if (text.length() == 0) {
//            return createErrorCharacter(EMPTY_CHARACTER_LITERAL.on(expression));
//        }
//
//        if (text.charAt(0) != '\\') {
//            // No escape
//            if (text.length() == 1) {
//                return new CharacterWithDiagnostic(text.charAt(0));
//            }
//            return createErrorCharacter(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, expression));
//        }
//        return escapedStringToCharacter(text, expression);
//    }
    @Nullable
    public static Character parseRune(@NotNull CjConstantExpression expression) {
        return parseCharacter(expression).getValue();
//        throw new RuntimeException("not implemented");
    }

    private static boolean noExpectedTypeOrError(CangJieType expectedType) {
        return TypeUtils.noExpectedType(expectedType) || CangJieTypeKt.isError(expectedType);
    }

    @NotNull
    private static CharacterWithDiagnostic createErrorCharacter(@NotNull Diagnostic diagnostic) {
        return new CharacterWithDiagnostic(diagnostic);
    }

    @NotNull
    private static CharacterWithDiagnostic parseCharacter(@NotNull CjConstantExpression expression) {
        String text = expression.getText();

        // Check for r' or r" at the start
        if (text.length() < 3 || !(text.startsWith("r'") && text.endsWith("'") || text.startsWith("r\"") && text.endsWith("\""))) {
            return createErrorCharacter(INCORRECT_CHARACTER_LITERAL.on(expression));
        }

        // Strip the prefix and quotes
        text = text.substring(2, text.length() - 1); // now there're no prefix and quotes

        if (text.isEmpty()) {
            return createErrorCharacter(EMPTY_CHARACTER_LITERAL.on(expression));
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return new CharacterWithDiagnostic(text.charAt(0));
            }
            return createErrorCharacter(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, text));
        }

        return escapedStringToCharacter(text, expression);
    }

    @NotNull
    private static CharacterWithDiagnostic illegalEscape(@NotNull CjElement expression) {
        return createErrorCharacter(ILLEGAL_ESCAPE.on(expression, expression));
    }

    @Nullable
    private static Character translateEscape(char c) {
        return switch (c) {
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case '\'' -> '\'';
            case '\"' -> '\"';
            case '\\' -> '\\';
            case '$' -> '$';
            default -> null;
        };
    }

    @NotNull
    public static CharacterWithDiagnostic escapedStringToCharacter(@NotNull String text, @NotNull CjElement expression) {
        assert !text.isEmpty() && text.charAt(0) == '\\' : "Only escaped sequences must be passed to this routine: " + text;

        // Escape
        String escape = text.substring(1); // strip the slash
        switch (escape.length()) {
            case 0:
                // bare slash
                return illegalEscape(expression);
            case 1:
                // one-char escape
                Character escaped = translateEscape(escape.charAt(0));
                if (escaped == null) {
                    return illegalEscape(expression);
                }
                return new CharacterWithDiagnostic(escaped);
            case 5:
                // unicode escape
                if (escape.charAt(0) == 'u') {
                    try {
                        Integer intValue = Integer.valueOf(escape.substring(1), 16);
                        return new CharacterWithDiagnostic((char) intValue.intValue());
                    } catch (NumberFormatException e) {
                        // Will be reported below
                    }
                }
                break;
        }
        return illegalEscape(expression);
    }

    private boolean reportError(@NotNull Diagnostic diagnostic) {
        if (!checkOnlyErrorsThatDependOnExpectedType || errorsThatDependOnExpectedType.contains(diagnostic.getFactory())) {
            trace.report(diagnostic);
            return true;
        }
        return false;
    }

    private boolean checkIntegerValue(
            @Nullable ConstantValue<?> value,
            @NotNull CangJieType expectedType,
            @NotNull CjConstantExpression expression
    ) {

        if (value == null) {
            return reportError(INT_LITERAL_OUT_OF_RANGE.on(expression));
        }

//        if (expression.getText().endsWith("l")) {
//            return reportError(WRONG_LONG_SUFFIX.on(expression));
//        }


//UInt64通过value是否为空来判断是否越界

        if (!CangJieBuiltIns.isUInt64(expectedType) && CangJieBuiltIns.isNumber(expectedType)) {
            long maxValue = PrimitiveTypeUtilKt.maxValue(expectedType);

            try {
                if (Long.parseLong(value.getValue().toString()) > maxValue) {
                    reportError(INT_LITERAL_OUT_OF_RANGE_BY_TYPE.on(expression, Long.parseLong(value.getValue().toString()), expectedType));
                }

            } catch (Exception ignored) {

            }
        }
        if (!noExpectedTypeOrError(expectedType)) {
            CangJieType valueType = value.getType(module);
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
                return reportConstantExpectedTypeMismatch(expression, "integer", expectedType, null);
            }
        }
        return false;
    }

    // return true if there is an error
    public boolean checkConstantExpressionType(
            @Nullable ConstantValue<?> compileTimeConstant,
            @NotNull CjConstantExpression expression,
            @NotNull CangJieType expectedType
    ) {
        IElementType elementType = expression.getNode().getElementType();

        if (elementType == CjNodeTypes.INTEGER_CONSTANT) {
            return checkIntegerValue(compileTimeConstant, expectedType, expression);
        } else if (elementType == CjNodeTypes.FLOAT_CONSTANT) {
            return checkFloatValue(compileTimeConstant, expectedType, expression);
        } else if (elementType == CjNodeTypes.BOOLEAN_CONSTANT) {
            return checkBooleanValue(expectedType, expression);
        } else if (elementType == CjNodeTypes.RUNE_CONSTANT) {
            return checkCharValue(compileTimeConstant, expectedType, expression);
        }

        return false;
    }

    private boolean checkCharValue(ConstantValue<?> constant, CangJieType expectedType, CjConstantExpression expression) {
        if (!noExpectedTypeOrError(expectedType)
                && !CangJieTypeChecker.DEFAULT.isSubtypeOf(builtIns.getRuneType(), expectedType)) {
            return reportConstantExpectedTypeMismatch(expression, "character", expectedType, builtIns.getRuneType());
        }

        if (constant != null) {
            return false;
        }

        Diagnostic diagnostic = parseCharacter(expression).getDiagnostic();
        if (diagnostic != null) {
            return reportError(diagnostic);
        }
        return false;
    }

    private boolean checkBooleanValue(
            @NotNull CangJieType expectedType,
            @NotNull CjConstantExpression expression
    ) {
        if (!noExpectedTypeOrError(expectedType)
                && !CangJieTypeChecker.DEFAULT.isSubtypeOf(builtIns.getBoolType(), expectedType)) {
            return reportConstantExpectedTypeMismatch(expression, "Bool", expectedType, builtIns.getBoolType());
        }
        return false;
    }

    private boolean checkFloatValue(
            @Nullable ConstantValue<?> value,
            @NotNull CangJieType expectedType,
            @NotNull CjConstantExpression expression
    ) {
        if (value == null) {
            return reportError(FLOAT_LITERAL_OUT_OF_RANGE.on(expression));
        }
        if (!noExpectedTypeOrError(expectedType)) {
            CangJieType valueType = value.getType(module);
            if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
                return reportConstantExpectedTypeMismatch(expression, "floating-point", expectedType, null);
            }
        }
        return false;
    }

    private boolean reportConstantExpectedTypeMismatch(
            @NotNull CjConstantExpression expression,
            @NotNull String typeName,
            @NotNull CangJieType expectedType,
            @Nullable CangJieType expressionType
    ) {
//        if (DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(context, expression, expectedType, expressionType)) return true;

        trace.report(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, typeName, expectedType));
        return true;
    }

    public static class CharacterWithDiagnostic {
        private Diagnostic diagnostic;
        private Character value;

        public CharacterWithDiagnostic(@NotNull Diagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }

        public CharacterWithDiagnostic(char value) {
            this.value = value;
        }

        @Nullable
        public Diagnostic getDiagnostic() {
            return diagnostic;
        }

        @Nullable
        public Character getValue() {
            return value;
        }
    }

}
