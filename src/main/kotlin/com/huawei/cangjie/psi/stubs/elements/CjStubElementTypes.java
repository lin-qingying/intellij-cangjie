package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.psi.*;
import com.intellij.psi.tree.TokenSet;

public interface CjStubElementTypes {
    CjPropertyAccessorElementType PROPERTY_ACCESSOR = new CjPropertyAccessorElementType("PROPERTY_ACCESSOR");
    CjPlaceHolderStubElementType<CjFunctionType> FUNCTION_TYPE =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE", CjFunctionType.class);
    CjConstantExpressionElementType BOOLEAN_CONSTANT = new CjConstantExpressionElementType("BOOLEAN_CONSTANT");

    CjPlaceHolderStubElementType<CjFunctionTypeReceiver> FUNCTION_TYPE_RECEIVER =
            new CjPlaceHolderStubElementType<>("FUNCTION_TYPE_RECEIVER", CjFunctionTypeReceiver.class);

    CjConstantExpressionElementType UNIT_CONSTANT = new CjConstantExpressionElementType("UNIT_CONSTANT");

    CjConstantExpressionElementType FLOAT_CONSTANT = new CjConstantExpressionElementType("FLOAT_CONSTANT");
    CjConstantExpressionElementType CHARACTER_CONSTANT = new CjConstantExpressionElementType("CHARACTER_CONSTANT");
    CjConstantExpressionElementType INTEGER_CONSTANT = new CjConstantExpressionElementType("INTEGER_CONSTANT");

    CjPlaceHolderWithTextStubElementType<CjSimpleNameStringTemplateEntry> SHORT_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("SHORT_STRING_TEMPLATE_ENTRY", CjSimpleNameStringTemplateEntry.class);

    CjPlaceHolderWithTextStubElementType<CjBlockStringTemplateEntry> LONG_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("LONG_STRING_TEMPLATE_ENTRY", CjBlockStringTemplateEntry.class);


    CjPlaceHolderStubElementType<CjValueArgumentName> VALUE_ARGUMENT_NAME =
            new CjPlaceHolderStubElementType<>("VALUE_ARGUMENT_NAME", CjValueArgumentName.class);
    CjPlaceHolderStubElementType<CjValueArgumentList> VALUE_ARGUMENT_LIST =
            new CjValueArgumentListElementType("VALUE_ARGUMENT_LIST");
    CjPlaceHolderStubElementType<CjConstructorCalleeExpression> CONSTRUCTOR_CALLEE =
            new CjPlaceHolderStubElementType<>("CONSTRUCTOR_CALLEE", CjConstructorCalleeExpression.class);

    CjPlaceHolderStubElementType<CjSuperTypeCallEntry> SUPER_TYPE_CALL_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_CALL_ENTRY", CjSuperTypeCallEntry.class);


    CjPlaceHolderStubElementType<CjSuperTypeEntry> SUPER_TYPE_ENTRY =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_ENTRY", CjSuperTypeEntry.class);

    CjVariableElementType VARIABLE = new CjVariableElementType("VARIABLE");
    CjPropertyElementType PROPERTY = new CjPropertyElementType("PROPERTY");

    CjPlaceHolderStubElementType<CjClassInitializer> CLASS_INITIALIZER =
            new CjPlaceHolderStubElementType<>("CLASS_INITIALIZER", CjClassInitializer.class);
    CjImportAliasElementType IMPORT_ALIAS = new CjImportAliasElementType("IMPORT_ALIAS");
    CjPlaceHolderStubElementType<CjSuperTypeList> SUPER_TYPE_LIST =
            new CjPlaceHolderStubElementType<>("SUPER_TYPE_LIST", CjSuperTypeList.class);

    CjTypeProjectionElementType TYPE_PROJECTION = new CjTypeProjectionElementType("TYPE_PROJECTION");
    CjParameterElementType VALUE_PARAMETER = new CjParameterElementType("VALUE_PARAMETER");
    CjClassElementType CLASS = new CjClassElementType("CLASS");

    CjInterfaceElementType INTERFACE = new CjInterfaceElementType("INTERFACE");

    CjStructElementType STRUCT = new CjStructElementType("STRUCT");
    CjPlaceHolderStubElementType<CjImportList> IMPORT_LIST =
            new CjPlaceHolderStubElementType<>("IMPORT_LIST", CjImportList.class);
    CjPlaceHolderStubElementType<CjTypeArgumentList> TYPE_ARGUMENT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", CjTypeArgumentList.class);
    CjTypeParameterElementType TYPE_PARAMETER = new CjTypeParameterElementType("TYPE_PARAMETER");
    CjPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new CjPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");
    CjMacroElementType MACRO = new CjMacroElementType("MACRO");
    CjForeignDirectiveElementType FOREIGN = new CjForeignDirectiveElementType("FOREIGN");
    CjPlaceHolderStubElementType<CjForeignBody> FOREIGN_BODY =
            new CjPlaceHolderStubElementType<>("FOREIGN_BODY", CjForeignBody.class);
    CjFunctionElementType FUNCTION = new CjFunctionElementType("FUNC");
    CjContextReceiverElementType CONTEXT_RECEIVER = new CjContextReceiverElementType("CONTEXT_RECEIVER");


    CjEnumElementType ENUM = new CjEnumElementType("ENUM");
    CjExtendElementType EXTEND = new CjExtendElementType("EXTEND");
    CjPlaceHolderStubElementType<CjContextReceiverList> CONTEXT_RECEIVER_LIST =
            new CjPlaceHolderStubElementType<>("CONTEXT_RECEIVER_LIST", CjContextReceiverList.class);
    CjPlaceHolderStubElementType<CjTypeParameterList> TYPE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_PARAMETER_LIST", CjTypeParameterList.class);
    CjPlaceHolderStubElementType<CjTypeConstraint> TYPE_CONSTRAINT =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT", CjTypeConstraint.class);

