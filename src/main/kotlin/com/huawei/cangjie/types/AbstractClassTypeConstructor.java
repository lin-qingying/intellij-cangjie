package com.huawei.cangjie.types;


import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.resolve.DescriptorUtilsKt;
import com.huawei.cangjie.storage.StorageManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @NotNull
    @Override
    public abstract ClassDescriptor getDeclarationDescriptor();

//    @Override
//    public final boolean isFinal() {
//        ClassDescriptor descriptor = getDeclarationDescriptor();
//        return ModalityUtilsKt.isFinalClass(descriptor) && !descriptor.isExpect();
//    }
@Override
protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
    return classifier instanceof ClassDescriptor && areFqNamesEqual(getDeclarationDescriptor(), classifier);
}

    @NotNull
    @Override
    public CangJieBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

//    @Override
//    protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
//        return classifier instanceof ClassDescriptor && areFqNamesEqual(getDeclarationDescriptor(), classifier);
//    }

//    @NotNull
//    @Override
//    protected Collection<CangJieType> getAdditionalNeighboursInSupertypeGraph(boolean useCompanions) {
//        DeclarationDescriptor containingDeclaration = getDeclarationDescriptor().getContainingDeclaration();
//
//        if (!(containingDeclaration instanceof ClassDescriptor)) {
//            return Collections.emptyList();
//        }
//
//        Collection<CangJieType> additionalNeighbours = new SmartList<CangJieType>();
//
//        // We suppose that there is an edge from C to A in graph when disconnecting loops in supertypes,
//        // because such cyclic declarations should be prohibited (see p.10.2.1 of CangJie spec)
//        // class A : B {
//        //   static class C {}
//        // }
//        // class B : A.C {}
//        ClassDescriptor containingClassDescriptor = (ClassDescriptor) containingDeclaration;
//        additionalNeighbours.add(containingClassDescriptor.getDefaultType());
//
//        // Also we add edge from host-class to companion object. Together with previous edges
//        // (from nesteds to containing class), they can create visibility loops like in the
//        // following example:
//        //
//        // class ContainingClass {
//        //   open class Nested {}  // to create scope for resolving Nested, we have to resolve CO header
//        //   companion object : Nested() {} // to resolve CO header, we have to resolve Nested
//        // }
//        //
//        // Relates to KT-21515
//        ClassDescriptor companion = containingClassDescriptor.getCompanionObjectDescriptor();
//        if (useCompanions && companion != null) {
//            additionalNeighbours.add(companion.getDefaultType());
//        }
//
//        return additionalNeighbours;
//    }
//
//    @Nullable
//    @Override
//    protected CangJieType defaultSupertypeIfEmpty() {
//        if (CangJieBuiltIns.isSpecialClassWithNoSupertypes(this.getDeclarationDescriptor())) {
//            return null;
//        } else {
//            return getBuiltIns().getAnyType();
//        }
//    }
}
