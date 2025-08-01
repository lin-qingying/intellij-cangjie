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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.CallableMemberDescriptor

/**
 * 抽象类，用于定义处理重写冲突、伪重写和继承冲突的策略。
 *
 * 该类提供了处理重写相关的操作，包括添加伪重写、处理重写冲突和继承冲突的抽象方法。
 * 还提供了一个默认实现，用于设置成员的重写描述符。
 */
abstract class OverridingStrategy {

    /**
     * 添加伪重写到重写集合中。
     *
     * @param fakeOverride 伪重写的描述符。
     */
    abstract fun addFakeOverride(fakeOverride: CallableMemberDescriptor)

    /**
     * 处理重写冲突。此方法用于在重写过程中遇到两个重载冲突时进行处理。
     *
     * @param fromSuper 超类中的描述符。
     * @param fromCurrent 当前类中的描述符。
     */
    abstract fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)
     /**
     * 处理静态冲突,子类与父类中的静态成员与非静态成员不能重名
     *
     * @param fromSuper 超类中的描述符。
     * @param fromCurrent 当前类中的描述符。
    */
    abstract fun staticConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor,  message:String )

    /**
     * 处理继承冲突。此方法用于处理继承自超类的成员冲突。
     *
     * @param first 第一个冲突的描述符。
     * @param second 第二个冲突的描述符。
     */
    abstract fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor)

    /**
     * 设置成员的重写描述符。
     *
     * 默认实现将成员的 `overriddenDescriptors` 属性设置为给定的重写描述符集合。
     *
     * @param member 当前成员的描述符。
     * @param overridden 被重写的成员描述符集合。
     */
    open fun setOverriddenDescriptors(
        member: CallableMemberDescriptor,
        overridden: Collection<CallableMemberDescriptor>
    ) {
        member.overriddenDescriptors = overridden
    }
}

abstract class NonReportingOverrideStrategy : OverridingStrategy() {
    override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
        conflict(fromSuper, fromCurrent)
    }


    override fun staticConflict(
        fromSuper: CallableMemberDescriptor,
        fromCurrent: CallableMemberDescriptor,
        message: String
    ) {
        conflict(fromSuper, fromCurrent)

    }
    override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        conflict(first, second)
    }

    protected abstract fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)
}
