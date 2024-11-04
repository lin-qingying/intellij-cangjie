package com.linqingying.cangjie.ide.refactoring


import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * @see CangJieCommonRefactoringSettings
 */
@Service(Service.Level.APP)
@State(
    name = "CangJieRefactoringSettings",
    storages = [Storage("cangjieRefactoring.xml")],
    category = SettingsCategory.CODE
)
class CangJieRefactoringSettings : PersistentStateComponent<CangJieRefactoringSettings> {
    @JvmField
    var MOVE_TO_UPPER_LEVEL_SEARCH_IN_COMMENTS = false

    @JvmField
    var MOVE_TO_UPPER_LEVEL_SEARCH_FOR_TEXT = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_PACKAGE = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_CLASS = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_METHOD = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_FIELD = false

    @JvmField
    var RENAME_SEARCH_IN_COMMENTS_FOR_VARIABLE = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_PACKAGE = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_CLASS = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_METHOD = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_FIELD = false

    @JvmField
    var RENAME_SEARCH_FOR_TEXT_FOR_VARIABLE = false

    @JvmField
    var MOVE_PREVIEW_USAGES = true

    @JvmField
    var MOVE_SEARCH_IN_COMMENTS = true

    @JvmField
    var MOVE_SEARCH_FOR_TEXT = true

    @JvmField
    var MOVE_SEARCH_REFERENCES = true

    @JvmField
    var MOVE_DELETE_EMPTY_SOURCE_FILES = true

    @JvmField
    var MOVE_MPP_DECLARATIONS = true

    @JvmField
    var EXTRACT_INTERFACE_JAVADOC: Int = 0

    @JvmField
    var EXTRACT_SUPERCLASS_JAVADOC: Int = 0

    @JvmField
    var PULL_UP_MEMBERS_JAVADOC: Int = 0

    @JvmField
    var PUSH_DOWN_PREVIEW_USAGES: Boolean = false

    var INLINE_METHOD_THIS: Boolean = false
    var INLINE_LOCAL_THIS: Boolean = false
    var INLINE_TYPE_ALIAS_THIS: Boolean = false
    var INLINE_METHOD_KEEP: Boolean = false
    var INLINE_PROPERTY_KEEP: Boolean = false
    var INLINE_TYPE_ALIAS_KEEP: Boolean = false

    var renameInheritors = true
    var renameParameterInHierarchy = true
    var renameFileNames = true
    var renameVariables = true
    var renameTests = true
    var renameOverloads = true
    var renameTestMethods = true

    var INTRODUCE_DECLARE_WITH_VAR = false
    var INTRODUCE_SPECIFY_TYPE_EXPLICITLY = false

    override fun getState() = this

    override fun loadState(state: CangJieRefactoringSettings) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        @JvmStatic
        val instance: CangJieRefactoringSettings
            get() = service()
    }
}
