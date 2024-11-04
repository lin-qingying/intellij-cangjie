package com.linqingying.cangjie.ide.hierarchy;


import com.linqingying.cangjie.descriptors.ClassDescriptor;
import com.linqingying.cangjie.descriptors.ClassifierDescriptor;
import com.linqingying.cangjie.descriptors.SourceElement;
import com.linqingying.cangjie.psi.CjTypeStatement;
import com.linqingying.cangjie.resolve.caches.ResolutionUtils;
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode;
import com.linqingying.cangjie.resolve.source.PsiSourceElementKt;
import com.linqingying.cangjie.types.CangJieType;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class CangJieTypeHierarchyTreeStructure extends CangJieSubTypesHierarchyTreeStructure {
    public CangJieTypeHierarchyTreeStructure(@NotNull final CjTypeStatement cl) {
        super(cl.getProject(), buildHierarchyElement(cl));
        setBaseElement(myBaseDescriptor);
    }

    private static CangJieHierarchyNodeDescriptor buildHierarchyElement(@NotNull final CjTypeStatement cl) {
        CangJieHierarchyNodeDescriptor descriptor = null;
        ClassDescriptor classDescriptor = ResolutionUtils.resolveToDescriptorIfAny(
                cl, BodyResolveMode.FULL
        );

        if (classDescriptor != null) {
            for (CangJieType superClass : classDescriptor.getTypeConstructor().getSupertypes()) {
                ClassifierDescriptor superClassDescriptor = superClass.getConstructor().getDeclarationDescriptor();
                if (superClassDescriptor != null) {
                    SourceElement sourceElement = superClassDescriptor.getSource();
                    if (sourceElement != null) {
                        PsiElement psiElement = PsiSourceElementKt.getPsi(sourceElement);
                        if (psiElement != null) {
                            final CangJieHierarchyNodeDescriptor newDescriptor = buildHierarchyElement((CjTypeStatement) psiElement);

//                            final CangJieHierarchyNodeDescriptor newDescriptor = new CangJieHierarchyNodeDescriptor(descriptor, psiElement, false);
                            if (descriptor != null) {
                                descriptor.setCachedChildren(new CangJieHierarchyNodeDescriptor[]{newDescriptor});
                            }
                            descriptor = newDescriptor;
                        }
                    }
                }


            }
            
        }
        final CangJieHierarchyNodeDescriptor newDescriptor = new CangJieHierarchyNodeDescriptor(descriptor, cl, true);
        if (descriptor != null) {
            descriptor.setCachedChildren(new HierarchyNodeDescriptor[]{newDescriptor});
        }
        return newDescriptor;
    }
}
