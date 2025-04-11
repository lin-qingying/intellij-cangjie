///*
// * Copyright 2024 LinQingYing. and contributors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * The use of this source code is governed by the Apache License 2.0,
// * which allows users to freely use, modify, and distribute the code,
// * provided they adhere to the terms of the license.
// *
// * The software is provided "as-is", and the authors are not responsible for
// * any damages or issues arising from its use.
// *
// */
//package cn.cangnova.cangjie.psi.nodetypes
//
//import cn.cangnova.cangjie.lang.CangJieLanguage
//import cn.cangnova.cangjie.psi.*
//import cn.cangnova.cangjie.psi.CjNodeType
//import cn.cangnova.cangjie.psi.stubs.elements.BlockExpressionElementType
//import cn.cangnova.cangjie.psi.stubs.elements.CaseBlockExpressionElementType
//import cn.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
//import cn.cangnova.cangjie.psi.stubs.elements.InitBlockExpressionElementType
//import cn.cangnova.cangjie.psi.stubs.elements.LambdaExpressionElementType
//
//import com.intellij.psi.tree.IElementType
//import com.intellij.psi.tree.IFileElementType
//import com.intellij.psi.tree.TokenSet
//
//
//        //    IElementType NULL = CjStubElementTypes.NULL;
//        val CATCH_PARAMETER: IElementType = CjStubElementTypes.CATCH_PARAMETER
//
//
//        val TYPE_REFERENCE: IElementType = CjStubElementTypes.TYPE_REFERENCE
//
//        val VALUE_PARAMETER_LIST: IElementType = CjStubElementTypes.VALUE_PARAMETER_LIST
//
//        val VALUE_PARAMETER: IElementType = CjStubElementTypes.VALUE_PARAMETER
//
//        val CLASS: IElementType = CjStubElementTypes.CLASS
//
//        val OPTIONAL_TYPE: IElementType = CjStubElementTypes.OPTIONAL_TYPE
//
//
//
//        val INVALID_DECLARATION: IElementType = IElementType("INVALID_DECLARATION", CangJieLanguage)
//
//        val INTERFACE: IElementType = CjStubElementTypes.INTERFACE
//
//        val STRUCT: IElementType = CjStubElementTypes.STRUCT
//
//        val PROPERTY: IElementType = CjStubElementTypes.PROPERTY
//
//
//        val TYPEALIAS: IElementType = CjStubElementTypes.TYPEALIAS
//
//
//
//        val PROPERTY_BODY: IElementType = CjStubElementTypes.PROPERTY_BODY
//
//        val PROPERTY_GET: IElementType = CjStubElementTypes.PROPERTY_GET
//
//        val PROPERTY_SET: IElementType = CjStubElementTypes.PROPERTY_SET
//        val VALUE_ARGUMENT_NAME: IElementType = CjStubElementTypes.VALUE_ARGUMENT_NAME
//        val VALUE_ARGUMENT: IElementType = CjStubElementTypes.VALUE_ARGUMENT
//        val VALUE_ARGUMENT_LIST: IElementType = CjStubElementTypes.VALUE_ARGUMENT_LIST
//
//        val OPERATOR: IElementType = CjNodeType("OPERATOR", CjOperator::class.java)
//
//        //    区间表达式
//        val RANGE_EXPRESSION: IElementType = CjNodeType("RANGE_EXPRESSION", CjRangeExpression::class.java)
//
//        val SLICE_EXPRESSION: IElementType = CjNodeType("SLICE_EXPRESSION", CjSliceExpression::class.java)
//        val POSTFIX_EXPRESSION: IElementType = CjNodeType("POSTFIX_EXPRESSION", CjPostfixExpression::class.java)
//
//        //CjUnsafeExpression
//        val UNSAFE_EXPRESSION: IElementType = CjNodeType("UNSAFE_EXPRESSION", CjUnsafeExpression::class.java)
//        val SPAWN_EXPRESSION: IElementType = CjNodeType("SPAWN_EXPRESSION", CjSpawnExpression::class.java)
//
//        val SYNCHRONIZED_EXPRESSION: IElementType =
//            CjNodeType("SYNCHRONIZED_EXPRESSION", CjSynchronizedExpression::class.java)
//        val CALL_EXPRESSION: IElementType = CjNodeType("CALL_EXPRESSION", CjCallExpression::class.java)
//        val QUOTE_EXPRESSION: IElementType = CjNodeType("QUOTE_EXPRESSION", CjQuoteExpression::class.java)
//
//        val INDICES: IElementType = CjNodeType("INDICES", CjContainerNode::class.java)
//        val ARRAY_ACCESS_EXPRESSION: IElementType =
//            CjNodeType("ARRAY_ACCESS_EXPRESSION", CjArrayAccessExpression::class.java)
//        val IS_EXPRESSION: IElementType = CjNodeType("IS_EXPRESSION", CjIsExpression::class.java)
//        val BINARY_WITH_TYPE: IElementType = CjNodeType("BINARY_WITH_TYPE", CjBinaryExpressionWithTypeRHS::class.java)
//        val BINARY_EXPRESSION: IElementType = CjNodeType("BINARY_EXPRESSION", CjBinaryExpression::class.java)
//        val PREFIX_EXPRESSION: IElementType = CjNodeType("PREFIX_EXPRESSION", CjPrefixExpression::class.java)
//        val OPERATION_REFERENCE: IElementType =
//            CjNodeType("OPERATION_REFERENCE", CjOperationReferenceExpression::class.java)
//
//
//        val OPERATION_NAME: IElementType = CjNodeType("OPERATION_NAME", CjOperationName::class.java)
//
//        val CONSTRUCTOR_DELEGATION_CALL: IElementType =
//            CjNodeType.CjLeftBoundNodeType("CONSTRUCTOR_DELEGATION_CALL", CjConstructorDelegationCall::class.java)
//
//        val CONSTRUCTOR_DELEGATION_REFERENCE: IElementType = CjNodeType.CjLeftBoundNodeType(
//            "CONSTRUCTOR_DELEGATION_REFERENCE",
//            CjConstructorDelegationReferenceExpression::class.java
//        )
//
//
//        val VARIABLE: IElementType = CjStubElementTypes.VARIABLE
//
//        val MAIN_FUNC: IElementType = CjStubElementTypes.MAIN_FUNC
//
//        val MACRO: IElementType = CjStubElementTypes.MACRO
//
//        val FOREIGN: IElementType = CjStubElementTypes.FOREIGN
//
//        val LAMBDA_ARGUMENT: IElementType = CjStubElementTypes.LAMBDA_ARGUMENT
//
//
//        val FOREIGN_BODY: IElementType = CjStubElementTypes.FOREIGN_BODY
//
//        val FUNC: IElementType = CjStubElementTypes.FUNCTION
//
//        val FUNC_EXTEND: IElementType = CjStubElementTypes.FUNCTION_EXTEND
//
//        val CJ_SCRIPT: IElementType = CjStubElementTypes.CJ_SCRIPT
//
//
//        val CJ_FILE: IFileElementType = IFileElementType(CangJieLanguage)
//
//        val BLOCK: IElementType = BlockExpressionElementType()
//        val CASE_BLOCK: IElementType = CaseBlockExpressionElementType()
//
//        val INIT_BLOCK: IElementType = InitBlockExpressionElementType()
//
//        val LAMBDA_EXPRESSION: IElementType = LambdaExpressionElementType()
//
//        val LABEL: IElementType = CjNodeType("LABEL", CjLabelReferenceExpression::class.java)
//
//        val CLASS_BODY: IElementType = CjStubElementTypes.CLASS_BODY
//        val INTERFACE_BODY: IElementType = CjStubElementTypes.INTERFACE_BODY
//
//        val TUPLE_EXPRESSION: IElementType = CjNodeType("TUPLE_EXPRESSION", CjTupleExpression::class.java)
//        val QUOTE_INTERPOLATE: IElementType = CjNodeType("QUOTE_INTERPOLATE", CjQuoteInterpolate::class.java)
//
//        val FUNCTION_LITERAL: IElementType = CjNodeType("FUNCTION_LITERAL", CjFunctionLiteral::class.java)
//        val LABEL_QUALIFIER: IElementType = CjNodeType("LABEL_QUALIFIER", CjContainerNode::class.java)
//
//        val PACKAGE_DIRECTIVE: IElementType = CjStubElementTypes.PACKAGE_DIRECTIVE
//
//        val SAFE_ACCESS_EXPRESSION: IElementType =
//            CjNodeType("SAFE_ACCESS_EXPRESSION", CjSafeQualifiedExpression::class.java)
//
//
//        val SUPER_TYPE_ENTRY: IElementType = CjStubElementTypes.SUPER_TYPE_ENTRY
//
//        val MODIFIER_LIST: IElementType = CjStubElementTypes.MODIFIER_LIST
//
//        val DESTRUCTURING_DECLARATION_ENTRY: IElementType =
//            CjNodeType("DESTRUCTURING_DECLARATION_ENTRY", CjDestructuringDeclarationEntry::class.java)
//        val DESTRUCTURING_DECLARATION: IElementType =
//            CjNodeType("DESTRUCTURING_DECLARATION", CjDestructuringDeclaration::class.java)
//
//        //    IElementType CLASS_INITIALIZER = CjStubElementTypes.CLASS_INITIALIZER;
//        val CLASS_INIT: IElementType = CjStubElementTypes.CLASS_INIT
//        val CLASS_TILDE_INIT: IElementType = CjStubElementTypes.CLASS_TILDE_INIT
//        val CLASS_MAIN_INIT: IElementType = CjStubElementTypes.CLASS_MAIN_INIT
//
//        val REFERENCE_EXPRESSION: IElementType = CjStubElementTypes.REFERENCE_EXPRESSION
//
//        val TYPE_PARAMETER_LIST: IElementType = CjStubElementTypes.TYPE_PARAMETER_LIST
//
//        val TYPE_CONSTRAINT_LIST: IElementType = CjStubElementTypes.TYPE_CONSTRAINT_LIST
//
//        val USER_TYPE: IElementType = CjStubElementTypes.USER_TYPE
//
//        val FUNCTION_TYPE: IElementType = CjStubElementTypes.FUNCTION_TYPE
//
//
//        val PARENTHESIZED_TYPE: IElementType = CjStubElementTypes.PARENTHESIZED_TYPE
//
//        val TUPLE_TYPE: IElementType = CjStubElementTypes.TUPLE_TYPE
//
//        val BASIC_TYPE: IElementType = CjStubElementTypes.BASIC_TYPE
//
//        val THIS_TYPE: IElementType = CjStubElementTypes.THIS_TYPE
//
//        val VARRAY_TYPE: IElementType = CjStubElementTypes.VARRAY_TYPE
//
//        //    IElementType BASIC_TYPE = new CjNodeType("BASIC_TYPE", CjBasicType.class);
//
//        val TYPE_PARAMETER: IElementType = CjStubElementTypes.TYPE_PARAMETER
//
//
//        val ENUM: IElementType = CjStubElementTypes.ENUM
//
//
//        val EXTEND: IElementType = CjStubElementTypes.EXTEND
//
//        val ENUM_BODY: IElementType = CjStubElementTypes.ENUM_BODY
//
//        val ENUM_ENTRY: IElementType = CjStubElementTypes.ENUM_ENTRY
//
//
//        val TYPE_LIST: IElementType = CjStubElementTypes.TYPE_LIST
//
//        val SUPER_TYPE_LIST: IElementType = CjStubElementTypes.SUPER_TYPE_LIST
//
//        val TYPE_CONSTRAINT: IElementType = CjStubElementTypes.TYPE_CONSTRAINT
//
//        val IMPORT_LIST: IElementType = CjStubElementTypes.IMPORT_LIST
//
//        val DOT_QUALIFIED_EXPRESSION: IElementType = CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
//
//
//        val IMPORT_ALIAS: IElementType = CjStubElementTypes.IMPORT_ALIAS
//        val MULIT_IMPORT_DIRECTIVE: IElementType =
//            CjNodeType("MULIT_IMPORT_DIRECTIVE", CjMultiImportDirective::class.java)
//
//        val IMPORT_DIRECTIVE_ITEM: IElementType = CjStubElementTypes.IMPORT_DIRECTIVE_ITEM
//
//
//        val IMPORT_DIRECTIVE: IElementType = CjStubElementTypes.IMPORT_DIRECTIVE
//
//        //    IElementType IMPORT_DIRECTIVE_ITEM = CjStubElementTypes.IMPORT_DIRECTIVE_ITEM;
//
//        val ANNOTATION_ENTRY: IElementType = CjStubElementTypes.ANNOTATION_ENTRY
//
//        val MACRO_EXPRESSION: IElementType = CjStubElementTypes.MACRO_EXPRESSION
//        val MACRO_INPUT: IElementType = CjNodeType("MACRO_INPUT", CjMacroInput::class.java)
//        val MACRO_ATTR: IElementType = CjNodeType("MACRO_ATTR", CjMacroAttr::class.java)
//        val QUOTE_TOKENS: IElementType = CjNodeType("QUOTE_TOKENS", CjQuoteTokens::class.java)
//        val QUOTE_PARAMETERS: IElementType = CjNodeType("QUOTE_PARAMETERS", CjQuoteParameters::class.java)
//
//        val LONG_STRING_TEMPLATE_ENTRY: IElementType = CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY
//        val SHORT_STRING_TEMPLATE_ENTRY: IElementType = CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY
//        val FLOAT_CONSTANT: IElementType = CjStubElementTypes.FLOAT_CONSTANT
//        val RUNE_CONSTANT: IElementType = CjStubElementTypes.RUNE_CONSTANT
//        val CHARACTER_BYTE_CONSTANT: IElementType = CjStubElementTypes.CHARACTER_BYTE_CONSTANT
//        val INTEGER_CONSTANT: IElementType = CjStubElementTypes.INTEGER_CONSTANT
//
//        val BOOLEAN_CONSTANT: IElementType = CjStubElementTypes.BOOLEAN_CONSTANT
//
//
//        val UNIT_CONSTANT: IElementType = CjStubElementTypes.UNIT_CONSTANT
//        val STRING_TEMPLATE: IElementType = CjStubElementTypes.STRING_TEMPLATE
//
//        val TYPE_PROJECTION: IElementType = CjStubElementTypes.TYPE_PROJECTION
//
//        val TYPE_ARGUMENT_LIST: IElementType = CjStubElementTypes.TYPE_ARGUMENT_LIST
//
//        //    IElementType MACRO_ARGUMENT_LIST                 = CjStubElementTypes.MACRO_ARGUMENT_LIST;
//        val LITERAL_STRING_TEMPLATE_ENTRY: IElementType = CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY
//        val ESCAPE_STRING_TEMPLATE_ENTRY: IElementType = CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY
//        val SUPER_EXPRESSION: IElementType = CjNodeType("SUPER_EXPRESSION", CjSuperExpression::class.java)
//        val THIS_EXPRESSION: IElementType = CjNodeType("THIS_EXPRESSION", CjThisExpression::class.java)
//        val COLLECTION_LITERAL_EXPRESSION: IElementType = CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION
//
//
//        val PARENTHESIZED: IElementType = CjNodeType("PARENTHESIZED", CjParenthesizedExpression::class.java)
//
//        val CONTINUE: IElementType = CjNodeType("CONTINUE", CjContinueExpression::class.java)
//
//        val BREAK: IElementType = CjNodeType("BREAK", CjBreakExpression::class.java)
//
//        val RETURN: IElementType = CjNodeType("RETURN", CjReturnExpression::class.java)
//
//        val THROW: IElementType = CjNodeType("THROW", CjThrowExpression::class.java)
//        val THEN: IElementType = CjNodeType("THEN", CjContainerNodeForControlStructureBody::class.java)
//
//        val ELSE: IElementType = CjNodeType("ELSE", CjContainerNodeForControlStructureBody::class.java)
//
//        val IF: IElementType = CjNodeType("IF", CjIfExpression::class.java)
//        val CONDITION: IElementType = CjNodeType("CONDITION", CjContainerNode::class.java)
//
//        val LET_EXPRESSION: IElementType = CjNodeType("LET_EXPRESSION", CjLetExpression::class.java)
//        val TRY: IElementType = CjNodeType("TRY", CjTryExpression::class.java)
//        val TRY_RESOURCE: IElementType = CjNodeType("TRY_RESOURCE", CjTryResource::class.java)
//        val TRY_RESOURCE_LIST: IElementType = CjNodeType("TRY_RESOURCE_LIST", CjTryResourceList::class.java)
//
//        val CATCH: IElementType = CjNodeType("CATCH", CjCatchClause::class.java)
//        val FINALLY: IElementType = CjNodeType("FINALLY", CjFinallySection::class.java)
//
//
//        val WHILE: IElementType = CjNodeType("WHILE", CjWhileExpression::class.java)
//        val BODY: IElementType = CjNodeType("BODY", CjContainerNodeForControlStructureBody::class.java)
//
//        val DO_WHILE: IElementType = CjNodeType("DO_WHILE", CjDoWhileExpression::class.java)
//        val PATTERN_GUARD: IElementType = CjNodeType("PATTERN_GUARD", CjPatternGuard::class.java)
//        val LOOP_RANGE: IElementType = CjNodeType("LOOP_RANGE", CjContainerNode::class.java)
//        val FOR: IElementType = CjNodeType("FOR", CjForExpression::class.java)
//
//        val MATCH: IElementType = CjNodeType("MATCH", CjMatchExpression::class.java)
//        val MATCH_ENTRY: IElementType = CjNodeType("MATCH_ENTRY", CjMatchEntry::class.java)
//
//
//        //    IElementType CASE_PATTERN = new CjNodeType("CASE_PATTERN", CjCasePattern.class);
//        //    IElementType CHARACTER_CONSTANT = CjStubElementTypes.CHARACTER_CONSTANT;
//        val WILDCARD_PATTERN: IElementType = CjNodeType("WILDCARD_PATTERN", CjWildcardPattern::class.java)
//        val TYPE_PATTERN: IElementType = CjNodeType("TYPE_PATTERN", CjTypePattern::class.java)
//        val ENUM_PATTERN: IElementType = CjNodeType("ENUM_PATTERN", CjEnumPattern::class.java)
//        val BINDING_PATTERN: IElementType = CjNodeType("BINDING_PATTERN", CjBindingPattern::class.java)
//        val TUPLE_PATTERN: IElementType = CjNodeType("TUPLE_PATTERN", CjTuplePattern::class.java)
//        val CONSTANT_PATTERN: IElementType = CjNodeType("CONSTANT_PATTERN", CjConstantPattern::class.java)
//
//
//        val PROPERTY_ACCESSOR: IElementType = CjStubElementTypes.PROPERTY_ACCESSOR
//        val ERROR_ELEMENT: IElementType = CjNodeType("ERROR_ELEMENT", CjErrorElement::class.java)
//        val END_SECONDARY_CONSTRUCTOR: IElementType = CjStubElementTypes.END_SECONDARY_CONSTRUCTOR
//
//        val SECONDARY_CONSTRUCTOR: IElementType = CjStubElementTypes.SECONDARY_CONSTRUCTOR
//
//        val PRIMARY_CONSTRUCTOR: IElementType = CjStubElementTypes.PRIMARY_CONSTRUCTOR
//        val CONTEXT_RECEIVER_LIST: IElementType = CjStubElementTypes.CONTEXT_RECEIVER_LIST
//
//
//        //    IFileElementType TYPE_CODE_FRAGMENT = CjStubElementTypes.TYPE_CODE_FRAGMENT;
//        //    IFileElementType EXPRESSION_CODE_FRAGMENT = CjStubElementTypes.EXPRESSION_CODE_FRAGMENT;
//        //    IFileElementType BLOCK_CODE_FRAGMENT = CjStubElementTypes.BLOCK_CODE_FRAGMENT;
//
//        val TYPE_CODE_FRAGMENT: CjTypeCodeFragmentType = CjTypeCodeFragmentType()
//
//        val EXPRESSION_CODE_FRAGMENT: CjExpressionCodeFragmentType = CjExpressionCodeFragmentType()
//
//        val BLOCK_CODE_FRAGMENT: CjBlockCodeFragmentType = CjBlockCodeFragmentType()
//
//
//        val MATCH_CONDITION_EXPRESSION: IElementType =
//            CjNodeType("MATCH_CONDITION_WITH_EXPRESSION", CjMatchConditionWithExpression::class.java)
//
//        val CONSTANT_EXPRESSIONS_TYPES: TokenSet = TokenSet.create(
//            BOOLEAN_CONSTANT,
//            FLOAT_CONSTANT,
//            RUNE_CONSTANT,
//            INTEGER_CONSTANT,
//
//            REFERENCE_EXPRESSION,
//            DOT_QUALIFIED_EXPRESSION,
//
//            STRING_TEMPLATE,
//
//
//            COLLECTION_LITERAL_EXPRESSION
//        )
