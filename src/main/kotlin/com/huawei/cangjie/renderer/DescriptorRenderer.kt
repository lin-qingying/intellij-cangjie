package com.huawei.cangjie.renderer

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import java.lang.reflect.Modifier
import kotlin.jvm.internal.PropertyReference1Impl
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty

abstract class DescriptorRenderer {
    abstract fun renderType(type: CangJieType): String
    abstract fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String
    abstract fun render(declarationDescriptor: DeclarationDescriptor): String
    abstract fun renderName(name: Name, rootRenderedElement: Boolean): String
    abstract fun renderFqName(fqName: FqNameUnsafe): String

    fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
        val options = (this as DescriptorRendererImpl).options.copy()
        options.changeOptions()
        options.lock()
        return DescriptorRendererImpl(options)
    }

    companion object {
        @JvmField
        val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
//            modifiers = emptySet()
        }
        fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

        @JvmField
        val DEBUG_TEXT: DescriptorRenderer = withOptions {
//           debugMode = true
//           classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
//           modifiers = DescriptorRendererModifier.ALL
        }
    }
}

enum class RenderingFormat {
    PLAIN {
        override fun escape(string: String) = string
    },
    HTML {
        override fun escape(string: String) = string.replace("<", "&lt;").replace(">", "&gt;")
    };

    abstract fun escape(string: String): String
}

interface DescriptorRendererOptions {
    var classifierNamePolicy: ClassifierNamePolicy
//    var modifiers: Set<DescriptorRendererModifier>

    //    var modifiers: Set<DescriptorRendererModifier>
//
//    var debugMode: Boolean
    var withDefinedIn: Boolean
    var textFormat: RenderingFormat

    var boldOnlyForNamesInHtml: Boolean

}

internal class DescriptorRendererOptionsImpl : DescriptorRendererOptions {
    var isLocked: Boolean = false
        private set

    private fun <T> property(initialValue: T): ReadWriteProperty<DescriptorRendererOptionsImpl, T> {
        return Delegates.vetoable(initialValue) { _, _, _ ->
            if (isLocked) {
                throw IllegalStateException("Cannot modify readonly DescriptorRendererOptions")
            } else {
                true
            }
        }
    }

    fun lock() {
        assert(!isLocked)
        isLocked = true
    }

    fun copy(): DescriptorRendererOptionsImpl {
        val copy = DescriptorRendererOptionsImpl()

        //TODO: use Kotlin reflection
        for (field in this::class.java.declaredFields) {
            if (field.modifiers.and(Modifier.STATIC) != 0) continue
            field.isAccessible = true
            val property = field.get(this) as? ObservableProperty<*> ?: continue
            assert(!field.name.startsWith("is")) { "Fields named is* are not supported here yet" }
            val value = property.getValue(
                this,
                PropertyReference1Impl(
                    DescriptorRendererOptionsImpl::class,
                    field.name,
                    "get" + field.name.replaceFirstChar(Char::uppercaseChar)
                )
            )
            field.set(copy, copy.property(value))
        }

        return copy
    }

    override var classifierNamePolicy: ClassifierNamePolicy by property(ClassifierNamePolicy.SOURCE_CODE_QUALIFIED)

    override var withDefinedIn by property(true)
    override var textFormat by property(RenderingFormat.PLAIN)

    override var boldOnlyForNamesInHtml: Boolean by property(false)


}

internal class DescriptorRendererImpl(
    val options: DescriptorRendererOptionsImpl
) : DescriptorRenderer(), DescriptorRendererOptions by options/* this gives access to options without qualifier */ {
    init {
        assert(options.isLocked)
    }

    /* TYPES RENDERING */
    override fun renderType(type: CangJieType): String = buildString {
//        renderNormalizedType(typeNormalizer(type))
    }

    override fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String {
        if (typeStringsDifferOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "($lowerRendered)!"
            }
            return "$lowerRendered!"
        }

//        val canngjieCollectionsPrefix = classifierNamePolicy.renderClassifier(builtIns.collection, this).substringBefore("Collection")
//        val mutablePrefix = "Mutable"
//        // java.util.List<Foo> -> (Mutable)List<Foo!>!
//        val simpleCollection = replacePrefixesInTypeRepresentations(
//            lowerRendered,
//            canngjieCollectionsPrefix + mutablePrefix,
//            upperRendered,
//            canngjieCollectionsPrefix,
//            "$canngjieCollectionsPrefix($mutablePrefix)"
//        )
//        if (simpleCollection != null) return simpleCollection
//        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
//        val mutableEntry = replacePrefixesInTypeRepresentations(
//            lowerRendered,
//            canngjieCollectionsPrefix + "MutableMap.MutableEntry",
//            upperRendered,
//            canngjieCollectionsPrefix + "Map.Entry",
//            "$canngjieCollectionsPrefix(Mutable)Map.(Mutable)Entry"
//        )
//        if (mutableEntry != null) return mutableEntry
//
//        val cangjiePrefix = classifierNamePolicy.renderClassifier(builtIns.array, this).substringBefore("Array")
//        // Foo[] -> Array<(out) Foo!>!
//        val array = replacePrefixesInTypeRepresentations(
//            lowerRendered,
//            cangjiePrefix + escape("Array<"),
//            upperRendered,
//            cangjiePrefix + escape("Array<out "),
//            cangjiePrefix + escape("Array<(out) ")
//        )
//        if (array != null) return array

        return "($lowerRendered..$upperRendered)"
    }

    override fun render(declarationDescriptor: DeclarationDescriptor): String {
        return buildString {
            declarationDescriptor.accept(RenderDeclarationDescriptorVisitor(), this)

            if (withDefinedIn) {
                appendDefinedIn(declarationDescriptor)
            }
        }
    }

    /* NAMES RENDERING */
    override fun renderName(name: Name, rootRenderedElement: Boolean): String {
        val escaped = escape(name.render())
        return if (boldOnlyForNamesInHtml && textFormat == RenderingFormat.HTML && rootRenderedElement) {
            "<b>$escaped</b>"
        } else
            escaped
    }

    private fun escape(string: String) = textFormat.escape(string)

    private fun renderFqName(pathSegments: List<Name>) = escape(com.huawei.cangjie.renderer.renderFqName(pathSegments))

    override fun renderFqName(fqName: FqNameUnsafe) = renderFqName(fqName.pathSegments())


    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private fun StringBuilder.appendDefinedIn(descriptor: DeclarationDescriptor) {
        if (descriptor is PackageFragmentDescriptor || descriptor is PackageViewDescriptor) {
            return
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration != null && containingDeclaration !is ModuleDescriptor) {
            append(" ")
//                .append(renderMessage("defined in"))
                .append(" ")
//            val fqName = DescriptorUtils.getFqName(containingDeclaration)
//            append(if (fqName.isRoot) "root package" else renderFqName(fqName))

            if (
//                withSourceFileForTopLevel &&
                containingDeclaration is PackageFragmentDescriptor &&
                descriptor is DeclarationDescriptorWithSource
            ) {
                descriptor.source.containingFile.name?.let { sourceFileName ->
                    append(" ")
//                        .append(renderMessage("in file"))
                        .append(" ").append(sourceFileName)
                }
            }
        }
    }

    //    override fun renderMessage(message: String): String = when (textFormat) {
