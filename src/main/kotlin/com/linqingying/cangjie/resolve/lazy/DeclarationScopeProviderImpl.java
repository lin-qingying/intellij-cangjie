package com.linqingying.cangjie.resolve.lazy;

import com.linqingying.cangjie.descriptors.ClassDescriptorWithResolutionScopes;
import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.psi.psiUtil.CjStubbedPsiUtil;
import com.linqingying.cangjie.psi.psiUtil.PsiUtilsKt;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import com.linqingying.cangjie.incremental.components.NoLookupLocation;
public class DeclarationScopeProviderImpl implements DeclarationScopeProvider {

    private final LazyDeclarationResolver lazyDeclarationResolver;

    private final FileScopeProvider fileScopeProvider;

    public DeclarationScopeProviderImpl(
            @NotNull LazyDeclarationResolver lazyDeclarationResolver,
            @NotNull FileScopeProvider fileScopeProvider
    ) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
        this.fileScopeProvider = fileScopeProvider;
    }

    @NotNull
    @Override
    public LexicalScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {


        CjDeclaration cjDeclaration = CjStubbedPsiUtil.getPsiOrStubParent(elementOfDeclaration, CjDeclaration.class, false);
        assert !(elementOfDeclaration instanceof CjDeclaration) || cjDeclaration == elementOfDeclaration :
                "For CjDeclaration element getParentOfType() should return itself.";
        assert cjDeclaration != null : "Should be contained inside declaration.";
        CjDeclaration parentDeclaration = CjStubbedPsiUtil.getContainingDeclaration(cjDeclaration);
        if (cjDeclaration instanceof CjPropertyAccessor) {
            parentDeclaration = CjStubbedPsiUtil.getContainingDeclaration(parentDeclaration, CjDeclaration.class);
        }

        if (parentDeclaration == null) {
            return fileScopeProvider.getFileResolutionScope((CjFile) elementOfDeclaration.getContainingFile());
        }


        if (parentDeclaration instanceof CjTypeStatement parentClassOrStruct) {
            ClassDescriptorWithResolutionScopes parentClassDescriptor = (ClassDescriptorWithResolutionScopes) lazyDeclarationResolver.getClassDescriptor(parentClassOrStruct, NoLookupLocation.MATCH_GET_DECLARATION_SCOPE);

            if (cjDeclaration instanceof CjAnonymousInitializer || cjDeclaration instanceof CjProperty  || cjDeclaration instanceof CjVariable) {
                return parentClassDescriptor.getScopeForInitializerResolution();
            }


            return parentClassDescriptor.getScopeForMemberDeclarationResolution();
        }
        throw new IllegalStateException("Don't call this method for local declarations: " + cjDeclaration + "\n" +
                PsiUtilsKt.getElementTextWithContext(cjDeclaration));
    }

    @NotNull
    @Override
    public DataFlowInfo getOuterDataFlowInfoForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        return DataFlowInfoFactory.EMPTY;

    }
}
