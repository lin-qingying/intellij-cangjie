package com.linqingying.cangjie.resolve.sam

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.types.CangJieType

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
