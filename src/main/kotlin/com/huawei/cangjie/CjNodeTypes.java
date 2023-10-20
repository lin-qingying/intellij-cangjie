package com.huawei.cangjie;

import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.psi.CjContainerNode;
import com.huawei.cangjie.psi.CjDestructuringDeclaration;
import com.huawei.cangjie.psi.CjDestructuringDeclarationEntry;
import com.huawei.cangjie.psi.CjLabelReferenceExpression;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

public interface CjNodeTypes {
    IElementType TYPE_REFERENCE = CjStubElementTypes.TYPE_REFERENCE;
    IElementType VALUE_PARAMETER_LIST = CjStubElementTypes.VALUE_PARAMETER_LIST;
    IElementType VALUE_PARAMETER = CjStubElementTypes.VALUE_PARAMETER;
    IElementType CLASS = CjStubElementTypes.CLASS;
    IElementType PROPERTY = CjStubElementTypes.PROPERTY;
    IElementType MAIN_FUNC = CjStubElementTypes.MAIN_FUNC;
    IElementType FUNC = CjStubElementTypes.FUNCTION;
    IFileElementType CJ_FILE = new IFileElementType(CangJieLanguage.INSTANCE);
    IElementType BLOCK = new BlockExpressionElementType();
    IElementType LABEL = new CjNodeType("LABEL", CjLabelReferenceExpression.class);
    IElementType CLASS_BODY = CjStubElementTypes.CLASS_BODY;
    IElementType LABEL_QUALIFIER = new CjNodeType("LABEL_QUALIFIER", CjContainerNode.class);
    IElementType PACKAGE_DIRECTIVE = CjStubElementTypes.PACKAGE_DIRECTIVE;
    IElementType SUPER_TYPE_ENTRY                   = CjStubElementTypes.SUPER_TYPE_ENTRY;
    IElementType MODIFIER_LIST = CjStubElementTypes.MODIFIER_LIST;
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new CjNodeType("DESTRUCTURING_DECLARATION_ENTRY", CjDestructuringDeclarationEntry.class);
    IElementType DESTRUCTURING_DECLARATION = new CjNodeType("DESTRUCTURING_DECLARATION", CjDestructuringDeclaration.class);
    IElementType CLASS_INITIALIZER = CjStubElementTypes.CLASS_INITIALIZER;
    IElementType REFERENCE_EXPRESSION = CjStubElementTypes.REFERENCE_EXPRESSION;
    IElementType TYPE_PARAMETER_LIST = CjStubElementTypes.TYPE_PARAMETER_LIST;
    IElementType TYPE_CONSTRAINT_LIST = CjStubElementTypes.TYPE_CONSTRAINT_LIST;
    IElementType USER_TYPE = CjStubElementTypes.USER_TYPE;
    IElementType BASIC_TYPE = CjStubElementTypes.BASIC_TYPE;
    IElementType TYPE_PARAMETER = CjStubElementTypes.TYPE_PARAMETER;
    IElementType SUPER_TYPE_LIST                    = CjStubElementTypes.SUPER_TYPE_LIST;
    IElementType TYPE_CONSTRAINT = CjStubElementTypes.TYPE_CONSTRAINT;
}
