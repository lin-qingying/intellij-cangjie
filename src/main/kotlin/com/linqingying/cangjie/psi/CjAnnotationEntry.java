package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.CjNodeTypes;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.psi.stubs.CangJieAnnotationEntryStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

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
    public @NotNull List<CjLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();

    }

    @Override
    public @NotNull List<CjTypeProjection> getTypeArguments() {
        CjTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public @Nullable CjTypeArgumentList getTypeArgumentList() {
        return null;
    }

    @Override
    public CjValueArgumentList getValueArgumentList() {
        CangJieAnnotationEntryStub stub = getStub();
        if (stub == null && getGreenStub() != null) {
            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST);
        }

        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    public @NotNull List<? extends ValueArgument> getValueArguments() {
        CangJieAnnotationEntryStub stub = getStub();
        if (stub != null && !stub.hasValueArguments()) {
            return Collections.<CjValueArgument>emptyList();
        }

        CjValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<CjValueArgument>emptyList();
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
