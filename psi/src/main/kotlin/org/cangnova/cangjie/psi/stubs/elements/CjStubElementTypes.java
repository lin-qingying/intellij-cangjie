/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.stubs.elements;

import org.cangnova.cangjie.psi.*;
import org.jetbrains.annotations.NotNull;

public interface CjStubElementTypes {

    CjScriptElementType CJ_SCRIPT = new CjScriptElementType("CJ_SCRIPT");

    CjEnumConstructorElementType ENUM_CONSTRUCTOR = new CjEnumConstructorElementType("ENUM_CONSTRUCTOR");

    CjClassElementType CLASS = new CjClassElementType("CLASS");
    CjFunctionElementType FUNCTION = new CjFunctionElementType("FUNC");

    CjPatternVariableElementType VARIABLE = new CjPatternVariableElementType("VARIABLE");
    CjFieldElementType FIELD = new CjFieldElementType("FIELD");
    CjPropertyElementType PROPERTY = new CjPropertyElementType("PROPERTY");
    CjPropertyAccessorElementType PROPERTY_ACCESSOR = new CjPropertyAccessorElementType("PROPERTY_ACCESSOR");
    //    CjBackingFieldElementType BACKING_FIELD = new CjBackingFieldElementType("BACKING_FIELD");
    CjTypeAliasElementType TYPEALIAS = new CjTypeAliasElementType("TYPEALIAS");


    CjCatchParameterElementType CATCH_PARAMETER = new CjCatchParameterElementType("CATCH_PARAMETER");
    CjParameterElementType VALUE_PARAMETER = new CjParameterElementType("VALUE_PARAMETER");
    CjPlaceHolderStubElementType<CjParameterList> VALUE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", CjParameterList.class);

    CjTypeParameterElementType TYPE_PARAMETER = new CjTypeParameterElementType("TYPE_PARAMETER");
    CjPlaceHolderStubElementType<CjTypeParameterList> TYPE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", CjTypeParameterList.class);
    @NotNull
    CjAnnotationElementType ANNOTATION = new CjAnnotationElementType("ANNOTATION");
    CjPlaceHolderStubElementType<CjAnnotations> ANNOTATIONS =
            new CjPlaceHolderStubElementType<>("ANNOTATIONS", CjAnnotations.class);

    //    CjAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new CjAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");
    CjPlaceHolderStubElementType<CjEnumBody> ENUM_BODY =
            new CjPlaceHolderStubElementType<>("ENUM_BODY", CjEnumBody.class);
    CjPlaceHolderStubElementType<CjClassBody> CLASS_BODY =
            new CjPlaceHolderStubElementType<>("CLASS_BODY", CjClassBody.class);
    CjPlaceHolderStubElementType<CjInterfaceBody> INTERFACE_BODY =
            new CjPlaceHolderStubElementType<>("INTERFACE_BODY", CjInterfaceBody.class);

    CjPlaceHolderStubElementType<CjImportList> IMPORT_LIST =
            new CjPlaceHolderStubElementType<>("IMPORT_LIST", CjImportList.class);

    CjImportItemElementType IMPORT_ITEM = new CjImportItemElementType("IMPORT_ITEM");
    CjImportDirectiveElementType IMPORT_DIRECTIVE = new CjImportDirectiveElementType("IMPORT_DIRECTIVE");
    CjImportAliasElementType IMPORT_ALIAS = new CjImportAliasElementType("IMPORT_ALIAS");


    CjPackageDirectiveElementType PACKAGE_DIRECTIVE =
            new CjPackageDirectiveElementType("PACKAGE_DIRECTIVE");

    CjModifierListElementType<CjDeclarationModifierList> MODIFIER_LIST =
            new CjModifierListElementType<>("MODIFIER_LIST", CjDeclarationModifierList.class);

    CjPlaceHolderStubElementType<CjTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", CjTypeConstraintList.class);

    CjPlaceHolderStubElementType<CjTypeConstraint> TYPE_CONSTRAINT =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT", CjTypeConstraint.class);

    CjPlaceHolderStubElementType<CjOptionType> OPTIONAL_TYPE =
            new CjPlaceHolderStubElementType<>("OPTIONAL_TYPE", CjOptionType.class);

