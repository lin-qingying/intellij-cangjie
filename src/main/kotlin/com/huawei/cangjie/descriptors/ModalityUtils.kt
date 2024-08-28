package com.huawei.cangjie.descriptors


val ClassDescriptor.isFinalOrEnum: Boolean
    get() = modality == Modality.FINAL
