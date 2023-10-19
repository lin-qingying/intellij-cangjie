package com.huawei.cangjie1;

import com.huawei.cangjie1.lang.CangJieLanguage;
import com.huawei.cangjie1.psi.CjContainerNode;
import com.huawei.cangjie1.psi.CjDestructuringDeclaration;
import com.huawei.cangjie1.psi.CjDestructuringDeclarationEntry;
import com.huawei.cangjie1.psi.CjLabelReferenceExpression;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;

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

    IElementType MODIFIER_LIST = CjStubElementTypes.MODIFIER_LIST;
    IElementType DESTRUCTURING_DECLARATION_ENTRY = new CjNodeType("DESTRUCTURING_DECLARATION_ENTRY", CjDestructuringDeclarationEntry.class);
    IElementType DESTRUCTURING_DECLARATION = new CjNodeType("DESTRUCTURING_DECLARATION", CjDestructuringDeclaration.class);
    IElementType CLASS_INITIALIZER = CjStubElementTypes.CLASS_INITIALIZER;


    IElementType USER_TYPE = CjStubElementTypes.USER_TYPE;
    IElementType BASIC_TYPE = CjStubElementTypes.BASIC_TYPE;

}
