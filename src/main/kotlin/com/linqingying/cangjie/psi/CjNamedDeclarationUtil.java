package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.FqNameUnsafe;
import com.linqingying.cangjie.name.Name;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public final class CjNamedDeclarationUtil {
    @Nullable
    public static FqNameUnsafe getUnsafeFQName(@NotNull CjNamedDeclaration namedDeclaration) {
        FqName fqName = namedDeclaration.getFqName();
        return fqName != null ? fqName.toUnsafe() : null;
    }

    @Nullable

  static FqName getFQName(@NotNull CjNamedDeclaration namedDeclaration) {
        Name name = namedDeclaration.getNameAsName();
        if (name == null) {
            return null;
        }

        FqName parentFqName = getParentFqName(namedDeclaration);

        if (parentFqName == null) {
            return null;
        }

        return parentFqName.child(name);
    }

    @Nullable
    public static FqName getParentFqName(@NotNull CjNamedDeclaration namedDeclaration) {
        PsiElement parent = namedDeclaration.getParent();
        if (parent instanceof CjClassBody) {

            parent = parent.getParent();
        }

        if (parent instanceof CjFile) {
            return ((CjFile) parent).getPackageFqName();
        }

        else if (namedDeclaration instanceof CjParameter) {
            CjClassOrStruct constructorClass = CjPsiUtil.getClassIfParameterIsProperty((CjParameter) namedDeclaration);
            if (constructorClass != null) {
                return getFQName(constructorClass);
            }
        }



        return null;
    }

    private CjNamedDeclarationUtil() {
    }
}
