package com.linqingying.cangjie.types.error

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.PropertyDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.VariableDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.ErrorUtils

class ErrorPropertyDescriptor : PropertyDescriptor
by (
        PropertyDescriptorImpl.create(
            ErrorUtils.errorClass, Annotations.EMPTY, Modality.OPEN,
            DescriptorVisibilities.PUBLIC, true, Name.special(ErrorEntity.ERROR_PROPERTY.debugText),
            CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
            /*false, false, false, false, false, false*/
        ).apply {
            setType(ErrorUtils.errorPropertyType, emptyList(), null, null, emptyList())
        }
        )


class ErrorVariableDescriptor : VariableDescriptor
by (
//        VariableDescriptorImpl.create(
//            ErrorUtils.errorClass, Annotations.EMPTY, Modality.OPEN,
//            DescriptorVisibilities.PUBLIC, true, Name.special(ErrorEntity.ERROR_VARIABLE.debugText),
//            CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
//            /*false, false, false, false, false, false*/
//        ).apply {
//            setType(ErrorUtils.errorVariableType, emptyList(), null, null, emptyList())
//        }

        VariableDescriptorImpl.create(
            ErrorUtils.errorClass,   Name.special(ErrorEntity.ERROR_VARIABLE.debugText),DescriptorVisibilities.PUBLIC, false,
            SourceElement.NO_SOURCE,
        ).apply {
            setType(ErrorUtils.errorVariableType, emptyList(), null, null, emptyList())
        }
        )
