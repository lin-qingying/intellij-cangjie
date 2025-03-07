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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import cn.cangnova.cangjie.lexer.CjTokens;

public class CjCallableReferenceExpression extends CjExpressionImpl implements CjDoubleColonExpression {
    public CjCallableReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public CjSimpleNameExpression getCallableReference() {
        PsiElement psi = getDoubleColonTokenReference();
        while (psi != null) {
            if (psi instanceof CjSimpleNameExpression) {
                return (CjSimpleNameExpression) psi;
            }
            psi = psi.getNextSibling();
        }

        throw new IllegalStateException("Callable reference simple name shouldn't be parsed to null");
    }

    @Nullable
    @Override
    public PsiElement findColonColon() {
        return null;
//        return findChildByType(CjTokens.COLONCOLON);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitCallableReferenceExpression(this, data);
    }
}
