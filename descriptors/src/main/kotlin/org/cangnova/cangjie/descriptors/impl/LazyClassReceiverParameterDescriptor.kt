/*
 * Copyright 2025 LinQingYing. and contributors.
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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

class LazyClassReceiverParameterDescriptor(private val descriptor: ClassDescriptor) :
    AbstractReceiverParameterDescriptor(
        Annotations.EMPTY
    ) {
    private val receiverValue =  ImplicitClassReceiver(descriptor, null)



    override val value: ReceiverValue
        get() = receiverValue

    override val containingDeclaration: DeclarationDescriptor
        get() = descriptor

    override fun copy(newOwner: DeclarationDescriptor): ReceiverParameterDescriptor {
        throw UnsupportedOperationException()
    }

    public override fun toString(): String {
        return descriptor.kind.toString() + " " + descriptor.name + "::this"
    }




}
