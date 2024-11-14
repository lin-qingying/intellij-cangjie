package com.linqingying.cangjie;

import com.linqingying.cangjie.lang.CangJieLanguage;
import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public interface CjNodeTypes {


//    IElementType NULL = CjStubElementTypes.NULL;
IElementType CATCH_PARAMETER = CjStubElementTypes.CATCH_PARAMETER;

    IElementType TYPE_REFERENCE = CjStubElementTypes.TYPE_REFERENCE;
    IElementType VALUE_PARAMETER_LIST = CjStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER = CjStubElementTypes.VALUE_PARAMETER;
    IElementType CLASS = CjStubElementTypes.CLASS;
    IElementType OPTIONAL_TYPE            = CjStubElementTypes.OPTIONAL_TYPE;


    IElementType INVALID_DECLARATION = new IElementType("INVALID_DECLARATION", CangJieLanguage.INSTANCE);
    IElementType INTERFACE = CjStubElementTypes.INTERFACE;
    IElementType STRUCT = CjStubElementTypes.STRUCT;
    IElementType PROPERTY = CjStubElementTypes.PROPERTY;

    IElementType TYPEALIAS = CjStubElementTypes.TYPEALIAS;


    IElementType PROPERTY_BODY = CjStubElementTypes.PROPERTY_BODY;

    IElementType PROPERTY_GET = CjStubElementTypes.PROPERTY_GET;

    IElementType PROPERTY_SET = CjStubElementTypes.PROPERTY_SET;
    IElementType VALUE_ARGUMENT_NAME = CjStubElementTypes.VALUE_ARGUMENT_NAME;
    IElementType VALUE_ARGUMENT = CjStubElementTypes.VALUE_ARGUMENT;
    IElementType VALUE_ARGUMENT_LIST = CjStubElementTypes.VALUE_ARGUMENT_LIST;

    IElementType OPERATOR = new CjNodeType("OPERATOR", CjOperator.class);

//    区间表达式

    IElementType RANGE_EXPRESSION = new CjNodeType("RANGE_EXPRESSION", CjRangeExpression.class);

    IElementType SLICE_EXPRESSION = new CjNodeType("SLICE_EXPRESSION", CjSliceExpression.class);
    IElementType POSTFIX_EXPRESSION = new CjNodeType("POSTFIX_EXPRESSION", CjPostfixExpression.class);
    //CjUnsafeExpression
    IElementType UNSAFE_EXPRESSION = new CjNodeType("UNSAFE_EXPRESSION", CjUnsafeExpression.class);
    IElementType SPAWN_EXPRESSION = new CjNodeType("SPAWN_EXPRESSION", CjSpawnExpression.class);
    IElementType SYNCHRONIZED_EXPRESSION = new CjNodeType("SYNCHRONIZED_EXPRESSION", CjSynchronizedExpression.class);
    IElementType CALL_EXPRESSION = new CjNodeType("CALL_EXPRESSION", CjCallExpression.class);
    IElementType QUOTE_EXPRESSION = new CjNodeType("QUOTE_EXPRESSION", CjQuoteExpression.class);

    IElementType INDICES = new CjNodeType("INDICES", CjContainerNode.class);
    IElementType ARRAY_ACCESS_EXPRESSION = new CjNodeType("ARRAY_ACCESS_EXPRESSION", CjArrayAccessExpression.class);
    IElementType IS_EXPRESSION = new CjNodeType("IS_EXPRESSION", CjIsExpression.class);
    IElementType BINARY_WITH_TYPE = new CjNodeType("BINARY_WITH_TYPE", CjBinaryExpressionWithTypeRHS.class);
    IElementType BINARY_EXPRESSION = new CjNodeType("BINARY_EXPRESSION", CjBinaryExpression.class);
    IElementType PREFIX_EXPRESSION = new CjNodeType("PREFIX_EXPRESSION", CjPrefixExpression.class);
    IElementType OPERATION_REFERENCE = new CjNodeType("OPERATION_REFERENCE", CjOperationReferenceExpression.class);

    IElementType OPERATION_NAME = new CjNodeType("OPERATION_NAME", CjOperationName.class);
    IElementType CONSTRUCTOR_DELEGATION_CALL = new CjNodeType.CjLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", CjConstructorDelegationCall.class);
    IElementType CONSTRUCTOR_DELEGATION_REFERENCE = new CjNodeType.CjLeftBoundNodeType("CONSTRUCTOR_DELEGATION_REFERENCE", CjConstructorDelegationReferenceExpression.class);

    IElementType VARIABLE = CjStubElementTypes.VARIABLE;
    IElementType MAIN_FUNC = CjStubElementTypes.MAIN_FUNC;
    IElementType MACRO = CjStubElementTypes.MACRO;
    IElementType FOREIGN = CjStubElementTypes.FOREIGN;

    IElementType LAMBDA_ARGUMENT = CjStubElementTypes.LAMBDA_ARGUMENT;

    IElementType FOREIGN_BODY = CjStubElementTypes.FOREIGN_BODY;
    IElementType FUNC = CjStubElementTypes.FUNCTION;
    IElementType FUNC_EXTEND = CjStubElementTypes.FUNCTION_EXTEND;
    IFileElementType CJ_FILE = new IFileElementType(CangJieLanguage.INSTANCE);
    IElementType BLOCK = new BlockExpressionElementType();
    IElementType CASE_BLOCK = new CaseBlockExpressionElementType();
    IElementType INIT_BLOCK = new InitBlockExpressionElementType();

    IElementType LAMBDA_EXPRESSION = new LambdaExpressionElementType();

    IElementType LABEL = new CjNodeType("LABEL", CjLabelReferenceExpression.class);
    IElementType CLASS_BODY = CjStubElementTypes.CLASS_BODY;
    IElementType INTERFACE_BODY = CjStubElementTypes.INTERFACE_BODY;

    IElementType TUPLE_EXPRESSION =new CjNodeType("TUPLE_EXPRESSION", CjTupleExpression.class);
    IElementType QUOTE_INTERPOLATE = new CjNodeType("QUOTE_INTERPOLATE", CjQuoteInterpolate.class);

    IElementType FUNCTION_LITERAL = new CjNodeType("FUNCTION_LITERAL", CjFunctionLiteral.class);
    IElementType LABEL_QUALIFIER = new CjNodeType("LABEL_QUALIFIER", CjContainerNode.class);
    IElementType PACKAGE_DIRECTIVE = CjStubElementTypes.PACKAGE_DIRECTIVE;

    IElementType SAFE_ACCESS_EXPRESSION    = new CjNodeType("SAFE_ACCESS_EXPRESSION", CjSafeQualifiedExpression.class);

    IElementType SUPER_TYPE_ENTRY = CjStubElementTypes.SUPER_TYPE_ENTRY;
    IElementType MODIFIER_LIST = CjStubElementTypes.MODIFIER_LIST;
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new CjNodeType("DESTRUCTURING_DECLARATION_ENTRY", CjDestructuringDeclarationEntry.class);
    IElementType DESTRUCTURING_DECLARATION = new CjNodeType("DESTRUCTURING_DECLARATION", CjDestructuringDeclaration.class);
//    IElementType CLASS_INITIALIZER = CjStubElementTypes.CLASS_INITIALIZER;

    IElementType CLASS_INIT = CjStubElementTypes.CLASS_INIT;
    IElementType CLASS_TILDE_INIT = CjStubElementTypes.CLASS_TILDE_INIT;
    IElementType CLASS_MAIN_INIT = CjStubElementTypes.CLASS_MAIN_INIT;
    IElementType REFERENCE_EXPRESSION = CjStubElementTypes.REFERENCE_EXPRESSION;
    IElementType TYPE_PARAMETER_LIST = CjStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_CONSTRAINT_LIST = CjStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType USER_TYPE = CjStubElementTypes.USER_TYPE;
    IElementType FUNCTION_TYPE = CjStubElementTypes.FUNCTION_TYPE;

    IElementType PARENTHESIZED_TYPE = CjStubElementTypes.PARENTHESIZED_TYPE;
    IElementType TUPLE_TYPE = CjStubElementTypes.TUPLE_TYPE;
        IElementType BASIC_TYPE = CjStubElementTypes.BASIC_TYPE;
        IElementType THIS_TYPE = CjStubElementTypes.THIS_TYPE;
//    IElementType BASIC_TYPE = new CjNodeType("BASIC_TYPE", CjBasicType.class);
    IElementType TYPE_PARAMETER = CjStubElementTypes.TYPE_PARAMETER;

    IElementType ENUM = CjStubElementTypes.ENUM;

    IElementType EXTEND = CjStubElementTypes.EXTEND;
    IElementType ENUM_BODY = CjStubElementTypes.ENUM_BODY;
    IElementType ENUM_ENTRY = CjStubElementTypes.ENUM_ENTRY;

    IElementType TYPE_LIST = CjStubElementTypes.TYPE_LIST;
    IElementType SUPER_TYPE_LIST = CjStubElementTypes.SUPER_TYPE_LIST;
    IElementType TYPE_CONSTRAINT = CjStubElementTypes.TYPE_CONSTRAINT;
    IElementType IMPORT_LIST = CjStubElementTypes.IMPORT_LIST;
    IElementType DOT_QUALIFIED_EXPRESSION = CjStubElementTypes.DOT_QUALIFIED_EXPRESSION;

    IElementType IMPORT_ALIAS = CjStubElementTypes.IMPORT_ALIAS;
    IElementType MULIT_IMPORT_DIRECTIVE = new CjNodeType("MULTI_IMPORT_DIRECTIVE", CjMultiImportDirective.class);
    IElementType MULIT_IMPORT_DIRECTIVE1 = new CjNodeType("MULTI_IMPORT_DIRECTIVE1", CjMultiImportDirective1.class);

    IElementType IMPORT_DIRECTIVE = CjStubElementTypes.IMPORT_DIRECTIVE;

//    IElementType IMPORT_DIRECTIVE_ITEM = CjStubElementTypes.IMPORT_DIRECTIVE_ITEM;
    IElementType ANNOTATION_ENTRY = CjStubElementTypes.ANNOTATION_ENTRY;
    IElementType MACRO_EXPRESSION = CjStubElementTypes.MACRO_EXPRESSION;

    IElementType LONG_STRING_TEMPLATE_ENTRY = CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    IElementType SHORT_STRING_TEMPLATE_ENTRY = CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    IElementType FLOAT_CONSTANT = CjStubElementTypes.FLOAT_CONSTANT;
    IElementType RUNE_CONSTANT = CjStubElementTypes.RUNE_CONSTANT;
    IElementType CHARACTER_BYTE_CONSTANT = CjStubElementTypes.CHARACTER_BYTE_CONSTANT;
    IElementType INTEGER_CONSTANT = CjStubElementTypes.INTEGER_CONSTANT;
    IElementType BOOLEAN_CONSTANT = CjStubElementTypes.BOOLEAN_CONSTANT;


    IElementType UNIT_CONSTANT = CjStubElementTypes.UNIT_CONSTANT;
    IElementType STRING_TEMPLATE = CjStubElementTypes.STRING_TEMPLATE;
    IElementType TYPE_PROJECTION = CjStubElementTypes.TYPE_PROJECTION;
    IElementType TYPE_ARGUMENT_LIST = CjStubElementTypes.TYPE_ARGUMENT_LIST;
    //    IElementType MACRO_ARGUMENT_LIST                 = CjStubElementTypes.MACRO_ARGUMENT_LIST;
    IElementType LITERAL_STRING_TEMPLATE_ENTRY = CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    IElementType ESCAPE_STRING_TEMPLATE_ENTRY = CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;
    IElementType SUPER_EXPRESSION = new CjNodeType("SUPER_EXPRESSION", CjSuperExpression.class);
    IElementType THIS_EXPRESSION = new CjNodeType("THIS_EXPRESSION", CjThisExpression.class);
    IElementType COLLECTION_LITERAL_EXPRESSION = CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION;


    IElementType PARENTHESIZED = new CjNodeType("PARENTHESIZED", CjParenthesizedExpression.class);

    IElementType CONTINUE = new CjNodeType("CONTINUE", CjContinueExpression.class);

    IElementType BREAK = new CjNodeType("BREAK", CjBreakExpression.class);

    IElementType RETURN = new CjNodeType("RETURN", CjReturnExpression.class);

    IElementType THROW = new CjNodeType("THROW", CjThrowExpression.class);
    IElementType THEN = new CjNodeType("THEN", CjContainerNodeForControlStructureBody.class);

    IElementType ELSE = new CjNodeType("ELSE", CjContainerNodeForControlStructureBody.class);

    IElementType IF = new CjNodeType("IF", CjIfExpression.class);
    IElementType CONDITION = new CjNodeType("CONDITION", CjContainerNode.class);

    IElementType LET_EXPRESSION = new CjNodeType("LET_EXPRESSION", CjLetExpression.class);
    IElementType TRY = new CjNodeType("TRY", CjTryExpression.class);
    IElementType TRY_RESOURCE = new CjNodeType("TRY_RESOURCE", CjTryResource.class);
    IElementType TRY_RESOURCE_LIST = new CjNodeType("TRY_RESOURCE_LIST", CjTryResourceList.class);

    IElementType CATCH = new CjNodeType("CATCH", CjCatchClause.class);
    IElementType FINALLY = new CjNodeType("FINALLY", CjFinallySection.class);


    IElementType WHILE = new CjNodeType("WHILE", CjWhileExpression.class);
    IElementType BODY = new CjNodeType("BODY", CjContainerNodeForControlStructureBody.class);

    IElementType DO_WHILE = new CjNodeType("DO_WHILE", CjDoWhileExpression.class);
    IElementType PATTERN_GUARD = new CjNodeType("PATTERN_GUARD", CjPatternGuard.class);
    IElementType LOOP_RANGE = new CjNodeType("LOOP_RANGE", CjContainerNode.class);
    IElementType FOR = new CjNodeType("FOR", CjForExpression.class);

    IElementType MATCH = new CjNodeType("MATCH", CjMatchExpression.class);
    IElementType MATCH_ENTRY = new CjNodeType("MATCH_ENTRY", CjMatchEntry.class);


//    IElementType CASE_PATTERN = new CjNodeType("CASE_PATTERN", CjCasePattern.class);

//    IElementType CHARACTER_CONSTANT = CjStubElementTypes.CHARACTER_CONSTANT;
IElementType WILDCARD_PATTERN = new CjNodeType("WILDCARD_PATTERN", CjWildcardPattern.class);
    IElementType TYPE_PATTERN = new CjNodeType("TYPE_PATTERN", CjTypePattern.class);
    IElementType ENUM_PATTERN = new CjNodeType("ENUM_PATTERN", CjEnumPattern.class);
    IElementType BINDING_PATTERN = new CjNodeType("BINDING_PATTERN", CjBindingPattern.class);
    IElementType TUPLE_PATTERN = new CjNodeType("TUPLE_PATTERN", CjTuplePattern.class);
    IElementType CONSTANT_PATTERN = new CjNodeType("CONSTANT_PATTERN", CjConstantPattern.class);

    IElementType PROPERTY_ACCESSOR = CjStubElementTypes.PROPERTY_ACCESSOR;
    IElementType ERROR_ELEMENT = new CjNodeType("ERROR_ELEMENT", CjErrorElement.class);

    IElementType SECONDARY_CONSTRUCTOR = CjStubElementTypes.SECONDARY_CONSTRUCTOR;
    IElementType PRIMARY_CONSTRUCTOR = CjStubElementTypes.PRIMARY_CONSTRUCTOR;
    IElementType CONTEXT_RECEIVER_LIST = CjStubElementTypes.CONTEXT_RECEIVER_LIST;


//    IFileElementType TYPE_CODE_FRAGMENT = CjStubElementTypes.TYPE_CODE_FRAGMENT;
//    IFileElementType EXPRESSION_CODE_FRAGMENT = CjStubElementTypes.EXPRESSION_CODE_FRAGMENT;
//    IFileElementType BLOCK_CODE_FRAGMENT = CjStubElementTypes.BLOCK_CODE_FRAGMENT;

    CjTypeCodeFragmentType TYPE_CODE_FRAGMENT = new CjTypeCodeFragmentType();
    CjExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new CjExpressionCodeFragmentType();
    CjBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new CjBlockCodeFragmentType();


    IElementType MATCH_CONDITION_EXPRESSION = new CjNodeType("MATCH_CONDITION_WITH_EXPRESSION", CjMatchConditionWithExpression.class);

    TokenSet CONSTANT_EXPRESSIONS_TYPES = TokenSet.create(

            BOOLEAN_CONSTANT,
            FLOAT_CONSTANT,
            RUNE_CONSTANT,
            INTEGER_CONSTANT,

            REFERENCE_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,

            STRING_TEMPLATE,



            COLLECTION_LITERAL_EXPRESSION
    );


}


