package com.huawei.cangjie.types.expressions.match

import com.huawei.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.huawei.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.huawei.cangjie.diagnostics.Errors.NOT_ENUM_MATCH
import com.huawei.cangjie.diagnostics.Errors.NOT_ENUM_PARAMETER_CONSTRUCTOR
import com.huawei.cangjie.incremental.components.NoLookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjBindingPattern
import com.huawei.cangjie.psi.CjConstantPattern
import com.huawei.cangjie.psi.CjEnum
import com.huawei.cangjie.psi.CjVisitor
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.resolve.BindingContext.REFERENCE_TARGET
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.CallExpressionResolver
import com.huawei.cangjie.resolve.scopes.findClassifier
import com.huawei.cangjie.types.util.isEnum
