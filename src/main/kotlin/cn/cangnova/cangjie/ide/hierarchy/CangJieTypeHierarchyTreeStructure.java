/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.hierarchy;


import cn.cangnova.cangjie.descriptors.ClassDescriptor;
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor;
import cn.cangnova.cangjie.descriptors.SourceElement;
import cn.cangnova.cangjie.psi.CjTypeStatement;
import cn.cangnova.cangjie.resolve.caches.ResolutionUtils;
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode;
import cn.cangnova.cangjie.resolve.source.PsiSourceElementKt;
import cn.cangnova.cangjie.types.CangJieType;
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
