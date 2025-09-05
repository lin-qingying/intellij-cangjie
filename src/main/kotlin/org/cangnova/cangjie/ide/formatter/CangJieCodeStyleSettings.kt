package org.cangnova.cangjie.ide.formatter

import org.cangnova.cangjie.utils.ReflectionUtil
import com.intellij.configurationStore.Property
import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import org.jdom.Element

class CangJieCodeStyleSettings(
    container: CodeStyleSettings,
    val isTempForDeserialize: Boolean = false
) : CustomCodeStyleSettings("CangJieCodeStyleSettings", container) {


    companion object {
        public val DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT = 5
        public val DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = 3

        private fun readExternalToTemp(parentElement: Element): CangJieCodeStyleSettings {
            val tempSettings = CangJieCodeStyleSettings(CodeStyleSettings.getDefaults(), true)
            tempSettings.readExternal(parentElement)
            return tempSettings
        }
    }

    @ReflectionUtil.SkipInEquals
    @JvmField
    var CODE_STYLE_DEFAULTS: String? = null

    @ReflectionUtil.SkipInEquals
    @Property(externalName = "packages_to_use_import_on_demand")
    @JvmField
    var PACKAGES_TO_USE_STAR_IMPORTS = CangJiePackageEntryTable()

    @ReflectionUtil.SkipInEquals
    @Property(externalName = "imports_layout")
    @JvmField
    var PACKAGES_IMPORT_LAYOUT = CangJiePackageEntryTable()

    @JvmField
    public var SPACE_AROUND_RANGE = true
    @JvmField
    public var SPACE_AROUND_UNARY_OPERATOR = true

    @JvmField
    public var SPACE_BEFORE_TYPE_COLON = false

    @JvmField
    public var SPACE_AFTER_TYPE_COLON = true

    @JvmField
    public var SPACE_BEFORE_EXTEND_COLON = true

    @JvmField
    public var SPACE_AFTER_EXTEND_COLON = true

    @JvmField
    public var INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true

    @JvmField
    public var ALIGN_IN_COLUMNS_CASE_BRANCH = false

    @JvmField
    public var LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY = true

    @JvmField
    public var SPACE_AROUND_FUNCTION_TYPE_ARROW = true

    @JvmField
    public var SPACE_AROUND_MATCH_ARROW = true

    @JvmField
    public var SPACE_BEFORE_LAMBDA_ARROW = true

    @JvmField
    public var SPACE_BEFORE_MATCH_PARENTHESES = true

    @JvmField
    public var LBRACE_ON_NEXT_LINE = false

    @JvmField
    public var NAME_COUNT_TO_USE_STAR_IMPORT =
        if (isUnitTestMode) Integer.MAX_VALUE else DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT

    @JvmField
    public var NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS =
        if (isUnitTestMode) Integer.MAX_VALUE else DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS

    @JvmField
    public var IMPORT_NESTED_CLASSES = false

    @JvmField
    public var CONTINUATION_INDENT_IN_PARAMETER_LISTS = true

    @JvmField
    public var CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true

    @JvmField
    public var CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true

    @JvmField
    var CONTINUATION_INDENT_FOR_CHAINED_CALLS = true

    @JvmField
    public var CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true

    @JvmField
    public var CONTINUATION_INDENT_IN_IF_CONDITIONS = true

    @JvmField
    public var CONTINUATION_INDENT_IN_ELVIS = true

    @JvmField
    public val BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES = 0

    @JvmField
    public var WRAP_EXPRESSION_BODY_FUNCTIONS = 0

    @JvmField
    public val WRAP_ELVIS_EXPRESSIONS = 1

    @JvmField
    public var IF_RPAREN_ON_NEW_LINE = false

    @JvmField
    public var ALLOW_TRAILING_COMMA = false

    @JvmField
    public val ALLOW_TRAILING_COMMA_ON_CALL_SITE = false

    @JvmField
    public val BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE = 1

    override fun clone(): Any {
        val clone = super.clone() as CangJieCodeStyleSettings

        clone.PACKAGES_TO_USE_STAR_IMPORTS = CangJiePackageEntryTable()
        clone.PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(this.PACKAGES_TO_USE_STAR_IMPORTS)

        clone.PACKAGES_IMPORT_LAYOUT = CangJiePackageEntryTable()
        clone.PACKAGES_IMPORT_LAYOUT.copyFrom(this.PACKAGES_IMPORT_LAYOUT)

        return clone
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is CangJieCodeStyleSettings) return false

        if (!Comparing.equal(PACKAGES_TO_USE_STAR_IMPORTS, obj.PACKAGES_TO_USE_STAR_IMPORTS)) return false
        if (!Comparing.equal(PACKAGES_IMPORT_LAYOUT, obj.PACKAGES_IMPORT_LAYOUT)) return false
        return ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(parentElement: Element, parentSettings: CustomCodeStyleSettings) {
        var parentSettings = parentSettings
        if (CODE_STYLE_DEFAULTS != null) {
            val defaultCangJieCodeStyle = parentSettings.clone() as CangJieCodeStyleSettings

            applyCangJieCodeStyle(CODE_STYLE_DEFAULTS, defaultCangJieCodeStyle, false)

            parentSettings = defaultCangJieCodeStyle
        }

        super.writeExternal(parentElement, parentSettings)
    }

    @Throws(InvalidDataException::class)
    override fun readExternal(parentElement: Element) {
        if (isTempForDeserialize) {
            super.readExternal(parentElement)
            return
        }

        val tempSettings = readExternalToTemp(parentElement)
        val customDefaults = tempSettings.CODE_STYLE_DEFAULTS

        applyCangJieCodeStyle(customDefaults, this, true)

        super.readExternal(parentElement)
    }

    override fun hashCode(): Int {
        var result = isTempForDeserialize.hashCode()
        result = 31 * result + (CODE_STYLE_DEFAULTS?.hashCode() ?: 0)
        result = 31 * result + PACKAGES_TO_USE_STAR_IMPORTS.hashCode()
        result = 31 * result + PACKAGES_IMPORT_LAYOUT.hashCode()
        result = 31 * result + SPACE_AROUND_RANGE.hashCode()
        result = 31 * result + SPACE_BEFORE_TYPE_COLON.hashCode()
        result = 31 * result + SPACE_AFTER_TYPE_COLON.hashCode()
        result = 31 * result + SPACE_BEFORE_EXTEND_COLON.hashCode()
        result = 31 * result + SPACE_AFTER_EXTEND_COLON.hashCode()
        result = 31 * result + INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD.hashCode()
        result = 31 * result + ALIGN_IN_COLUMNS_CASE_BRANCH.hashCode()
        result = 31 * result + LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY.hashCode()
        result = 31 * result + SPACE_AROUND_FUNCTION_TYPE_ARROW.hashCode()
        result = 31 * result + SPACE_AROUND_MATCH_ARROW.hashCode()
        result = 31 * result + SPACE_BEFORE_LAMBDA_ARROW.hashCode()
        result = 31 * result + SPACE_BEFORE_MATCH_PARENTHESES.hashCode()
        result = 31 * result + LBRACE_ON_NEXT_LINE.hashCode()
        result = 31 * result + NAME_COUNT_TO_USE_STAR_IMPORT
        result = 31 * result + NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS
        result = 31 * result + IMPORT_NESTED_CLASSES.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_PARAMETER_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_ARGUMENT_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_FOR_EXPRESSION_BODIES.hashCode()
        result = 31 * result + CONTINUATION_INDENT_FOR_CHAINED_CALLS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_SUPERTYPE_LISTS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_IF_CONDITIONS.hashCode()
        result = 31 * result + CONTINUATION_INDENT_IN_ELVIS.hashCode()
        result = 31 * result + BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES
        result = 31 * result + WRAP_EXPRESSION_BODY_FUNCTIONS
        result = 31 * result + WRAP_ELVIS_EXPRESSIONS
        result = 31 * result + IF_RPAREN_ON_NEW_LINE.hashCode()
        result = 31 * result + ALLOW_TRAILING_COMMA.hashCode()
        result = 31 * result + ALLOW_TRAILING_COMMA_ON_CALL_SITE.hashCode()
        result = 31 * result + BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE
        return result
    }
}