    CjPlaceHolderStubElementType<CjTypeReference> TYPE_REFERENCE =
            new CjPlaceHolderStubElementType<>("TYPE_REFERENCE", CjTypeReference.class);

    CjPlaceHolderStubElementType<CjVArrayType> VARRAY_TYPE =
            new CjPlaceHolderStubElementType<>("VARRAY_TYPE", CjVArrayType.class);
    CjBasicTypeElementType BASIC_TYPE = new CjBasicTypeElementType("BASIC_TYPE");

    CjPlaceHolderStubElementType<CjThisType> THIS_TYPE =
            new CjPlaceHolderStubElementType<>("THIS_TYPE", CjThisType.class);

    CjUserTypeElementType USER_TYPE = new CjUserTypeElementType("USER_TYPE");

    CjPlaceHolderStubElementType<CjFunctionType> FUNCTION_TYPE =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE", CjFunctionType.class);


    CjTypeProjectionElementType TYPE_PROJECTION = new CjTypeProjectionElementType("TYPE_PROJECTION");

    CjPlaceHolderStubElementType<CjFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", CjFunctionTypeReceiver.class);
    CjNameBasicReferenceExpressionElementType BASIC_REFERENCE_EXPRESSION = new CjNameBasicReferenceExpressionElementType("BASIC_REFERENCE_EXPRESSION");

    CjNameReferenceExpressionElementType REFERENCE_EXPRESSION = new CjNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    CjDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new CjDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
    CjPlaceHolderStubElementType<CjTypeArgumentList> TYPE_ARGUMENT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", CjTypeArgumentList.class);

    CjPlaceHolderStubElementType<CjValueArgumentList> VALUE_ARGUMENT_LIST =
            new CjValueArgumentListElementType("VALUE_ARGUMENT_LIST");

    CjValueArgumentElementType<CjValueArgument> VALUE_ARGUMENT =
            new CjValueArgumentElementType<>("VALUE_ARGUMENT", CjValueArgument.class);

    CjValueArgumentElementType<CjLambdaArgument> LAMBDA_ARGUMENT =
            new CjValueArgumentElementType<>("LAMBDA_ARGUMENT", CjLambdaArgument.class);

    CjPlaceHolderStubElementType<CjValueArgumentName> VALUE_ARGUMENT_NAME =
            new CjPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", CjValueArgumentName.class);

    CjPlaceHolderStubElementType<CjSuperTypeList> SUPER_TYPE_LIST =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_LIST", CjSuperTypeList.class);


    CjPlaceHolderStubElementType<CjSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", CjSuperTypeCallEntry.class);
    CjPlaceHolderStubElementType<CjSuperTypeEntry> SUPER_TYPE_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", CjSuperTypeEntry.class);
    CjPlaceHolderStubElementType<CjConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new CjPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", CjConstructorCalleeExpression.class);

    CjContextReceiverElementType CONTEXT_RECEIVER = new CjContextReceiverElementType("CONTEXT_RECEIVER");
    CjConstantExpressionElementType BOOLEAN_CONSTANT = new CjConstantExpressionElementType("BOOLEAN_CONSTANT");
    CjConstantExpressionElementType FLOAT_CONSTANT = new CjConstantExpressionElementType("FLOAT_CONSTANT");
    CjConstantExpressionElementType INTEGER_CONSTANT = new CjConstantExpressionElementType("INTEGER_CONSTANT");
    CjConstantExpressionElementType RUNE_CONSTANT = new CjConstantExpressionElementType("RUNE_CONSTANT");
    CjConstantExpressionElementType UNIT_CONSTANT = new CjConstantExpressionElementType("UNIT_CONSTANT");


    CjCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION = new CjCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");

    CjPlaceHolderStubElementType<CjStringTemplateExpression> STRING_TEMPLATE =
            new CjStringTemplateExpressionElementType("STRING_TEMPLATE");

    CjPlaceHolderWithTextStubElementType<CjBlockStringTemplateEntry> LONG_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("LONG_STRING_TEMPLATE_ENTRY", CjBlockStringTemplateEntry.class);

