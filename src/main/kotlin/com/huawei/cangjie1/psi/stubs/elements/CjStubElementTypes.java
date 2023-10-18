package com.huawei.cangjie1.psi.stubs.elements;

import com.huawei.cangjie1.psi.*;

public interface CjStubElementTypes {

    CjImportAliasElementType IMPORT_ALIAS = new CjImportAliasElementType("IMPORT_ALIAS");

    CjTypeProjectionElementType TYPE_PROJECTION = new CjTypeProjectionElementType("TYPE_PROJECTION");
    CjParameterElementType VALUE_PARAMETER = new CjParameterElementType("VALUE_PARAMETER");
    CjClassElementType CLASS = new CjClassElementType("CLASS");
    CjPlaceHolderStubElementType<CjImportList> IMPORT_LIST =
            new CjPlaceHolderStubElementType<>("IMPORT_LIST", CjImportList.class);
    CjPlaceHolderStubElementType<CjTypeArgumentList> TYPE_ARGUMENT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_ARGUMENT_LIST", CjTypeArgumentList.class);
    CjTypeParameterElementType TYPE_PARAMETER = new CjTypeParameterElementType("TYPE_PARAMETER");
    CjPrimaryConstructorElementType PRIMARY_CONSTRUCTOR =
            new CjPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR");

    CjFunctionElementType FUNCTION = new CjFunctionElementType("FUNC");
    CjContextReceiverElementType CONTEXT_RECEIVER = new CjContextReceiverElementType("CONTEXT_RECEIVER");

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


    CjPlaceHolderStubElementType<CjClassBody> CLASS_BODY =
            new CjPlaceHolderStubElementType<>("CLASS_BODY", CjClassBody.class);


    CjPlaceHolderStubElementType<CjTypeConstraintList> TYPE_CONSTRAINT_LIST =
            new CjPlaceHolderStubElementType<>("TYPE_CONSTRAINT_LIST", CjTypeConstraintList.class);
    CjPlaceHolderStubElementType<CjParameterList> VALUE_PARAMETER_LIST =
            new CjPlaceHolderStubElementType<>("VALUE_PARAMETER_LIST", CjParameterList.class);

    CjNameReferenceExpressionElementType REFERENCE_EXPRESSION = new CjNameReferenceExpressionElementType("REFERENCE_EXPRESSION");
    CjClassElementType ENUM_ENTRY = new CjClassElementType("ENUM_ENTRY");
    CjSecondaryConstructorElementType SECONDARY_CONSTRUCTOR =
            new CjSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR");

    CjImportDirectiveElementType IMPORT_DIRECTIVE = new CjImportDirectiveElementType("IMPORT_DIRECTIVE");

    CjDotQualifiedExpressionElementType DOT_QUALIFIED_EXPRESSION = new CjDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION");

    CjMainFunctionElementType MAIN_FUNC = new CjMainFunctionElementType("MAIN_FUNC");
}
