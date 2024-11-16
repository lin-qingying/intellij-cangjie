package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.NotFoundClasses
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor

class ReflectionTypes(module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses)
{

  companion object{
    fun isNumberedTypeWithOneOrMoreNumber(type: CangJieType): Boolean {
      val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
      val fqName = DescriptorUtils.getFqName(descriptor)
      if (fqName.isRoot) return false

//      if (fqName.parent().toSafe() != CANGJIE_REFLECT_FQ_NAME) return false
      val shortName = descriptor.name.asString()

//      for (prefix in PREFIXES) {
//        if (shortName.startsWith(prefix)) {
//          val number = shortName.removePrefix(prefix)
//          return number.isNotEmpty() && number != "0"
//        }
//      }
      return false
    }
    fun isPossibleExpectedCallableType(typeConstructor: TypeConstructor): Boolean {
      val descriptor = typeConstructor.declarationDescriptor as? ClassDescriptor ?: return false
      if (CangJieBuiltIns.isAny(descriptor)) return true

      val fqName = DescriptorUtils.getFqName(descriptor)
      if (fqName.isRoot) return false




      return false
    }
  }
}
