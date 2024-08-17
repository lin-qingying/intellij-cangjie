package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableMemberDescriptor

abstract class OverridingStrategy {
    abstract fun addFakeOverride(fakeOverride: CallableMemberDescriptor)

    abstract fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)

    abstract fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor)

    open fun setOverriddenDescriptors(member: CallableMemberDescriptor, overridden: Collection<CallableMemberDescriptor>) {
        member.overriddenDescriptors = overridden
    }
}

abstract class NonReportingOverrideStrategy : OverridingStrategy() {
    override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
        conflict(fromSuper, fromCurrent)
    }

    override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        conflict(first, second)
    }

    protected abstract fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor)
}
