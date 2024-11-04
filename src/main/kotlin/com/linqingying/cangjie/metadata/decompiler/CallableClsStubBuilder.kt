package com.linqingying.cangjie.metadata.decompiler

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import com.linqingying.cangjie.metadata.MODALITY
import com.linqingying.cangjie.metadata.OPERATOR
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.ProtoBuf.Modality
import com.linqingying.cangjie.metadata.VISIBILITY
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.impl.*
import com.linqingying.cangjie.serialization.deserialization.AnnotatedCallableKind
import com.linqingying.cangjie.serialization.deserialization.ProtoContainer
import com.linqingying.cangjie.serialization.deserialization.descriptors.FacadeClassSource
import com.linqingying.cangjie.serialization.deserialization.getName

const val COMPILED_DEFAULT_INITIALIZER = "COMPILED_CODE"
fun createPackageDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer.Package,
    packageProto: ProtoBuf.Package
) {
    createDeclarationsStubs(parentStub, outerContext, protoContainer, packageProto.functionList, emptyList())
    createTypeAliasesStubs(parentStub, outerContext, protoContainer, packageProto.typeAliasList)
}

fun createTypeAliasesStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    typeAliasesProtos: List<ProtoBuf.TypeAlias>
) {
    for (typeAliasProto in typeAliasesProtos) {
        createTypeAliasStub(parentStub, typeAliasProto, protoContainer, outerContext)
    }
}

fun createDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    functionProtos: List<ProtoBuf.Function>,
    propertyProtos: List<ProtoBuf.Property>,
) {
    for (propertyProto in propertyProtos) {
        if (mustNotBeWrittenToStubs(propertyProto.flags)) {
            continue
        }

        PropertyClsStubBuilder(parentStub, outerContext, protoContainer, propertyProto).build()
    }
    for (functionProto in functionProtos) {
        if (mustNotBeWrittenToStubs(functionProto.flags)) {
            continue
        }

        FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionProto).build()
    }
}


private fun mustNotBeWrittenToStubs(flags: Int): Boolean {
    return Flags.MEMBER_KIND.get(flags) == ProtoBuf.MemberKind.FAKE_OVERRIDE
}

abstract class CallableClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protected val protoContainer: ProtoContainer,
    private val typeParameters: List<ProtoBuf.TypeParameter>
) {
    protected val c = outerContext.child(typeParameters)
    protected val typeStubBuilder = TypeClsStubBuilder(c)
    private val contextReceiversListStubBuilder = ContextReceiversListStubBuilder(c)
    protected val isTopLevel: Boolean get() = protoContainer is ProtoContainer.Package
    protected val callableStub: StubElement<out PsiElement> by lazy(LazyThreadSafetyMode.NONE) {
        doCreateCallableStub(
            parent
        )
    }

    fun build() {
        contextReceiversListStubBuilder.createContextReceiverStubs(callableStub, contextReceiverTypes)
        createModifierListStub()
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(callableStub, typeParameters)
        createReceiverTypeReferenceStub()
        createValueParameterList()
        createReturnTypeStub()
        typeStubBuilder.createTypeConstraintListStub(callableStub, typeConstraintListData)
        createCallableSpecialParts()
    }

    abstract val receiverType: ProtoBuf.Type?
    abstract val receiverAnnotations: List<AnnotationWithTarget>

    abstract val returnType: ProtoBuf.Type?
    abstract val contextReceiverTypes: List<ProtoBuf.Type>

    private fun createReceiverTypeReferenceStub() {
        receiverType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it, this::receiverAnnotations)
        }
    }

    private fun createReturnTypeStub() {
        returnType?.let {
            typeStubBuilder.createTypeReferenceStub(callableStub, it)
        }
    }

    abstract fun createModifierListStub()

    abstract fun createValueParameterList()

    abstract fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement>

    protected open fun createCallableSpecialParts() {}
}

