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

package cn.cangnova.cangjie.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import cn.cangnova.cangjie.lang.CangJieLanguage;
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
    public static class CjLeftBoundNodeType extends CjNodeType {
        public CjLeftBoundNodeType(@NotNull @NonNls String debugName, Class<? extends CjElement> psiClass) {
            super(debugName, psiClass);
        }

        @Override
        public boolean isLeftBound() {
            return true;
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
