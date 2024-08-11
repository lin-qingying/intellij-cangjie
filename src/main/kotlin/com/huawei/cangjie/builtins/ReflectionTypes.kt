package com.huawei.cangjie.builtins

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.NotFoundClasses
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor

class ReflectionTypes(module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses)
{

  companion object{
//      fun isCCallableType(type: CangJieType): Boolean =
//          hasCCallableTypeFqName(type) || type.constructor.supertypes.any { isCCallableType(it) }
//      fun hasCCallableTypeFqName(type: CangJieType): Boolean =
//          hasFqName(type.constructor, StandardNames.FqNames.cCallable)
//      private fun hasFqName(typeConstructor: TypeConstructor, fqName: FqNameUnsafe): Boolean {
//          val descriptor = typeConstructor.declarationDescriptor
//          return descriptor is ClassDescriptor && hasFqName(descriptor, fqName)
//      }
//      private fun hasFqName(descriptor: ClassDescriptor, fqName: FqNameUnsafe): Boolean {
//          return descriptor.name == fqName.shortName() && DescriptorUtils.getFqName(descriptor) == fqName
//      }
  }
}