//        RenderingFormat.PLAIN -> message
//        RenderingFormat.HTML -> "<i>$message</i>"
//    }
    /* STUPID DISPATCH-ONLY VISITOR */
    private inner class RenderDeclarationDescriptorVisitor : DeclarationDescriptorVisitor<Unit, StringBuilder> {
//        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, builder: StringBuilder) {
//            renderValueParameter(descriptor, true, builder, true)
//        }
//
//        override fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: StringBuilder) {
//            renderVariable(descriptor, true, builder, true)
//        }
//
//        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: StringBuilder) {
//            renderProperty(descriptor, builder)
//        }
//
//        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, builder: StringBuilder) {
//            visitPropertyAccessorDescriptor(descriptor, builder, "getter")
//        }
//
//        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, builder: StringBuilder) {
//            visitPropertyAccessorDescriptor(descriptor, builder, "setter")
//        }
//
//        private fun visitPropertyAccessorDescriptor(descriptor: PropertyAccessorDescriptor, builder: StringBuilder, kind: String) {
//            when (propertyAccessorRenderingPolicy) {
//                PropertyAccessorRenderingPolicy.PRETTY -> {
//                    renderAccessorModifiers(descriptor, builder)
//                    builder.append("$kind for ")
//                    renderProperty(descriptor.correspondingProperty, builder)
//                }
//                PropertyAccessorRenderingPolicy.DEBUG -> {
//                    visitFunctionDescriptor(descriptor, builder)
//                }
//                PropertyAccessorRenderingPolicy.NONE -> {
//                }
//            }
//        }
//
//        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: StringBuilder) {
//            renderFunction(descriptor, builder)
//        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, builder: StringBuilder) {
            builder.append(descriptor.name) // renders <this>
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: StringBuilder?) {
            //    TODO("Not yet implemented")
        }

        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: StringBuilder?) {
            //     TODO("Not yet implemented")
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: StringBuilder?) {
            //    TODO("Not yet implemented")
        }

        override fun visitVariableDescriptor(descriptor: VariableDescriptor?, data: StringBuilder?) {
            //    TODO("Not yet implemented")
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: StringBuilder?) {
            //   TODO("Not yet implemented")
        }

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: StringBuilder) {
//            TODO("Not yet implemented")
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor?, data: StringBuilder?) {
//            TODO("Not yet implemented")
        }

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: StringBuilder) {
            TODO("Not yet implemented")
        }
//
//        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, builder: StringBuilder) {
//            renderConstructor(constructorDescriptor, builder)
//        }
//
//        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: StringBuilder) {
//            renderTypeParameter(descriptor, builder, true)
//        }
//
//        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, builder: StringBuilder) {
//            renderPackageFragment(descriptor, builder)
//        }
//
//        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: StringBuilder) {
//            renderPackageView(descriptor, builder)
//        }
//
//        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: StringBuilder) {
//            renderName(descriptor, builder, true)
//        }
//
//        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, builder: StringBuilder) {
//            visitClassDescriptor(scriptDescriptor, builder)
//        }
//
//        override fun visitClassDescriptor(descriptor: ClassDescriptor, builder: StringBuilder) {
//            renderClass(descriptor, builder)
//        }
//
//        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: StringBuilder) {
//            renderTypeAlias(descriptor, builder)
//        }
    }

    private fun StringBuilder.renderNormalizedType(type: CangJieType) {
//        val abbreviated = type.unwrap() as? AbbreviatedType
//        if (abbreviated != null) {
//            if (renderTypeExpansions) {
//                renderNormalizedTypeAsIs(abbreviated.expandedType)
//            } else {
//                // TODO nullability is lost for abbreviated type?
//                renderNormalizedTypeAsIs(abbreviated.abbreviation)
//                if (renderUnabbreviatedType) {
//                    renderAbbreviatedTypeExpansion(abbreviated)
//                }
//            }
//            return
//        }
//
//        renderNormalizedTypeAsIs(type)
    }

}
