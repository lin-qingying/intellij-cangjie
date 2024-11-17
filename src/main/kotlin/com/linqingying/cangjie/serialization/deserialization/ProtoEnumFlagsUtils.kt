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

package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.metadata.ProtoBuf

@Suppress("unused")
fun ProtoEnumFlags.memberKind(memberKind: ProtoBuf.MemberKind?): CallableMemberDescriptor.Kind = when (memberKind) {
    ProtoBuf.MemberKind.DECLARATION -> CallableMemberDescriptor.Kind.DECLARATION
    ProtoBuf.MemberKind.FAKE_OVERRIDE -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
    ProtoBuf.MemberKind.DELEGATION -> CallableMemberDescriptor.Kind.DELEGATION
    ProtoBuf.MemberKind.SYNTHESIZED -> CallableMemberDescriptor.Kind.SYNTHESIZED
    else -> CallableMemberDescriptor.Kind.DECLARATION
}

@Suppress("unused")
fun ProtoEnumFlags.memberKind(kind: CallableMemberDescriptor.Kind): ProtoBuf.MemberKind = when (kind) {
    CallableMemberDescriptor.Kind.DECLARATION -> ProtoBuf.MemberKind.DECLARATION
    CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> ProtoBuf.MemberKind.FAKE_OVERRIDE
    CallableMemberDescriptor.Kind.DELEGATION -> ProtoBuf.MemberKind.DELEGATION
    CallableMemberDescriptor.Kind.SYNTHESIZED -> ProtoBuf.MemberKind.SYNTHESIZED
}

@Suppress("unused")
fun ProtoEnumFlags.descriptorVisibility(visibility: ProtoBuf.Visibility?): DescriptorVisibility = when (visibility) {
    ProtoBuf.Visibility.INTERNAL -> DescriptorVisibilities.INTERNAL
    ProtoBuf.Visibility.PRIVATE -> DescriptorVisibilities.PRIVATE
    ProtoBuf.Visibility.PRIVATE_TO_THIS -> DescriptorVisibilities.PRIVATE_TO_THIS
    ProtoBuf.Visibility.PROTECTED -> DescriptorVisibilities.PROTECTED
    ProtoBuf.Visibility.PUBLIC -> DescriptorVisibilities.PUBLIC
    ProtoBuf.Visibility.LOCAL -> DescriptorVisibilities.LOCAL
    else -> DescriptorVisibilities.PRIVATE
}

@Suppress("unused")
fun ProtoEnumFlags.descriptorVisibility(visibility: DescriptorVisibility): ProtoBuf.Visibility = when (visibility) {
    DescriptorVisibilities.INTERNAL -> ProtoBuf.Visibility.INTERNAL
    DescriptorVisibilities.PUBLIC -> ProtoBuf.Visibility.PUBLIC
    DescriptorVisibilities.PRIVATE -> ProtoBuf.Visibility.PRIVATE
    DescriptorVisibilities.PRIVATE_TO_THIS -> ProtoBuf.Visibility.PRIVATE_TO_THIS
    DescriptorVisibilities.PROTECTED -> ProtoBuf.Visibility.PROTECTED
    DescriptorVisibilities.LOCAL -> ProtoBuf.Visibility.LOCAL
    else -> throw IllegalArgumentException("Unknown visibility: $visibility")
}
