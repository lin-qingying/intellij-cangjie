package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.name.FqName;
import com.huawei.cangjie1.name.FqNameUnsafe;
import com.huawei.cangjie1.name.Name;
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
    //NOTE: use JetNamedDeclaration#getFqName instead
    /*package private*/ static FqName getFQName(@NotNull CjNamedDeclaration namedDeclaration) {
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
            // One nesting to JetClassBody doesn't affect to qualified name
            parent = parent.getParent();
        }

        if (parent instanceof CjFile) {
            return ((CjFile) parent).getPackageFqName();
        }

        else if (namedDeclaration instanceof CjParameter) {
            CjClassOrObject constructorClass = CjPsiUtil.getClassIfParameterIsProperty((CjParameter) namedDeclaration);
            if (constructorClass != null) {
                return getFQName(constructorClass);
            }
        }



        return null;
    }

    private CjNamedDeclarationUtil() {
    }
}