private class FunctionClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val functionProto: ProtoBuf.Function
) : CallableClsStubBuilder(parent, outerContext, protoContainer, functionProto.typeParameterList) {
    override val receiverType: ProtoBuf.Type?
        get() = functionProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<AnnotationWithTarget>
        get() {
            return c.components.annotationLoader
                .loadExtensionReceiverParameterAnnotations(
                    protoContainer,
                    functionProto,
                    AnnotatedCallableKind.FUNCTION
                )
                .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }
        }

    override val returnType: ProtoBuf.Type
        get() = functionProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = functionProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(
            callableStub,
            functionProto,
            functionProto.valueParameterList,
            protoContainer
        )
    }

    override fun createModifierListStub() {
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)
        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, functionProto.flags,
            listOf(VISIBILITY, OPERATOR) + modalityModifier
        )

        // If function is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return

        val annotations = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, functionProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotations, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(functionProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As functions are never decompiled to fun f() = 1 form, hasBlockBody is always true
        // This info is anyway irrelevant for the purposes these stubs are used
        val hasContract = functionProto.hasContract()
        return CangJieFunctionStubImpl(
            parent,
            CjStubElementTypes.FUNCTION,
            callableName.ref(),
            isTopLevel,
            c.containerFqName.child(callableName),
            isExtension = functionProto.hasReceiver(),
            hasBlockBody = true,
            hasBody = Flags.MODALITY.get(functionProto.flags) != Modality.ABSTRACT,
            hasTypeParameterListBeforeFunctionName = functionProto.typeParameterList.isNotEmpty(),
            null,
//            runIf(hasContract) {
//                ClsContractBuilder(c, typeStubBuilder).loadContract(functionProto)
//            },
            origin = createStubOrigin(protoContainer)
        )
    }
}

internal fun createStubOrigin(protoContainer: ProtoContainer): CangJieStubOrigin? {
    if (protoContainer is ProtoContainer.Package) {
        val source = protoContainer.source
        if (source is FacadeClassSource) {
            val className = source.className.internalName
            val facadeClassName = source.facadeClassName?.internalName
            if (facadeClassName != null) {
                return CangJieStubOrigin.MultiFileFacade(className, facadeClassName)
            }

            return CangJieStubOrigin.Facade(className)
        }
    }

    return null
}

