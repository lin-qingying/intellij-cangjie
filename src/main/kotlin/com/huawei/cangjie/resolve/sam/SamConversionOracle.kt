package com.huawei.cangjie.resolve.sam

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.types.CangJieType

@DefaultImplementation(impl = SamConversionOracleDefault::class)
interface SamConversionOracle {
    fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean
    fun isPossibleSamType(samType: CangJieType): Boolean

}
class SamConversionOracleDefault : SamConversionOracle {
    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean = true

    override fun isPossibleSamType(samType: CangJieType): Boolean {
        val descriptor = samType.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && descriptor.isFun
    }


}
