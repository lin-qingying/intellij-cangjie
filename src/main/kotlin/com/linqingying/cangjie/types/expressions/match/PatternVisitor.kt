package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.descriptors.impl.LazySubstitutingClassDescriptor
import com.linqingying.cangjie.diagnostics.Errors.NOT_ENUM_MATCH
import com.linqingying.cangjie.diagnostics.Errors.NOT_ENUM_PARAMETER_CONSTRUCTOR
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjBindingPattern
import com.linqingying.cangjie.psi.CjConstantPattern
import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjVisitor
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.resolve.BindingContext.REFERENCE_TARGET
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.CallExpressionResolver
import com.linqingying.cangjie.resolve.scopes.findClassifier
import com.linqingying.cangjie.types.util.isEnum
