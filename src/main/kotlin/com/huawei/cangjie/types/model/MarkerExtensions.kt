package com.huawei.cangjie.types.model

fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext): SimpleTypeMarker = with(c) { defaultType() }
fun CapturedTypeMarker.captureStatus(c: TypeSystemInferenceExtensionContext): CaptureStatus =
    with(c) {
        captureStatus()
    }
