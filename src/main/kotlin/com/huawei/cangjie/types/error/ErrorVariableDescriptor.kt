package com.huawei.cangjie.types.error

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.VariableDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.ErrorUtils

//class ErrorVariableDescriptor : VariableDescriptor
//by (
//        VariableDescriptorImpl.create(
//            ErrorUtils.errorClass, Annotations.EMPTY, Modality.OPEN,
//            DescriptorVisibilities.PUBLIC, true, Name.special(ErrorEntity.ERROR_PROPERTY.debugText),
//            CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
//            false, false, false, false, false, false
//        ).apply {
//            setType(ErrorUtils.errorVariableType, emptyList(), null, null, emptyList())
//        }
//        )
