package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.isNumberedFunctionClassFqName
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.metadata.INTERFACE_MODALITY
import com.linqingying.cangjie.metadata.MODALITY
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.ProtoBuf.Class.Kind.*
import com.linqingying.cangjie.metadata.VISIBILITY
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.CjAbstractClassBody
import com.linqingying.cangjie.psi.CjSuperTypeEntry
import com.linqingying.cangjie.psi.CjSuperTypeList
import com.linqingying.cangjie.psi.stubs.elements.*
import com.linqingying.cangjie.psi.stubs.impl.*
import com.linqingying.cangjie.serialization.deserialization.ProtoContainer
import com.linqingying.cangjie.serialization.deserialization.getClassId
import com.linqingying.cangjie.serialization.deserialization.getName

fun createClassStub(
    parent: StubElement<out PsiElement>,
    classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    classId: ClassId,
    source: SourceElement?,
    context: ClsStubBuilderContext
) {
    ClassClsStubBuilder(parent, classProto, nameResolver, classId, source, context).build()
}

private class ClassClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    private val classId: ClassId,
    source: SourceElement?,
    outerContext: ClsStubBuilderContext
) {
    private val thisAsProtoContainer = ProtoContainer.Class(
        classProto, nameResolver, TypeTable(classProto.typeTable), source, outerContext.protoContainer
    )
    private val classKind = thisAsProtoContainer.kind

    private val c = outerContext.child(
        classProto.typeParameterList,
        classId.shortClassName,
        nameResolver,
        thisAsProtoContainer.typeTable,
        thisAsProtoContainer
    )
    private val contextReceiversListStubBuilder = ContextReceiversListStubBuilder(c)
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val supertypeIds = run {
        val supertypeIds = classProto.supertypes(c.typeTable).map { c.nameResolver.getClassId(it.className) }
        //empty supertype list if single supertype is Any
        if (supertypeIds.singleOrNull()
                ?.let { StandardNames.FqNames.anyUFqName == it.asSingleFqName().toUnsafe() } == true
        ) {
            listOf()
        } else {
            supertypeIds
        }
    }

    private val companionObjectName = if (classProto.hasCompanionObjectName())
        c.nameResolver.getName(classProto.companionObjectName)
    else
        null

    private val classOrObjectStub = createClassOrObjectStubAndModifierListStub()

    fun build() {
        val typeConstraintListData =
            typeStubBuilder.createTypeParameterListStub(classOrObjectStub, classProto.typeParameterList)
        createConstructorStub()
        createDelegationSpecifierList()
        typeStubBuilder.createTypeConstraintListStub(classOrObjectStub, typeConstraintListData)
        createClassBodyAndMemberStubs()
    }

    private fun createClassOrObjectStubAndModifierListStub(): StubElement<out PsiElement> {
        val classOrObjectStub = doCreateClassOrObjectStub()
        contextReceiversListStubBuilder.createContextReceiverStubs(
            classOrObjectStub,
            classProto.contextReceiverTypes(c.typeTable)
        )
        val modifierList = createModifierListForClass(classOrObjectStub)
        if (Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
            createAnnotationStubs(
                c.components.annotationLoader.loadClassAnnotations(thisAsProtoContainer),
                modifierList
            )
        }
        return classOrObjectStub
    }

    private fun createModifierListForClass(parent: StubElement<out PsiElement>): CangJieModifierListStubImpl {
        val relevantFlags = arrayListOf(VISIBILITY)

        if (isClass()) {

            relevantFlags.add(MODALITY)

        }
        if (isInterface()) {

            relevantFlags.add(INTERFACE_MODALITY)
        }
        val additionalModifiers = when (classKind) {
//            ENUM -> listOf(CjTokens.ENUM_KEYWORD)

//            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> listOf(CjTokens.ANNOTATION_KEYWORD)
            else -> listOf<CjModifierKeywordToken>()
        }
        return createModifierListStubForDeclaration(parent, classProto.flags, relevantFlags, additionalModifiers)
    }

    private fun doCreateClassOrObjectStub(): StubElement<out PsiElement> {

        val fqName = classId.asSingleFqName()
        val shortName = fqName.shortName().ref()
        val superTypeRefs = supertypeIds.filterNot {
            //TODO: filtering function types should go away
            isNumberedFunctionClassFqName(it.asSingleFqName().toUnsafe())
        }.map { it.shortClassName.ref() }.toTypedArray()
        val classId = classId.takeUnless { it.isLocal }
        return when (classKind) {

//            else -> {
//                CangJieClassStubImpl(
//                    CjClassElementType.getStubType(classKind == ProtoBuf.Class.Kind.ENUM_ENTRY),
//                    parentStub,
//                    fqName.ref(),
//                    classId = classId,
//                    shortName,
//                    superTypeRefs,
//                    isInterface = classKind == ProtoBuf.Class.Kind.INTERFACE,
//                    isEnumEntry = classKind == ProtoBuf.Class.Kind.ENUM_ENTRY,
//                    isLocal = false,
//                    isTopLevel = !this.classId.isNestedClass,
//                )
//            }
            CLASS -> CangJieClassStubImpl(
                CjClassElementType.getStubType(),
                parentStub,
                fqName.ref(),
                classId = classId,
                shortName,
                superTypeRefs,

                isLocal = false,

                )

            INTERFACE -> CangJieInterfaceStubImpl(
                CjInterfaceElementType.getStubType(),
                parentStub,
                fqName.ref(),
                classId = classId,
                shortName,
                superTypeRefs,

                isLocal = false,

                )

            ENUM -> CangJieEnumStubImpl(
                CjEnumElementType.getStubType(),
                parentStub,
                fqName.ref(),
                classId = classId,
                shortName,
                superTypeRefs,

                isLocal = false,

                )

            ENUM_ENTRY -> CangJieEnumEntryStubImpl(
                CjEnumEntryElementType.getStubType(),
                parentStub,
                fqName.ref(),
                fqName.ref(),
                classId = classId,
                name = shortName,


                isLocal = false,
            )

            ANNOTATION_CLASS -> TODO()
            STRUCT -> CangJieStructStubImpl(
                CjStructElementType.getStubType(),
                parentStub,
                fqName.ref(),
                classId = classId,
                shortName,
                superTypeRefs,

                isLocal = false,
            )
        }
    }

    private fun createConstructorStub() {
        if (!isClass()) return

        val primaryConstructorProto = classProto.constructorList.find { !Flags.IS_SECONDARY.get(it.flags) } ?: return

        createConstructorStub(classOrObjectStub, primaryConstructorProto, c, thisAsProtoContainer)
    }

    private fun createDelegationSpecifierList() {
        // if single supertype is any then no delegation specifier list is needed
        if (supertypeIds.isEmpty()) return

        val delegationSpecifierListStub =
            CangJiePlaceHolderStubImpl<CjSuperTypeList>(classOrObjectStub, CjStubElementTypes.SUPER_TYPE_LIST)

        classProto.supertypes(c.typeTable).forEach { type ->
            val superClassStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(
                delegationSpecifierListStub, CjStubElementTypes.SUPER_TYPE_ENTRY
            )
            typeStubBuilder.createTypeReferenceStub(superClassStub, type)
        }
    }

    private fun createClassBodyAndMemberStubs() {
        val classBody =
            CangJiePlaceHolderStubImpl<CjAbstractClassBody>(classOrObjectStub, CjStubElementTypes.CLASS_BODY)
        createEnumEntryStubs(classBody)
        createCompanionObjectStub(classBody)
        createCallableMemberStubs(classBody)
        createInnerAndNestedClasses(classBody)
        createTypeAliasesStubs(classBody)
    }

    private fun createCompanionObjectStub(classBody: CangJiePlaceHolderStubImpl<CjAbstractClassBody>) {
        if (companionObjectName == null) {
            return
        }

        val companionObjectId = classId.createNestedClassId(companionObjectName)
        createNestedClassStub(classBody, companionObjectId)
    }

    private fun createEnumEntryStubs(classBody: CangJiePlaceHolderStubImpl<CjAbstractClassBody>) {
        if (classKind != ENUM) return

        classProto.enumEntryList.forEach { entry ->
            val name = c.nameResolver.getName(entry.name)
            val annotations = c.components.annotationLoader.loadEnumEntryAnnotations(thisAsProtoContainer, entry)
            val enumEntryStub = CangJieEnumEntryStubImpl(
                CjStubElementTypes.ENUM_ENTRY,
                classBody,
                c.containerFqName.child(name).ref(),
                c.containerFqName.child(name).ref(),

                classId = null, // enum entry do not have class id
                name = name.ref(),

                isLocal = false,

                )
            if (annotations.isNotEmpty()) {
                createAnnotationStubs(annotations, createEmptyModifierListStub(enumEntryStub))
            }
        }
    }

    private fun createCallableMemberStubs(classBody: CangJiePlaceHolderStubImpl<CjAbstractClassBody>) {
        for (secondaryConstructorProto in classProto.constructorList) {
            if (Flags.IS_SECONDARY.get(secondaryConstructorProto.flags)) {
                createConstructorStub(classBody, secondaryConstructorProto, c, thisAsProtoContainer)
            }
        }

        createDeclarationsStubs(classBody, c, thisAsProtoContainer, classProto.functionList, classProto.propertyList)
    }

    private fun isClass(): Boolean {
        return classKind == CLASS ||
                classKind == ProtoBuf.Class.Kind.ENUM ||
                classKind == ANNOTATION_CLASS
    }

    private fun isInterface(): Boolean {
        return classKind == INTERFACE
    }

    private fun createInnerAndNestedClasses(classBody: CangJiePlaceHolderStubImpl<CjAbstractClassBody>) {
        classProto.nestedClassNameList.forEach { id ->
            val nestedClassName = c.nameResolver.getName(id)
            if (nestedClassName != companionObjectName) {
                val nestedClassId = classId.createNestedClassId(nestedClassName)
                createNestedClassStub(classBody, nestedClassId)
            }
        }
    }

    private fun createTypeAliasesStubs(classBody: CangJiePlaceHolderStubImpl<CjAbstractClassBody>) {
        createTypeAliasesStubs(classBody, c, thisAsProtoContainer, classProto.typeAliasList)
    }

    private fun createNestedClassStub(classBody: StubElement<out PsiElement>, nestedClassId: ClassId) {
        val (nameResolver, classProto, _, sourceElement) =
            c.components.classDataFinder.findClassData(nestedClassId)
                ?: c.components.virtualFileForDebug.let { rootFile ->
                    if (LOG.isDebugEnabled) {
                        val outerClassId = nestedClassId.outerClassId
                        val sortedChildren = rootFile.parent.children.sortedBy { it.name }
                        val msgPrefix = "Could not find data for nested class $nestedClassId of class $outerClassId\n"
                        val explanation = when {
                            outerClassId != null && sortedChildren.none { it.name.startsWith("${outerClassId.relativeClassName}\$a") } ->

                                "Reason: obfuscation suspected (single-letter name)\n"

                            else ->
                                // General case
                                ""
                        }
                        val msg = msgPrefix + explanation +
                                "Root file: ${rootFile.canonicalPath}\n" +
                                "Dir: ${rootFile.parent.canonicalPath}\n" +
                                "Children:\n" +
                                sortedChildren.joinToString(separator = "\n") {
                                    "${it.name} (valid: ${it.isValid})"
                                }
                        LOG.debug(msg)
                    }
                    return
                }
        createClassStub(classBody, classProto, nameResolver, nestedClassId, sourceElement, c)
    }

    companion object {
        private val LOG = Logger.getInstance(ClassClsStubBuilder::class.java)
    }
}
