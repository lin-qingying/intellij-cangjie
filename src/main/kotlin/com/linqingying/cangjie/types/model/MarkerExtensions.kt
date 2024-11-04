package com.linqingying.cangjie.types.model
fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }

fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext): SimpleTypeMarker = with(c) { defaultType() }
fun CapturedTypeMarker.captureStatus(c: TypeSystemInferenceExtensionContext): CaptureStatus =
    with(c) {
        captureStatus()
    }
fun CangJieTypeMarker.dependsOnTypeParameters(c: TypeSystemInferenceExtensionContext, typeParameters: Collection<TypeParameterMarker>): Boolean =
    with(c) {
        val typeConstructors = typeParameters.mapTo(mutableSetOf()) { it.getTypeConstructor() }
        dependsOnTypeConstructor(c, typeConstructors)
    }
fun CangJieTypeMarker.dependsOnTypeConstructor(c: TypeSystemInferenceExtensionContext, typeConstructors: Set<TypeConstructorMarker>): Boolean =
    with(c) {
        contains { it.typeConstructor() in typeConstructors }
    }
fun TypeSubstitutorMarker.safeSubstitute(
    c: TypeSystemInferenceExtensionContext,
    type: CangJieTypeMarker
): CangJieTypeMarker = with(c) { safeSubstitute(type) }
