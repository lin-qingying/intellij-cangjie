package com.linqingying.cangjie;

import com.linqingying.cangjie.lang.CangJieLanguage;
import com.linqingying.cangjie.psi.CjElement;
import com.linqingying.cangjie.psi.CjElementImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;


public class CjNodeType extends IElementType {
    private final Constructor<? extends CjElement> myPsiFactory;

    public CjNodeType(@NotNull @NonNls String debugName, Class<? extends CjElement> psiClass) {


        super(debugName, CangJieLanguage.INSTANCE);
        try {
            myPsiFactory = psiClass != null ? psiClass.getConstructor(ASTNode.class) : null;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Must have a constructor with ASTNode");
        }
    }

    public CjElement createPsi(ASTNode node) {
        assert node.getElementType() == this;

        try {
            if (myPsiFactory == null) {
                return new CjElementImpl(node);
            }
            return myPsiFactory.newInstance(node);
        } catch (Exception e) {
            throw new RuntimeException("Error creating psi element for node", e);
        }
    }


}
