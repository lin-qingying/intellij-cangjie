package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
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
    fun isPossibleExpectedCallableType(typeConstructor: TypeConstructor): Boolean {
      val descriptor = typeConstructor.declarationDescriptor as? ClassDescriptor ?: return false
      if (CangJieBuiltIns.isAny(descriptor)) return true

      val fqName = DescriptorUtils.getFqName(descriptor)
      if (fqName.isRoot) return false




      return false
    }
  }
}
