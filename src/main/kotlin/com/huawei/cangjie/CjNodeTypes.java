package com.huawei.cangjie;

import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

public interface CjNodeTypes {
    IElementType TYPE_REFERENCE = CjStubElementTypes.TYPE_REFERENCE;
    IElementType VALUE_PARAMETER_LIST = CjStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER = CjStubElementTypes.VALUE_PARAMETER;
    IElementType CLASS = CjStubElementTypes.CLASS;
    IElementType INTERFACE = CjStubElementTypes.INTERFACE;
    IElementType STRUCT = CjStubElementTypes.STRUCT;
    IElementType PROPERTY = CjStubElementTypes.PROPERTY;
    IElementType PROPERTY_BODY = CjStubElementTypes.PROPERTY_BODY;

    IElementType PROPERTY_GET = CjStubElementTypes.PROPERTY_GET;

    IElementType PROPERTY_SET = CjStubElementTypes.PROPERTY_SET;
    IElementType VALUE_ARGUMENT_NAME                = CjStubElementTypes.VALUE_ARGUMENT_NAME;
    IElementType VALUE_ARGUMENT                     = CjStubElementTypes.VALUE_ARGUMENT;
    IElementType VALUE_ARGUMENT_LIST                = CjStubElementTypes.VALUE_ARGUMENT_LIST;

    IElementType POSTFIX_EXPRESSION        = new CjNodeType("POSTFIX_EXPRESSION", CjPostfixExpression.class);
    IElementType CALL_EXPRESSION           = new CjNodeType("CALL_EXPRESSION", CjCallExpression.class);
    IElementType INDICES                   = new CjNodeType("INDICES", CjContainerNode.class);
    IElementType ARRAY_ACCESS_EXPRESSION   = new CjNodeType("ARRAY_ACCESS_EXPRESSION", CjArrayAccessExpression.class);
    IElementType IS_EXPRESSION             = new CjNodeType("IS_EXPRESSION", CjIsExpression.class);
    IElementType BINARY_WITH_TYPE          = new CjNodeType("BINARY_WITH_TYPE", CjBinaryExpressionWithTypeRHS.class);
    IElementType BINARY_EXPRESSION         = new CjNodeType("BINARY_EXPRESSION", CjBinaryExpression.class);
    IElementType PREFIX_EXPRESSION         = new CjNodeType("PREFIX_EXPRESSION",CjPrefixExpression.class);
    IElementType OPERATION_REFERENCE       = new CjNodeType("OPERATION_REFERENCE", CjOperationReferenceExpression.class);
    IElementType VARIABLE = CjStubElementTypes.VARIABLE;
    IElementType MAIN_FUNC = CjStubElementTypes.MAIN_FUNC;
    IElementType MACRO = CjStubElementTypes.MACRO;
    IElementType FUNC = CjStubElementTypes.FUNCTION;
    IFileElementType CJ_FILE = new IFileElementType(CangJieLanguage.INSTANCE);
    IElementType BLOCK = new BlockExpressionElementType();
    IElementType LABEL = new CjNodeType("LABEL", CjLabelReferenceExpression.class);
    IElementType CLASS_BODY = CjStubElementTypes.CLASS_BODY;

    IElementType FUNCTION_LITERAL          = new CjNodeType("FUNCTION_LITERAL", CjFunctionLiteral.class);
    IElementType LABEL_QUALIFIER = new CjNodeType("LABEL_QUALIFIER", CjContainerNode.class);
    IElementType PACKAGE_DIRECTIVE = CjStubElementTypes.PACKAGE_DIRECTIVE;
    IElementType SUPER_TYPE_ENTRY                   = CjStubElementTypes.SUPER_TYPE_ENTRY;
    IElementType MODIFIER_LIST = CjStubElementTypes.MODIFIER_LIST;
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new CjNodeType("DESTRUCTURING_DECLARATION_ENTRY", CjDestructuringDeclarationEntry.class);
    IElementType DESTRUCTURING_DECLARATION = new CjNodeType("DESTRUCTURING_DECLARATION", CjDestructuringDeclaration.class);
//    IElementType CLASS_INITIALIZER = CjStubElementTypes.CLASS_INITIALIZER;

    IElementType CLASS_INIT = CjStubElementTypes.CLASS_INIT;
    IElementType REFERENCE_EXPRESSION = CjStubElementTypes.REFERENCE_EXPRESSION;
    IElementType TYPE_PARAMETER_LIST = CjStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_CONSTRAINT_LIST = CjStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType USER_TYPE = CjStubElementTypes.USER_TYPE;


    IElementType TUPLE_TYPE = CjStubElementTypes.TUPLE_TYPE;
    IElementType BASIC_TYPE = CjStubElementTypes.BASIC_TYPE;
    IElementType TYPE_PARAMETER = CjStubElementTypes.TYPE_PARAMETER;

    IElementType ENUM = CjStubElementTypes.ENUM;

    IElementType EXTEND = CjStubElementTypes.EXTEND;
    IElementType ENUM_BODY = CjStubElementTypes.ENUM_BODY;
    IElementType ENUM_ENTRY = CjStubElementTypes.ENUM_ENTRY;

    IElementType TYPE_LIST = CjStubElementTypes.TYPE_LIST;
    IElementType SUPER_TYPE_LIST                    = CjStubElementTypes.SUPER_TYPE_LIST;
    IElementType TYPE_CONSTRAINT = CjStubElementTypes.TYPE_CONSTRAINT;
    IElementType IMPORT_LIST                        = CjStubElementTypes.IMPORT_LIST;
    IElementType DOT_QUALIFIED_EXPRESSION  = CjStubElementTypes.DOT_QUALIFIED_EXPRESSION;

