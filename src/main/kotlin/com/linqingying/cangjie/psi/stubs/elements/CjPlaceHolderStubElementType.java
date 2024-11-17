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


import com.linqingying.cangjie.psi.CjElementImplStub;
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjPlaceHolderStubElementType<T extends CjElementImplStub<? extends StubElement<?>>> extends
        CjStubElementType<CangJiePlaceHolderStub<T>, T> {
    public CjPlaceHolderStubElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, CangJiePlaceHolderStub.class);
    }


    @Override
    public @NotNull CangJiePlaceHolderStub<T> createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new CangJiePlaceHolderStubImpl<>(parentStub, this);
    }



    @Override
    public void serialize(@NotNull CangJiePlaceHolderStub<T> stub, @NotNull StubOutputStream dataStream) throws IOException {
        //do nothing
    }

    @NotNull
    @Override
    public CangJiePlaceHolderStub<T> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new CangJiePlaceHolderStubImpl<>(parentStub, this);
    }


}