private class PropertyClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val propertyProto: ProtoBuf.Property
) : CallableClsStubBuilder(parent, outerContext, protoContainer, propertyProto.typeParameterList) {
    private val isVar = Flags.IS_VAR.get(propertyProto.flags)

    override val receiverType: ProtoBuf.Type?
        get() = propertyProto.receiverType(c.typeTable)

    override val receiverAnnotations: List<AnnotationWithTarget>
        get() = c.components.annotationLoader
            .loadExtensionReceiverParameterAnnotations(
                protoContainer,
                propertyProto,
                AnnotatedCallableKind.PROPERTY_GETTER
            )
            .map { AnnotationWithTarget(it, AnnotationUseSiteTarget.RECEIVER) }

    override val returnType: ProtoBuf.Type
        get() = propertyProto.returnType(c.typeTable)

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = propertyProto.contextReceiverTypes(c.typeTable)

    override fun createValueParameterList() {
    }

    override fun createModifierListStub() {
//        val constModifier = if (isVar) listOf() else listOf(CONST)
        val modalityModifier = if (isTopLevel) listOf() else listOf(MODALITY)

        val modifierListStubImpl = createModifierListStubForDeclaration(
            callableStub, propertyProto.flags,
            listOf(VISIBILITY) + /*constModifier +*/ modalityModifier
        )

        // If field is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(propertyProto.flags)) return

//        val propertyAnnotations =
//            c.components.annotationLoader.loadCallableAnnotations(protoContainer, propertyProto, AnnotatedCallableKind.PROPERTY)
//        val backingFieldAnnotations =
//            c.components.annotationLoader.loadPropertyBackingFieldAnnotations(protoContainer, propertyProto)
//        val delegateFieldAnnotations =
//            c.components.annotationLoader.loadPropertyDelegateFieldAnnotations(protoContainer, propertyProto)
//        val allAnnotations =
//            propertyAnnotations.map { AnnotationWithTarget(it, null) } +
//                    backingFieldAnnotations.map { AnnotationWithTarget(it, AnnotationUseSiteTarget.FIELD) } +
//                    delegateFieldAnnotations.map { AnnotationWithTarget(it, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD) }
        createTargetedAnnotationStubs(emptyList(), modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val callableName = c.nameResolver.getName(propertyProto.name)

        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // This info is anyway irrelevant for the purposes these stubs are used
        return CangJiePropertyStubImpl(
            parent,
            callableName.ref(),


            hasReturnTypeRef = true,
            fqName = c.containerFqName.child(callableName),
            isExtension = propertyProto.hasReceiver(),

            )
    }

    override fun createCallableSpecialParts() {

        val flags = propertyProto.flags
        if (Flags.HAS_GETTER[flags] && propertyProto.hasGetterFlags()) {
            val getterFlags = propertyProto.getterFlags
            if (Flags.IS_NOT_DEFAULT.get(getterFlags)) {
                createModifierListAndAnnotationStubsForAccessor(
                    CangJiePropertyAccessorStubImpl(callableStub, true, false, true),
                    flags = getterFlags,
                    callableKind = AnnotatedCallableKind.PROPERTY_GETTER
                )
            }
        }

        if (Flags.HAS_SETTER[flags] && propertyProto.hasSetterFlags()) {
            val setterFlags = propertyProto.setterFlags
            if (Flags.IS_NOT_DEFAULT.get(setterFlags)) {
                val setterStub = CangJiePropertyAccessorStubImpl(callableStub, false, true, true)
                createModifierListAndAnnotationStubsForAccessor(
                    setterStub,
                    flags = setterFlags,
                    callableKind = AnnotatedCallableKind.PROPERTY_SETTER
                )
                if (propertyProto.hasSetterValueParameter()) {
                    typeStubBuilder.createValueParameterListStub(
                        setterStub,
                        propertyProto,
                        listOf(propertyProto.setterValueParameter),
                        protoContainer,
                        AnnotatedCallableKind.PROPERTY_SETTER
                    )
                }
            }
        }
    }

    private fun createModifierListAndAnnotationStubsForAccessor(
        accessorStub: CangJiePropertyAccessorStubImpl,
        flags: Int,
        callableKind: AnnotatedCallableKind
    ) {
        val modifierList = createModifierListStubForDeclaration(
            accessorStub,
            flags,
            listOf(VISIBILITY, MODALITY)
        )
        if (Flags.HAS_ANNOTATIONS.get(flags)) {
            val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
                protoContainer,
                propertyProto,
                callableKind
            )
            createAnnotationStubs(annotationIds, modifierList)
        }
    }


}
fun createConstructorStub(
    parentStub: StubElement<out PsiElement>,
    constructorProto: ProtoBuf.Constructor,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer
) {
    ConstructorClsStubBuilder(parentStub, outerContext, protoContainer, constructorProto).build()
}
private class ConstructorClsStubBuilder(
    parent: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    private val constructorProto: ProtoBuf.Constructor
) : CallableClsStubBuilder(parent, outerContext, protoContainer, emptyList()) {
    override val receiverType: ProtoBuf.Type?
        get() = null

    override val receiverAnnotations: List<AnnotationWithTarget>
        get() = emptyList()

    override val returnType: ProtoBuf.Type?
        get() = null

    override val contextReceiverTypes: List<ProtoBuf.Type>
        get() = emptyList()

    override fun createValueParameterList() {
        typeStubBuilder.createValueParameterListStub(callableStub, constructorProto, constructorProto.valueParameterList, protoContainer)
    }

    override fun createModifierListStub() {
        val modifierListStubImpl = createModifierListStubForDeclaration(callableStub, constructorProto.flags, listOf(VISIBILITY))

        // If constructor is marked as having no annotations, we don't create stubs for it
        if (!Flags.HAS_ANNOTATIONS.get(constructorProto.flags)) return

        val annotationIds = c.components.annotationLoader.loadCallableAnnotations(
            protoContainer, constructorProto, AnnotatedCallableKind.FUNCTION
        )
        createAnnotationStubs(annotationIds, modifierListStubImpl)
    }

    override fun doCreateCallableStub(parent: StubElement<out PsiElement>): StubElement<out PsiElement> {
        val name = (protoContainer as ProtoContainer.Class).classId.shortClassName.ref()
        // Note that arguments passed to stubs here and elsewhere are based on what stabs would be generated based on decompiled code
        // As decompiled code for secondary constructor would be just constructor(args) { /* compiled code */ } every secondary constructor
        // delegated call is not to this (as there is no this keyword) and it has body (while primary does not have one)
        // This info is anyway irrelevant for the purposes these stubs are used
        return if (Flags.IS_SECONDARY.get(constructorProto.flags))
            CangJieConstructorStubImpl(
                parent, CjStubElementTypes.SECONDARY_CONSTRUCTOR, name, hasBody = true,
                isDelegatedCallToThis = false,

            )
        else
            CangJieConstructorStubImpl(
                parent, CjStubElementTypes.PRIMARY_CONSTRUCTOR, name, hasBody = false,
                isDelegatedCallToThis = false,

            )
    }
}

