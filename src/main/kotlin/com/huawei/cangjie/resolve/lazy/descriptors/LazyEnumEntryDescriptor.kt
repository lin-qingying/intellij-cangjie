package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.ClassConstructorDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.huawei.cangjie.diagnostics.Errors.REDECLARATION
import com.huawei.cangjie.diagnostics.reportOnDeclaration
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.descriptorUtil.classId
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.data.CjClassInfo
import com.huawei.cangjie.resolve.lazy.data.CjEnmuEntryInfo
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.ClassAndEnumConstructorDescriptor


fun CjClassInfo<*>.toSourceElement(): SourceElement {
    return elementByE.toSourceElement()
}
