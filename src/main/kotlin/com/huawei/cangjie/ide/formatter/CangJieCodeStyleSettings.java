

package com.huawei.cangjie.ide.formatter;

import com.huawei.cangjie.utils.ReflectionUtil;
import com.intellij.configurationStore.Property;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class CangJieCodeStyleSettings extends CustomCodeStyleSettings {
    @NotNull
    @ReflectionUtil.SkipInEquals
    @Property(externalName = "packages_to_use_import_on_demand")
    public CangJiePackageEntryTable PACKAGES_TO_USE_STAR_IMPORTS = new CangJiePackageEntryTable();

    @NotNull
    @ReflectionUtil.SkipInEquals
    @Property(externalName = "imports_layout")
    public CangJiePackageEntryTable PACKAGES_IMPORT_LAYOUT = new CangJiePackageEntryTable();

    public static final int DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT = 5;
    public static final int DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = 3;

    public boolean SPACE_AROUND_RANGE = false;
    public boolean SPACE_BEFORE_TYPE_COLON = false;
    public boolean SPACE_AFTER_TYPE_COLON = true;
    public boolean SPACE_BEFORE_EXTEND_COLON = true;
    public boolean SPACE_AFTER_EXTEND_COLON = true;
    public boolean INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true;
    public boolean ALIGN_IN_COLUMNS_CASE_BRANCH = false;
    public boolean LINE_BREAK_AFTER_MULTILINE_MATCH_ENTRY = true;
    public boolean SPACE_AROUND_FUNCTION_TYPE_ARROW = true;
    public boolean SPACE_AROUND_MATCH_ARROW = true;
    public boolean SPACE_BEFORE_LAMBDA_ARROW = true;
    public boolean SPACE_BEFORE_MATCH_PARENTHESES = true;
    public boolean LBRACE_ON_NEXT_LINE = false;
    public int NAME_COUNT_TO_USE_STAR_IMPORT = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT;
    public int NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : DEFAULT_NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS;
    public boolean IMPORT_NESTED_CLASSES = false;
    public boolean CONTINUATION_INDENT_IN_PARAMETER_LISTS = true;
    public boolean CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true;
    public boolean CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true;
    public boolean CONTINUATION_INDENT_FOR_CHAINED_CALLS = true;
    public boolean CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true;
    public boolean CONTINUATION_INDENT_IN_IF_CONDITIONS = true;
    public boolean CONTINUATION_INDENT_IN_ELVIS = true;
    public int BLANK_LINES_AROUND_BLOCK_MATCH_BRANCHES = 0;
    public int WRAP_EXPRESSION_BODY_FUNCTIONS = 0;
    public int WRAP_ELVIS_EXPRESSIONS = 1;
    public boolean IF_RPAREN_ON_NEW_LINE = false;
    public boolean ALLOW_TRAILING_COMMA = false;
    public boolean ALLOW_TRAILING_COMMA_ON_CALL_SITE = false;
    public int BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE = 1;

    @ReflectionUtil.SkipInEquals
    public String CODE_STYLE_DEFAULTS = null;

    private final boolean isTempForDeserialize;

    public CangJieCodeStyleSettings(CodeStyleSettings container) {
        this(container, false);
    }

    private CangJieCodeStyleSettings(@NotNull CodeStyleSettings container, boolean isTempForDeserialize) {
        super("JetCodeStyleSettings", container);

        this.isTempForDeserialize = isTempForDeserialize;


//        if (!ApplicationManager.getApplication().isUnitTestMode()) {
//            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new CangJiePackageEntry("java.util", false));
//            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new CangJiePackageEntry("kotlinx.android.synthetic", true));
//            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new CangJiePackageEntry("io.ktor", true));
//        }
//
//
//        PACKAGES_IMPORT_LAYOUT.addEntry(CangJiePackageEntry.ALL_OTHER_IMPORTS_ENTRY);
//        PACKAGES_IMPORT_LAYOUT.addEntry(new CangJiePackageEntry("java", true));
//        PACKAGES_IMPORT_LAYOUT.addEntry(new CangJiePackageEntry("javax", true));
//        PACKAGES_IMPORT_LAYOUT.addEntry(new CangJiePackageEntry("kotlin", true));
//        PACKAGES_IMPORT_LAYOUT.addEntry(CangJiePackageEntry.ALL_OTHER_ALIAS_IMPORTS_ENTRY);
    }

    @Override
    public Object clone() {
        CangJieCodeStyleSettings clone = (CangJieCodeStyleSettings) super.clone();

        clone.PACKAGES_TO_USE_STAR_IMPORTS = new CangJiePackageEntryTable();
        clone.PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(this.PACKAGES_TO_USE_STAR_IMPORTS);

        clone.PACKAGES_IMPORT_LAYOUT = new CangJiePackageEntryTable();
        clone.PACKAGES_IMPORT_LAYOUT.copyFrom(this.PACKAGES_IMPORT_LAYOUT);

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CangJieCodeStyleSettings that)) return false;

        if (!Comparing.equal(PACKAGES_TO_USE_STAR_IMPORTS, that.PACKAGES_TO_USE_STAR_IMPORTS)) return false;
        if (!Comparing.equal(PACKAGES_IMPORT_LAYOUT, that.PACKAGES_IMPORT_LAYOUT)) return false;
        return ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, that);
    }

    @Override
    public void writeExternal(Element parentElement, @NotNull CustomCodeStyleSettings parentSettings) throws WriteExternalException {
        if (CODE_STYLE_DEFAULTS != null) {
            CangJieCodeStyleSettings defaultCangJieCodeStyle = (CangJieCodeStyleSettings) parentSettings.clone();

            FormatterUtilKt.applyCangJieCodeStyle(CODE_STYLE_DEFAULTS, defaultCangJieCodeStyle, false);

            parentSettings = defaultCangJieCodeStyle;
        }

        super.writeExternal(parentElement, parentSettings);
    }

    @Override
    public void readExternal(Element parentElement) throws InvalidDataException {
        if (isTempForDeserialize) {
            super.readExternal(parentElement);
            return;
        }

        CangJieCodeStyleSettings tempSettings = readExternalToTemp(parentElement);
        String customDefaults = tempSettings.CODE_STYLE_DEFAULTS;

        FormatterUtilKt.applyCangJieCodeStyle(customDefaults, this, true);


        super.readExternal(parentElement);
    }

    private static CangJieCodeStyleSettings readExternalToTemp(Element parentElement) {

        CangJieCodeStyleSettings tempSettings = new CangJieCodeStyleSettings(CodeStyleSettings.getDefaults(), true);
        tempSettings.readExternal(parentElement);

        return tempSettings;
    }
}