    IElementType IMPORT_ALIAS                       = CjStubElementTypes.IMPORT_ALIAS;
    IElementType IMPORT_DIRECTIVE                   = CjStubElementTypes.IMPORT_DIRECTIVE;

    IElementType IMPORT_DIRECTIVE_ITEM                   = CjStubElementTypes.IMPORT_DIRECTIVE_ITEM;

    IElementType LONG_STRING_TEMPLATE_ENTRY    = CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY;
    IElementType SHORT_STRING_TEMPLATE_ENTRY   = CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY;
    IElementType FLOAT_CONSTANT     = CjStubElementTypes.FLOAT_CONSTANT;
    IElementType CHARACTER_CONSTANT = CjStubElementTypes.CHARACTER_CONSTANT;
    IElementType INTEGER_CONSTANT   = CjStubElementTypes.INTEGER_CONSTANT;
    IElementType BOOLEAN_CONSTANT   = CjStubElementTypes.BOOLEAN_CONSTANT;



    IElementType UNIT_CONSTANT = CjStubElementTypes.UNIT_CONSTANT;
    IElementType STRING_TEMPLATE               = CjStubElementTypes.STRING_TEMPLATE;
    IElementType TYPE_PROJECTION          = CjStubElementTypes.TYPE_PROJECTION;
    IElementType TYPE_ARGUMENT_LIST                 = CjStubElementTypes.TYPE_ARGUMENT_LIST;
    IElementType LITERAL_STRING_TEMPLATE_ENTRY = CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY;
    IElementType ESCAPE_STRING_TEMPLATE_ENTRY  = CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY;
    IElementType SUPER_EXPRESSION          = new CjNodeType("SUPER_EXPRESSION", CjSuperExpression.class);
    IElementType THIS_EXPRESSION           = new CjNodeType("THIS_EXPRESSION", CjThisExpression.class);
    IElementType COLLECTION_LITERAL_EXPRESSION = CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION;


    IElementType TUPLE_LITERAL_EXPRESSION = CjStubElementTypes.TUPLE_LITERAL_EXPRESSION;
    IElementType PARENTHESIZED             = new CjNodeType("PARENTHESIZED", CjParenthesizedExpression.class);

    IElementType CONTINUE                  = new CjNodeType("CONTINUE", CjContinueExpression.class);

    IElementType BREAK                     = new CjNodeType("BREAK", CjBreakExpression.class);

    IElementType RETURN                    = new CjNodeType("RETURN", CjReturnExpression.class);

    IElementType THROW                     = new CjNodeType("THROW", CjThrowExpression.class);
    IElementType THEN                      = new CjNodeType("THEN", CjContainerNodeForControlStructureBody.class);

    IElementType ELSE                      = new CjNodeType("ELSE", CjContainerNodeForControlStructureBody.class);

    IElementType IF                        = new CjNodeType("IF", CjIfExpression.class);
    IElementType CONDITION                 = new CjNodeType("CONDITION", CjContainerNode.class);

    IElementType LET_EXPRESSION                 = new CjNodeType("LET_EXPRESSION", CjLetExpression.class);
    IElementType TRY                       = new CjNodeType("TRY", CjTryExpression.class);

    IElementType CATCH                     = new CjNodeType("CATCH", CjCatchClause.class);
    IElementType FINALLY                   = new CjNodeType("FINALLY", CjFinallySection.class);


    IElementType WHILE                     = new CjNodeType("WHILE", CjWhileExpression.class);
    IElementType BODY                      = new CjNodeType("BODY", CjContainerNodeForControlStructureBody.class);

    IElementType DO_WHILE                  = new CjNodeType("DO_WHILE", CjDoWhileExpression.class);

    IElementType LOOP_RANGE                = new CjNodeType("LOOP_RANGE", CjContainerNode.class);
    IElementType FOR                       = new CjNodeType("FOR", CjForExpression.class);

    IElementType MATCH                      = new CjNodeType("MATCH", CjMatchExpression.class);
    IElementType MATCH_ENTRY                = new CjNodeType("MATCH_ENTRY", CjMatchEntry.class);


    IElementType CASE_PATTERN = new CjNodeType("CASE_PATTERN", CjCasePattern.class);



    IElementType TYPE_PATTERN = new CjNodeType("TYPE_PATTERN", CjTypePattern.class);
    IElementType ENUM_PATTERN = new CjNodeType("ENUM_PATTERN", CjEnumPattern.class);
    IElementType FUNCTION_TYPE            = CjStubElementTypes.FUNCTION_TYPE;
    IElementType PROPERTY_ACCESSOR       = CjStubElementTypes.PROPERTY_ACCESSOR;
    IElementType ERROR_ELEMENT = new CjNodeType("ERROR_ELEMENT", CjErrorElement.class);

    IElementType SECONDARY_CONSTRUCTOR  = CjStubElementTypes.SECONDARY_CONSTRUCTOR;
    IElementType PRIMARY_CONSTRUCTOR    = CjStubElementTypes.PRIMARY_CONSTRUCTOR;
    IElementType CONTEXT_RECEIVER_LIST  = CjStubElementTypes.CONTEXT_RECEIVER_LIST;
    IFileElementType TYPE_CODE_FRAGMENT = CjStubElementTypes.TYPE_CODE_FRAGMENT;
    IFileElementType EXPRESSION_CODE_FRAGMENT = CjStubElementTypes.EXPRESSION_CODE_FRAGMENT;
    IFileElementType BLOCK_CODE_FRAGMENT = CjStubElementTypes.BLOCK_CODE_FRAGMENT;
}


