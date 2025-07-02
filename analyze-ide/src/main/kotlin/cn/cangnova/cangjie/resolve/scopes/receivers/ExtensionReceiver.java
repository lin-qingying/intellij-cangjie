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

package cn.cangnova.cangjie.resolve.scopes.receivers;

import cn.cangnova.cangjie.descriptors.CallableDescriptor;
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor;
import cn.cangnova.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtensionReceiver extends AbstractReceiverValue implements ImplicitReceiver{

    private final CallableDescriptor descriptor;

    public ExtensionReceiver(
            @NotNull CallableDescriptor callableDescriptor,
            @NotNull CangJieType receiverType,
            @Nullable ReceiverValue original
    ) {
        super(receiverType, original);
        this.descriptor = callableDescriptor;
    }
    @NotNull
    @Override
    public DeclarationDescriptor getDeclarationDescriptor() {
        return descriptor;

    }

    @Override
    public String toString() {
        return getType() + ": Ext {" + descriptor + "}";
    }
    @Override
    public @NotNull ReceiverValue replaceType(@NotNull CangJieType newType) {
        return new ExtensionReceiver(descriptor, newType, getOriginal());

    }
}