    CjPlaceHolderWithTextStubElementType<CjSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", CjSimpleNameStringTemplateEntry.class);

    CjPlaceHolderWithTextStubElementType<CjLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", CjLiteralStringTemplateEntry.class);

    CjPlaceHolderWithTextStubElementType<CjEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", CjEscapeStringTemplateEntry.class);


    CjMacroExpressionElementType MACRO_EXPRESSION = new CjMacroExpressionElementType("MACRO_EXPRESSION");


    CjConstantExpressionElementType CHARACTER_BYTE_CONSTANT = new CjConstantExpressionElementType("CHARACTER_BYTE_CONSTANT");


    CjInterfaceElementType INTERFACE = new CjInterfaceElementType("INTERFACE");

    CjStructElementType STRUCT = new CjStructElementType("STRUCT");


    CjMacroElementType MACRO = new CjMacroElementType("MACRO");
    CjForeignDirectiveElementType FOREIGN = new CjForeignDirectiveElementType("FOREIGN");
    CjPlaceHolderStubElementType<CjForeignBody> FOREIGN_BODY =
            new CjPlaceHolderStubElementType<>("FOREIGN_BODY", CjForeignBody.class);


    CjEnumElementType ENUM = new CjEnumElementType("ENUM");
    CjExtendElementType EXTEND = new CjExtendElementType("EXTEND");

    //    CjTupleTypeElementType TUPLE_TYPE = new CjTupleTypeElementType("TUPLE_TYPE");
    CjPlaceHolderStubElementType<CjTupleType> TUPLE_TYPE = new CjPlaceHolderStubElementType<>("TUPLE_TYPE", CjTupleType.class);
    CjPlaceHolderStubElementType<CjParenthesizedType> PARENTHESIZED_TYPE = new CjPlaceHolderStubElementType<>("PARENTHESIZED_TYPE", CjParenthesizedType.class);


    CjPlaceHolderStubElementType<CjPropertyBody> PROPERTY_BODY =
            new CjPlaceHolderStubElementType<>("PROPERTY_BODY", CjPropertyBody.class);
    CjPlaceHolderStubElementType<CjPropertyGet> PROPERTY_GET =
            new CjPlaceHolderStubElementType<>("PROPERTY_GET", CjPropertyGet.class);

    CjPlaceHolderStubElementType<CjPropertySet> PROPERTY_SET =
            new CjPlaceHolderStubElementType<>("PROPERTY_SET", CjPropertySet.class);


    CjPlaceHolderStubElementType<CjEnumConstructorTypeEntry> TYPE_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_LIST", CjEnumConstructorTypeEntry.class);


//    CjImportDirectiveItemElementType IMPORT_DIRECTIVE_ITEM = new CjImportDirectiveItemElementType("IMPORT_DIRECTIVE_ITEM");


    CjMainFunctionElementType MAIN_FUNC = new CjMainFunctionElementType("MAIN_FUNC");

    CjEndSecondaryConstructorElementType END_SECONDARY_CONSTRUCTOR =
            new CjEndSecondaryConstructorElementType("END_SECONDARY_CONSTRUCTOR");
    CjSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new CjSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    CjPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new CjPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    // 模式 Stub ElementTypes
    CjBindingPatternElementType BINDING_PATTERN = new CjBindingPatternElementType("BINDING_PATTERN");
    CjTuplePatternElementType TUPLE_PATTERN = new CjTuplePatternElementType("TUPLE_PATTERN");
    CjEnumPatternElementType ENUM_PATTERN = new CjEnumPatternElementType("ENUM_PATTERN");
    CjWildcardPatternElementType WILDCARD_PATTERN = new CjWildcardPatternElementType("WILDCARD_PATTERN");
    CjTypePatternElementType TYPE_PATTERN = new CjTypePatternElementType("TYPE_PATTERN");
    CjConstantPatternElementType CONSTANT_PATTERN = new CjConstantPatternElementType("CONSTANT_PATTERN");
    CjMatchConditionElementType MATCH_CONDITION = new CjMatchConditionElementType("MATCH_CONDITION");

}
