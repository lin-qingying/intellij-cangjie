package com.huawei.cangjie.utils.exceptions;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.huawei.cangjie.lexer.CjSingleValueToken;
import com.huawei.cangjie.lexer.CjToken;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.Name;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import static com.huawei.cangjie.utils.OperatorNameConventions.*;

public class OperatorConventions {

    private OperatorConventions() {}

    // Names for primitive type conversion properties
    public static final Name DOUBLE = Name.identifier("toDouble");
    public static final Name FLOAT = Name.identifier("toFloat");
    public static final Name LONG = Name.identifier("toLong");
    public static final Name INT = Name.identifier("toInt");
    public static final Name CHAR = Name.identifier("toChar");
    public static final Name SHORT = Name.identifier("toShort");
    public static final Name BYTE = Name.identifier("toByte");


    public static final ImmutableSet<Name> NUMBER_CONVERSIONS = ImmutableSet.of(
            DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR
    );

    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well

    public static final ImmutableBiMap<CjSingleValueToken, Name> UNARY_OPERATION_NAMES = ImmutableBiMap.<CjSingleValueToken, Name>builder()
            .put(CjTokens.PLUSPLUS, INC)
            .put(CjTokens.MINUSMINUS, DEC)
            .put(CjTokens.PLUS, UNARY_PLUS)
            .put(CjTokens.MINUS, UNARY_MINUS)
            .put(CjTokens.EXCL, NOT)
            .build();

    public static final ImmutableBiMap<CjSingleValueToken, Name> BINARY_OPERATION_NAMES = ImmutableBiMap.<CjSingleValueToken, Name>builder()
            .put(CjTokens.MUL, TIMES)
            .put(CjTokens.PLUS, PLUS)
            .put(CjTokens.MINUS, MINUS)
            .put(CjTokens.DIV, DIV)
            .put(CjTokens.PERC, REM)
            .put(CjTokens.RANGE, RANGE_TO)

            .build();

    public static final ImmutableBiMap<Name, Name> REM_TO_MOD_OPERATION_NAMES = ImmutableBiMap.<Name, Name>builder()
            .put(REM, MOD)
            .put(REM_ASSIGN, MOD_ASSIGN)
            .build();

    public static final ImmutableSet<CjSingleValueToken> NOT_OVERLOADABLE =
            ImmutableSet.of(CjTokens.ANDAND, CjTokens.OROR);

    public static final ImmutableSet<CjSingleValueToken> INCREMENT_OPERATIONS =
            ImmutableSet.of(CjTokens.PLUSPLUS, CjTokens.MINUSMINUS);

    public static final ImmutableSet<CjSingleValueToken> COMPARISON_OPERATIONS =
            ImmutableSet.of(CjTokens.LT, CjTokens.GT, CjTokens.LTEQ, CjTokens.GTEQ);

    public static final ImmutableSet<CjSingleValueToken> EQUALS_OPERATIONS =
            ImmutableSet.of(CjTokens.EQEQ, CjTokens.EXCLEQ);



    public static final ImmutableSet<CjSingleValueToken> IN_OPERATIONS =
            ImmutableSet.of(CjTokens.IN_KEYWORD);

    public static final ImmutableBiMap<CjSingleValueToken, Name> ASSIGNMENT_OPERATIONS = ImmutableBiMap.<CjSingleValueToken, Name>builder()
            .put(CjTokens.MULTEQ, TIMES_ASSIGN)
            .put(CjTokens.DIVEQ, DIV_ASSIGN)
            .put(CjTokens.PERCEQ, REM_ASSIGN)
            .put(CjTokens.PLUSEQ, PLUS_ASSIGN)
            .put(CjTokens.MINUSEQ, MINUS_ASSIGN)
            .build();

    public static final ImmutableBiMap<CjSingleValueToken, CjSingleValueToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableBiMap.<CjSingleValueToken, CjSingleValueToken>builder()
            .put(CjTokens.MULTEQ, CjTokens.MUL)
            .put(CjTokens.DIVEQ, CjTokens.DIV)
            .put(CjTokens.PERCEQ, CjTokens.PERC)
            .put(CjTokens.PLUSEQ, CjTokens.PLUS)
            .put(CjTokens.MINUSEQ, CjTokens.MINUS)
            .build();

    public static final Name ASSIGN_METHOD = Name.identifier("assign");

    public static final ImmutableBiMap<CjSingleValueToken, Name> BOOLEAN_OPERATIONS = ImmutableBiMap.<CjSingleValueToken, Name>builder()
            .put(CjTokens.ANDAND, AND)
            .put(CjTokens.OROR, OR)
            .build();

    public static final ImmutableSet<Name> CONVENTION_NAMES = ImmutableSet.<Name>builder()
            .add(GET, SET, INVOKE, CONTAINS, ITERATOR, NEXT, HAS_NEXT, EQUALS, COMPARE_TO, GET_VALUE, SET_VALUE)
            .addAll(UNARY_OPERATION_NAMES.values())
            .addAll(BINARY_OPERATION_NAMES.values())
            .addAll(ASSIGNMENT_OPERATIONS.values())
            .build();

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull CjToken token) {
        return getNameForOperationSymbol(token, true, true);
    }

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull CjToken token, boolean unaryOperations, boolean binaryOperations) {
        Name name;

        if (binaryOperations) {
            name = BINARY_OPERATION_NAMES.get(token);
            if (name != null) return name;
        }

        if (unaryOperations) {
            name = UNARY_OPERATION_NAMES.get(token);
            if (name != null) return name;
        }

        name = ASSIGNMENT_OPERATIONS.get(token);
        if (name != null) return name;
        if (COMPARISON_OPERATIONS.contains(token)) return COMPARE_TO;
        if (EQUALS_OPERATIONS.contains(token)) return EQUALS;
        if (IN_OPERATIONS.contains(token)) return CONTAINS;
        return null;
    }

    @Nullable
    public static CjToken getOperationSymbolForName(@NotNull Name name) {
        if (!isConventionName(name)) return null;

        CjToken token;
        token = BINARY_OPERATION_NAMES.inverse().get(name);
        if (token != null) return token;
        token = UNARY_OPERATION_NAMES.inverse().get(name);
        if (token != null) return token;
        token = ASSIGNMENT_OPERATIONS.inverse().get(name);
        return token;
    }

    public static boolean isConventionName(@NotNull Name name) {
        return CONVENTION_NAMES.contains(name) || COMPONENT_REGEX.matches(name.asString());
    }
}
