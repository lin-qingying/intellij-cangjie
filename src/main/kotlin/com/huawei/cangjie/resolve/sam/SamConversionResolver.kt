package com.huawei.cangjie.resolve.sam

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.container.PlatformSpecificExtension
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.SimpleType

val SAM_LOOKUP_NAME = Name.special("<SAM-CONSTRUCTOR>")

//@DefaultImplementation(impl = SamConversionResolverImpl.SamConversionResolverWithoutReceiverConversion::class)
//interface SamConversionResolver : PlatformSpecificExtension<SamConversionResolver> {
//    object Empty : SamConversionResolver {
//        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? = null
//    }
//
//    fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType?
//}
//class SamConversionResolverImpl(
//    storageManager: StorageManager,
//    private val samWithReceiverResolvers: Iterable<SamWithReceiverResolver>
//) : SamConversionResolver {
//    class SamConversionResolverWithoutReceiverConversion(storageManager: StorageManager) : SamConversionResolver {
//        val resolver = SamConversionResolverImpl(storageManager, emptyList())
//
//        override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
//            return resolver.resolveFunctionTypeIfSamInterface(classDescriptor)
//        }
//    }
//
//    private val functionTypesForSamInterfaces = storageManager.createCacheWithNullableValues<ClassDescriptor, SimpleType>()
//
//    override fun resolveFunctionTypeIfSamInterface(classDescriptor: ClassDescriptor): SimpleType? {
//        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
//            val abstractMethod = getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
//            val shouldConvertFirstParameterToDescriptor = samWithReceiverResolvers.any { it.shouldConvertFirstSamParameterToReceiver(abstractMethod) }
//            getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
//        }
//    }
//}
