package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.stubs.CangJieAnnotationEntryStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjAnnotationEntry extends CjElementImplStub<CangJieAnnotationEntryStub> implements CjCallElement  {


    public CjAnnotationEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjAnnotationEntry(@NotNull CangJieAnnotationEntryStub stub) {
        super(stub, CjStubElementTypes.ANNOTATION_ENTRY);
    }
    @Override
    public CjConstructorCalleeExpression getCalleeExpression() {
        return getStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE);
    }
    @Override
    public CjValueArgumentList getValueArgumentList() {
        CangJieAnnotationEntryStub stub = getStub();
        if (stub == null && getGreenStub() != null) {
            return (CjValueArgumentList) findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST);
        }

        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_LIST);
    }
    @Nullable @IfNotParsed
    public CjTypeReference getTypeReference() {
        CjConstructorCalleeExpression calleeExpression = getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }
        return calleeExpression.getTypeReference();
    }
    @Nullable
    public Name getShortName() {
      CangJieAnnotationEntryStub stub = getStub();
        if (stub != null) {
            String shortName = stub.getShortName();
            if (shortName != null) {
                return Name.identifier(shortName);
            }
            return null;
        }

        CjTypeReference typeReference = getTypeReference();
        assert typeReference != null : "Annotation entry hasn't typeReference " + getText();
       CjTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof CjUserType userType) {
            String shortName = userType.getReferencedName();
            if (shortName != null) {
                return Name.identifier(shortName);
            }
        }
        return null;
    }


}
