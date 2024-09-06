package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.utils.SmartSet
import java.util.*

/**
 * @param <H> is something that handles CallableDescriptor inside
 */
fun <H : Any> Collection<H>.selectMostSpecificInEachOverridableGroup(
    descriptorByHandle: H.() -> CallableDescriptor
): Collection<H> {
    if (size <= 1) return this
    val queue = LinkedList<H>(this)
    val result = SmartSet.create<H>()

    while (queue.isNotEmpty()) {
        val nextHandle: H = queue.first()

        val conflictedHandles = SmartSet.create<H>()

        val overridableGroup =
            OverridingUtil.extractMembersOverridableInBothWays(nextHandle, queue, descriptorByHandle) { conflictedHandles.add(it) }

        if (overridableGroup.size == 1 && conflictedHandles.isEmpty()) {
            result.add(overridableGroup.single())
            continue
        }

        val mostSpecific = OverridingUtil.selectMostSpecificMember(overridableGroup, descriptorByHandle)
        val mostSpecificDescriptor = mostSpecific.descriptorByHandle()

        overridableGroup.filterNotTo(conflictedHandles) {
            OverridingUtil.isMoreSpecific(mostSpecificDescriptor, it.descriptorByHandle())
        }

        if (conflictedHandles.isNotEmpty()) {
            result.addAll(conflictedHandles)
        }

        result.add(mostSpecific)
    }
    return result
}