    CjPlaceHolderStubElementType<CjTypeReference> TYPE_REFERENCE =
            new CjPlaceHolderStubElementType<>("TYPE_REFERENCE", CjTypeReference.class);
    CjPlaceHolderStubElementType<CjPackageDirective> PACKAGE_DIRECTIVE = new CjPlaceHolderStubElementType<>("PACKAGE_DIRECTIVE", CjPackageDirective.class);

    CjModifierListElementType<CjDeclarationModifierList> MODIFIER_LIST =
            new CjModifierListElementType<>("MODIFIER_LIST", CjDeclarationModifierList.class);

    CjValueArgumentElementType<CjValueArgument> VALUE_ARGUMENT =
            new CjValueArgumentElementType<>("VALUE_ARGUMENT", CjValueArgument.class);

    CjUserTypeElementType USER_TYPE = new CjUserTypeElementType("USER_TYPE");

    CjTupleTypeElementType TUPLE_TYPE = new CjTupleTypeElementType("TUPLE_TYPE");

    CjPlaceHolderStubElementType<CjClassBody> CLASS_BODY =
            new CjPlaceHolderStubElementType<>("CLASS_BODY", CjClassBody.class);
    CjPlaceHolderStubElementType<CjEnumBody> ENUM_BODY =
            new CjPlaceHolderStubElementType<>("ENUM_BODY", CjEnumBody.class);

    CjPlaceHolderStubElementType<CjPropertyBody> PROPERTY_BODY =
            new CjPlaceHolderStubElementType<>("PROPERTY_BODY", CjPropertyBody.class);
    CjPlaceHolderStubElementType<CjPropertyGet> PROPERTY_GET =
            new CjPlaceHolderStubElementType<>("PROPERTY_GET", CjPropertyGet.class);

    CjPlaceHolderStubElementType<CjPropertySet> PROPERTY_SET =
            new CjPlaceHolderStubElementType<>("PROPERTY_SET", CjPropertySet.class);
    CjPlaceHolderStubElementType<CjTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", CjTypeConstraintList.class);
    CjPlaceHolderStubElementType<CjParameterList> VALUE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", CjParameterList.class);

    CjNameReferenceExpressionElementType REFERENCE_EXPRESSION = new CjNameReferenceExpressionElementType("REFERENCE_EXPRESSION");

    CjPlaceHolderStubElementType<CjSuperTypeEntry> ENUM_ENTRY =
            new CjPlaceHolderStubElementType<>("ENUM_ENTRY", CjSuperTypeEntry.class);
    CjPlaceHolderStubElementType<CjSuperTypeEntry> TYPE_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_LIST", CjSuperTypeEntry.class);
    CjSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new CjSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");

    CjImportDirectiveElementType IMPORT_DIRECTIVE = new CjImportDirectiveElementType("IMPORT_DIRECTIVE");
    CjImportDirectiveItemElementType IMPORT_DIRECTIVE_ITEM = new CjImportDirectiveItemElementType("IMPORT_DIRECTIVE_ITEM");

    CjDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new CjDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");

    CjMainFunctionElementType MAIN_FUNC = new CjMainFunctionElementType("MAIN_FUNC");
    CJClassInitElementType CLASS_INIT = new CJClassInitElementType("CLASS_INIT");
    CJClassInitElementType CLASS_MAIN_INIT = new CJClassInitElementType("CLASS_MAIN_INIT");
    CJClassInitElementType CLASS_TILDE_INIT = new CJClassInitElementType("CLASS_TILDE_INIT");

    CjBasicTypeElementType BASIC_TYPE = new CjBasicTypeElementType("BASIC_TYPE");
    CjPlaceHolderStubElementType<CjStringTemplateExpression> STRING_TEMPLATE =
            new CjStringTemplateExpressionElementType("STRING_TEMPLATE");

    CjPlaceHolderWithTextStubElementType<CjEscapeStringTemplateEntry> ESCAPE_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("ESCAPE_STRING_TEMPLATE_ENTRY", CjEscapeStringTemplateEntry.class);
    CjPlaceHolderWithTextStubElementType<CjLiteralStringTemplateEntry> LITERAL_STRING_TEMPLATE_ENTRY =
            new CjPlaceHolderWithTextStubElementType<>("LITERAL_STRING_TEMPLATE_ENTRY", CjLiteralStringTemplateEntry.class);


    CjCollectionLiteralExpressionElementType COLLECTION_LITERAL_EXPRESSION =
            new CjCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION");


    CjCollectionLiteralExpressionElementType TUPLE_LITERAL_EXPRESSION =
            new CjCollectionLiteralExpressionElementType("TUPLE_LITERAL_EXPRESSION");


    CjTypeCodeFragmentType TYPE_CODE_FRAGMENT = new CjTypeCodeFragmentType();

    CjExpressionCodeFragmentType EXPRESSION_CODE_FRAGMENT = new CjExpressionCodeFragmentType();
    CjBlockCodeFragmentType BLOCK_CODE_FRAGMENT = new CjBlockCodeFragmentType();


    TokenSet CONSTANT_EXPRESSIONS_TYPES = TokenSet.create(

            BOOLEAN_CONSTANT,
            FLOAT_CONSTANT,
            CHARACTER_CONSTANT,
            INTEGER_CONSTANT,

            REFERENCE_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,

            STRING_TEMPLATE,


            COLLECTION_LITERAL_EXPRESSION
    );

}
