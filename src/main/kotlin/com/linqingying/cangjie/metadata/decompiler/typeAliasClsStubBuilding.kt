package com.linqingying.cangjie.metadata.decompiler

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.VISIBILITY
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.deserialization.underlyingType
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import com.linqingying.cangjie.serialization.deserialization.ProtoContainer
import com.linqingying.cangjie.serialization.deserialization.getName
fun createTypeAliasStub(
    parent: StubElement<out PsiElement>,
    typeAliasProto: ProtoBuf.TypeAlias,
    protoContainer: ProtoContainer,
    outerContext: ClsStubBuilderContext
) {
    val c = outerContext.child(typeAliasProto.typeParameterList)
    val shortName = c.nameResolver.getName(typeAliasProto.name)

    val classId = when (protoContainer) {
        is ProtoContainer.Class -> protoContainer.classId.createNestedClassId(shortName)
        is ProtoContainer.Package -> ClassId.topLevel(protoContainer.fqName.child(shortName))
    }

    val typeAlias = CangJieTypeAliasStubImpl(
        parent, classId.shortClassName.ref(), classId.asSingleFqName().ref(), classId,
//        isTopLevel = !classId.isNestedClass
    )

    val modifierList = createModifierListStubForDeclaration(typeAlias, typeAliasProto.flags, arrayListOf(VISIBILITY), listOf())

    val typeStubBuilder = TypeClsStubBuilder(c)
    val restConstraints = typeStubBuilder.createTypeParameterListStub(typeAlias, typeAliasProto.typeParameterList)
    assert(restConstraints.isEmpty()) {
        "'where' constraints are not allowed for type aliases"
    }

    if (Flags.HAS_ANNOTATIONS.get(typeAliasProto.flags)) {
        createAnnotationStubs(
            typeAliasProto.annotationList.map {
                c.components.annotationLoader.loadAnnotation(it, c.nameResolver)
            },
            modifierList
        )
    }

    val typeAliasUnderlyingType = typeAliasProto.underlyingType(c.typeTable)
    typeStubBuilder.createTypeReferenceStub(typeAlias, typeAliasUnderlyingType)
}