//open class AnnotationMemberDefaultValueVisitor : CangJieBinaryClass.AnnotationArgumentVisitor {
//    protected val args = mutableMapOf<Name, ConstantValue<*>>()
//
//    private fun nameOrSpecial(name: Name?): Name {
//        return name ?: Name.special("<no_name>")
//    }
//
//    override fun visit(name: Name?, value: Any?) {
//        val constantValue = createConstantValue(value)
//        args[nameOrSpecial(name)] = constantValue
//    }
//
//    override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
//        args[nameOrSpecial(name)] = createConstantValue(KClassData(value.classId, value.arrayNestedness))
//    }
//
//    override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
//        args[nameOrSpecial(name)] = createConstantValue(EnumData(enumClassId, enumEntryName))
//    }
//
//    override fun visitAnnotation(
//        name: Name?,
//        classId: ClassId
//    ): CangJieJvmBinaryClass.AnnotationArgumentVisitor? {
//        val visitor = AnnotationMemberDefaultValueVisitor()
//        return object : CangJieJvmBinaryClass.AnnotationArgumentVisitor by visitor {
//            override fun visitEnd() {
//                args[nameOrSpecial(name)] = createConstantValue(AnnotationData(classId, visitor.args))
//            }
//        }
//    }
//
//    override fun visitArray(name: Name?): CangJieJvmBinaryClass.AnnotationArrayArgumentVisitor? {
//        return object : CangJieJvmBinaryClass.AnnotationArrayArgumentVisitor {
//            private val elements = mutableListOf<Any>()
//
//            override fun visit(value: Any?) {
//                elements.addIfNotNull(value)
//            }
//
//            override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
//                elements.add(EnumData(enumClassId, enumEntryName))
//            }
//
//            override fun visitClassLiteral(value: ClassLiteralValue) {
//                elements.add(KClassData(value.classId, value.arrayNestedness))
//            }
//
//            override fun visitAnnotation(classId: ClassId): CangJieJvmBinaryClass.AnnotationArgumentVisitor {
//                val visitor = AnnotationMemberDefaultValueVisitor()
//                return object : CangJieJvmBinaryClass.AnnotationArgumentVisitor by visitor {
//                    override fun visitEnd() {
//                        elements.addIfNotNull(AnnotationData(classId, visitor.args))
//                    }
//                }
//            }
//
//            override fun visitEnd() {
//                args[nameOrSpecial(name)] = createConstantValue(elements.toTypedArray())
//            }
//        }
//    }
//
//    override fun visitEnd() {}
//}
