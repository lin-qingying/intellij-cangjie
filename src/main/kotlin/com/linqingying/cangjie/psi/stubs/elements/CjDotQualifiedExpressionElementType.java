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

package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.CjDotQualifiedExpression;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;



public class CjDotQualifiedExpressionElementType extends CjPlaceHolderStubElementType<CjDotQualifiedExpression> {
    public CjDotQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjDotQualifiedExpression.class);
    }

    private static boolean checkNodeTypesTraversal(ASTNode node) {

        IElementType type = node.getElementType();
        if (type != CjStubElementTypes.DOT_QUALIFIED_EXPRESSION &&
                type != CjStubElementTypes.REFERENCE_EXPRESSION &&
                type != CjTokens.IDENTIFIER &&
                type != CjTokens.DOT
        ) {
            return false;
        }

        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (!checkNodeTypesTraversal(child)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) return false;

        IElementType parentElementType = treeParent.getElementType();
        if (
                parentElementType == CjStubElementTypes.PACKAGE_DIRECTIVE ||
                parentElementType == CjStubElementTypes.VALUE_ARGUMENT ||

                parentElementType == CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
        ) {
            return checkNodeTypesTraversal(node) && super.shouldCreateStub(node);
        }

        return false;
    }
}
