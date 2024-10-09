package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.utils.DFS
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
fun <D : CallableDescriptor> D.findOriginalTopMostOverriddenDescriptors(): Set<D> {
    return findTopMostOverriddenDescriptors().mapTo(LinkedHashSet<D>()) {
        @Suppress("UNCHECKED_CAST")
        (it.original as D)
    }
}

fun <D : CallableDescriptor> D.findTopMostOverriddenDescriptors(): List<D> {
    return DFS.dfs(
        listOf(this),
        { current -> current.overriddenDescriptors },
        object : DFS.CollectingNodeHandler<CallableDescriptor, CallableDescriptor, ArrayList<D>>(ArrayList<D>()) {
            override fun afterChildren(current: CallableDescriptor) {
                if (current.overriddenDescriptors.isEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    result.add(current as D)
                }
            }
        })
}
