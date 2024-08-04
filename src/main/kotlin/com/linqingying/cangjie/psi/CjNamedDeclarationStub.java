package com.linqingying.cangjie.psi;


import com.linqingying.cangjie.lang.CangJieFileType;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.stubs.CangJieStubWithFqName;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linqingying.cangjie.CjNodeTypes.OPERATION_REFERENCE;

public abstract class CjNamedDeclarationStub<T extends CangJieStubWithFqName<?>> extends CjDeclarationStub<T> implements CjNamedDeclaration {
    public CjNamedDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjNamedDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        T stub = getStub();
        if (stub != null) {
            return stub.getName();
        }

        PsiElement identifier = getNameIdentifier();
        if (identifier != null) {
            String text = identifier.getText();
            return text != null ? CjPsiUtil.unquoteIdentifier(text) : null;
        }
        PsiElement operation = getOperatorReference();

        if (operation != null) {
            return operation.getText();
        }

        return null;

    }

    @Override
    public Name getNameAsName() {
        String name = getName();
        return name != null ? Name.identifier(name) : null;
    }

    @Override
    @NotNull
    public Name getNameAsSafeName() {
        return CjPsiUtil.safeName(getName());
    }

    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(CjTokens.IDENTIFIER);
    }

    public PsiElement getOperatorReference() {
        return findChildByType(OPERATION_REFERENCE);
    }


    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        PsiElement identifier = getNameIdentifier();
        if (identifier == null) return null;


        PsiElement newIdentifier = new CjPsiFactory(getProject()).createNameIdentifierIfPossible(CjPsiUtilKt.quoteIfNeeded(name));
        if (newIdentifier != null) {
            CjPsiUtilKt.astReplace(identifier, newIdentifier);
        } else {
            identifier.delete();
        }
        return this;
    }

    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        CjElement enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock != null) {
//            PsiElement enclosingParent = enclosingBlock.getParent();


//            if (enclosingParent instanceof CjContainerNode) {
//                enclosingParent = enclosingParent.getParent();
//            }


            return new LocalSearchScope(enclosingBlock);
        }

//        PsiElement parent = getParent();
//        PsiElement grandParent = parent != null ? parent.getParent() : null;


        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) {
            CjElement containingClass = PsiTreeUtil.getParentOfType(this, CjClassOrStruct.class);

            if (containingClass != null) {
                return new LocalSearchScope(containingClass);
            }
            CjFile CjFile = getContainingCjFile();
            if (this instanceof CjClassOrStruct) {

                Project project = getProject();
                GlobalSearchScope CangJieFilesScope = GlobalSearchScope.getScopeRestrictedByFileTypes(
                        GlobalSearchScope.allScope(project),
                        CangJieFileType.INSTANCE
                );

                SearchScope fileScope = GlobalSearchScope.fileScope(CjFile);

                SearchScope nonCangJieJvmScope = GlobalSearchScope.notScope(CangJieFilesScope);
                return fileScope.union(nonCangJieJvmScope);
            } else {
                return new LocalSearchScope(CjFile);
            }
        }

        SearchScope scope = super.getUseScope();

        CjClassOrStruct ClassOrStruct = CjPsiUtilKt.getContainingClassOrStruct(this);
        if (ClassOrStruct != null) {
            scope = scope.intersectWith(ClassOrStruct.getUseScope());
        }

        return scope;
    }

    @Nullable
    @Override
    public FqName getFqName() {

        T stub = getStub();
        if (stub != null) {
            return stub.getFqName();
        }
        return CjNamedDeclarationUtil.getFQName(this);
    }
}
