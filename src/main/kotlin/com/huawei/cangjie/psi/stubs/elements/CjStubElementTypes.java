package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.psi.*;
import com.intellij.psi.tree.TokenSet;

public interface CjStubElementTypes {


    CjEnumEntryElementType ENUM_ENTRY = new CjEnumEntryElementType("ENUM_ENTRY");

    CjClassElementType CLASS = new CjClassElementType("CLASS");
    CjFunctionElementType FUNCTION = new CjFunctionElementType("FUNC");

    CjVariableElementType VARIABLE = new CjVariableElementType("VARIABLE");
    CjPropertyElementType PROPERTY = new CjPropertyElementType("PROPERTY");
    CjPropertyAccessorElementType PROPERTY_ACCESSOR = new CjPropertyAccessorElementType("PROPERTY_ACCESSOR");
//    CjBackingFieldElementType BACKING_FIELD = new CjBackingFieldElementType("BACKING_FIELD");
    CjTypeAliasElementType TYPEALIAS = new CjTypeAliasElementType("TYPEALIAS");

//    CjObjectElementType OBJECT_DECLARATION = new CjObjectElementType("OBJECT_DECLARATION");
    CjPlaceHolderStubElementType<CjClassInitializer> CLASS_INITIALIZER =
            new CjPlaceHolderStubElementType<>("CLASS_INITIALIZER", CjClassInitializer.class);
    CjSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new CjSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");
    CjPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new CjPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    CjParameterElementType VALUE_PARAMETER = new CjParameterElementType("VALUE_PARAMETER");
    CjPlaceHolderStubElementType<CjParameterList> VALUE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", CjParameterList.class);

    CjTypeParameterElementType TYPE_PARAMETER = new CjTypeParameterElementType("TYPE_PARAMETER");
    CjPlaceHolderStubElementType<CjTypeParameterList> TYPE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", CjTypeParameterList.class);

    CjAnnotationEntryElementType ANNOTATION_ENTRY = new CjAnnotationEntryElementType("ANNOTATION_ENTRY");
    CjPlaceHolderStubElementType<CjAnnotation> ANNOTATION =
            new CjPlaceHolderStubElementType<>("ANNOTATION", CjAnnotation.class);

//    CjAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new CjAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");

    CjPlaceHolderStubElementType<CjClassBody> CLASS_BODY =
            new CjPlaceHolderStubElementType<>("CLASS_BODY", CjClassBody.class);

    CjPlaceHolderStubElementType<CjImportList> IMPORT_LIST =
            new CjPlaceHolderStubElementType<>("IMPORT_LIST", CjImportList.class);

//    CjPlaceHolderStubElementType<CjFileAnnotationList> FILE_ANNOTATION_LIST =
//            new CjPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", CjFileAnnotationList.class);

    CjImportDirectiveElementType IMPORT_DIRECTIVE = new CjImportDirectiveElementType("IMPORT_DIRECTIVE");

    CjImportAliasElementType IMPORT_ALIAS = new CjImportAliasElementType("IMPORT_ALIAS");

//    CjPlaceHolderStubElementType<CjPackageDirective> PACKAGE_DIRECTIVE =
//            new CjPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", CjPackageDirective.class);
CjPackageDirectiveElementType  PACKAGE_DIRECTIVE =
        new CjPackageDirectiveElementType("PACKAGE_DIRECTIVE" );

    CjModifierListElementType<CjDeclarationModifierList> MODIFIER_LIST =
            new CjModifierListElementType<>("MODIFIER_LIST", CjDeclarationModifierList.class);

    CjPlaceHolderStubElementType<CjTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", CjTypeConstraintList.class);

    CjPlaceHolderStubElementType<CjTypeConstraint> TYPE_CONSTRAINT =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT", CjTypeConstraint.class);

//    CjPlaceHolderStubElementType<CjNullableType> NULLABLE_TYPE =
//            new CjPlaceHolderStubElementType<>("NULLABLE_TYPE", CjNullableType.class);

//    CjPlaceHolderStubElementType<CjIntersectionType> INTERSECTION_TYPE =
//            new CjPlaceHolderStubElementType<>("INTERSECTION_TYPE", CjIntersectionType.class);
CjPlaceHolderStubElementType<CjOptionType> OPTIONAL_TYPE =
        new CjPlaceHolderStubElementType<>("OPTIONAL_TYPE", CjOptionType.class);

    CjPlaceHolderStubElementType<CjTypeReference> TYPE_REFERENCE =
            new CjPlaceHolderStubElementType<>("TYPE_REFERENCE", CjTypeReference.class);
    CjBasicTypeElementType BASIC_TYPE = new CjBasicTypeElementType("BASIC_TYPE");

    CjUserTypeElementType USER_TYPE = new CjUserTypeElementType("USER_TYPE");
//    CjPlaceHolderStubElementType<CjDynamicType> DYNAMIC_TYPE =
//            new CjPlaceHolderStubElementType<>("DYNAMIC_TYPE", CjDynamicType.class);

    CjPlaceHolderStubElementType<CjFunctionType> FUNCTION_TYPE =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE", CjFunctionType.class);


    CjTypeProjectionElementType TYPE_PROJECTION = new CjTypeProjectionElementType("TYPE_PROJECTION");

    CjPlaceHolderStubElementType<CjFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", CjFunctionTypeReceiver.class);

    CjNameReferenceExpressionElementType REFERENCE_EXPRESSION = new CjNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    CjDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new CjDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");
//    CjEnumEntrySuperClassReferenceExpressionElementType
//            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
//            new CjEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
    CjPlaceHolderStubElementType<CjTypeArgumentList> TYPE_ARGUMENT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", CjTypeArgumentList.class);

    CjPlaceHolderStubElementType<CjValueArgumentList> VALUE_ARGUMENT_LIST =
            new CjValueArgumentListElementType("VALUE_ARGUMENT_LIST");

    CjValueArgumentElementType<CjValueArgument> VALUE_ARGUMENT =
            new CjValueArgumentElementType<>("VALUE_ARGUMENT", CjValueArgument.class);

//    CjPlaceHolderStubElementType<CjContractEffectList> CONTRACT_EFFECT_LIST =
//            new CjContractEffectListElementType("CONTRACT_EFFECT_LIST");

//    CjContractEffectElementType CONTRACT_EFFECT =
//            new CjContractEffectElementType("CONTRACT_EFFECT", CjContractEffect.class);

    CjValueArgumentElementType<CjLambdaArgument> LAMBDA_ARGUMENT =
            new CjValueArgumentElementType<>("LAMBDA_ARGUMENT", CjLambdaArgument.class);

    CjPlaceHolderStubElementType<CjValueArgumentName> VALUE_ARGUMENT_NAME =
            new CjPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", CjValueArgumentName.class);

    CjPlaceHolderStubElementType<CjSuperTypeList> SUPER_TYPE_LIST =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_LIST", CjSuperTypeList.class);

//    CjPlaceHolderStubElementType<CjInitializerList> INITIALIZER_LIST =
//            new CjPlaceHolderStubElementType<>("INITIALIZER_LIST", CjInitializerList.class);

//    CjPlaceHolderStubElementType<CjDelegatedSuperTypeEntry> DELEGATED_SUPER_TYPE_ENTRY =
//            new CjPlaceHolderStubElementType<>("DELEGATED_SUPER_TYPE_ENTRY", CjDelegatedSuperTypeEntry.class);

    CjPlaceHolderStubElementType<CjSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", CjSuperTypeCallEntry.class);
    CjPlaceHolderStubElementType<CjSuperTypeEntry> SUPER_TYPE_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", CjSuperTypeEntry.class);
    CjPlaceHolderStubElementType<CjConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new CjPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", CjConstructorCalleeExpression.class);

    CjContextReceiverElementType CONTEXT_RECEIVER = new CjContextReceiverElementType("CONTEXT_RECEIVER");
    CjPlaceHolderStubElementType<CjContextReceiverList> CONTEXT_RECEIVER_LIST =
            new CjPlaceHolderStubElementType<>("CONTEXT_RECEIVER_LIST", CjContextReceiverList.class);

//    CjConstantExpressionElementType NULL                = new CjConstantExpressionElementType("NULL");
    CjConstantExpressionElementType BOOLEAN_CONSTANT    = new CjConstantExpressionElementType("BOOLEAN_CONSTANT");
    CjConstantExpressionElementType FLOAT_CONSTANT      = new CjConstantExpressionElementType("FLOAT_CONSTANT");
//    CjConstantExpressionElementType CHARACTER_CONSTANT  = new CjConstantExpressionElementType("CHARACTER_CONSTANT");
    CjConstantExpressionElementType INTEGER_CONSTANT    = new CjConstantExpressionElementType("INTEGER_CONSTANT");
//    CjClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new CjClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
CjConstantExpressionElementType RUNE_CONSTANT = new CjConstantExpressionElementType("RUNE_CONSTANT");


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





    CjConstantExpressionElementType UNIT_CONSTANT = new CjConstantExpressionElementType("UNIT_CONSTANT");
    CjAnnotationEntryElementType MACRO_EXPRESSION = new CjAnnotationEntryElementType("MACRO_EXPRESSION");


    //    CjPlaceHolderStubElementType<CjAnnotation> ANNOTATION =
//            new CjPlaceHolderStubElementType<>("ANNOTATION", CjAnnotation.class);

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


    CjPlaceHolderStubElementType<CjEnumBody> ENUM_BODY =
            new CjPlaceHolderStubElementType<>("ENUM_BODY", CjEnumBody.class);

    CjPlaceHolderStubElementType<CjPropertyBody> PROPERTY_BODY =
            new CjPlaceHolderStubElementType<>("PROPERTY_BODY", CjPropertyBody.class);
    CjPlaceHolderStubElementType<CjPropertyGet> PROPERTY_GET =
            new CjPlaceHolderStubElementType<>("PROPERTY_GET", CjPropertyGet.class);

    CjPlaceHolderStubElementType<CjPropertySet> PROPERTY_SET =
            new CjPlaceHolderStubElementType<>("PROPERTY_SET", CjPropertySet.class);


    CjPlaceHolderStubElementType<CjEnumEntryTypeEntry> TYPE_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_LIST", CjEnumEntryTypeEntry.class);


//    CjImportDirectiveItemElementType IMPORT_DIRECTIVE_ITEM = new CjImportDirectiveItemElementType("IMPORT_DIRECTIVE_ITEM");


    CjMainFunctionElementType MAIN_FUNC = new CjMainFunctionElementType("MAIN_FUNC");
    CjClassInitElementType CLASS_INIT = new CjClassInitElementType("CLASS_INIT");
    CjClassInitElementType CLASS_MAIN_INIT = new CjClassInitElementType("CLASS_MAIN_INIT");
    CjClassInitElementType CLASS_TILDE_INIT = new CjClassInitElementType("CLASS_TILDE_INIT");

//    CjBasicTypeElementType BASIC_TYPE = new CjBasicTypeElementType("BASIC_TYPE");










}
