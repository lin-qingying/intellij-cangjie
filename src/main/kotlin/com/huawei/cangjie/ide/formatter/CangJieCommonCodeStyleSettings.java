
package com.huawei.cangjie.ide.formatter;


import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.utils.ReflectionUtil;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleProvider;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.util.xmlb.XmlSerializer;
import kotlin.collections.ArraysKt;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class CangJieCommonCodeStyleSettings extends CommonCodeStyleSettings {
    @ReflectionUtil.SkipInEquals
    public String CODE_STYLE_DEFAULTS = null;

    private final boolean isTempForDeserialize;

    public CangJieCommonCodeStyleSettings() {
        this(false);
    }

    private CangJieCommonCodeStyleSettings(boolean isTempForDeserialize) {
        super(CangJieLanguage.INSTANCE);
        this.isTempForDeserialize = isTempForDeserialize;
    }

    private static CangJieCommonCodeStyleSettings createForTempDeserialize() {
        return new CangJieCommonCodeStyleSettings(true);
    }

    @Override
    public void readExternal(Element element) {
        if (isTempForDeserialize) {
            super.readExternal(element);
            return;
        }

        CangJieCommonCodeStyleSettings tempDeserialize = createForTempDeserialize();
        tempDeserialize.readExternal(element);

        FormatterUtilKt.applyCangJieCodeStyle(tempDeserialize.CODE_STYLE_DEFAULTS, this, true);

        super.readExternal(element);
    }
//
//    @Override
//    public void writeExternal(@NotNull Element element, @NotNull LanguageCodeStyleProvider provider) {
//        CommonCodeStyleSettings defaultSettings = provider.getDefaultCommonSettings();
//        FormatterUtilKt.applyCangJieCodeStyle(CODE_STYLE_DEFAULTS, defaultSettings, false);
//
//        writeExternalBase(element, defaultSettings, provider);
//    }
//
//
//    private void writeExternalBase(
//            @NotNull Element element,
//            @NotNull CommonCodeStyleSettings defaultSettings,
//            @NotNull LanguageCodeStyleProvider provider
//    ) {
//        Set<String> supportedFields = provider.getSupportedFields();
//        if (supportedFields != null) {
//            supportedFields.add("FORCE_REARRANGE_MODE");
//            supportedFields.add("CODE_STYLE_DEFAULTS");
//        } else {
//            return;
//        }
//
//
//        DefaultJDOMExternalizer.write(this, element, new SupportedFieldsDiffFilter(this, supportedFields, defaultSettings));
//        List<Integer> softMargins = getSoftMargins();
//        serializeInto(softMargins, element);
//
//        IndentOptions myIndentOptions = getIndentOptions();
//        if (myIndentOptions != null) {
//            IndentOptions defaultIndentOptions = defaultSettings.getIndentOptions();
//            Element indentOptionsElement = new Element(INDENT_OPTIONS_TAG);
//            myIndentOptions.serialize(indentOptionsElement, defaultIndentOptions);
//            if (!indentOptionsElement.getChildren().isEmpty()) {
//                element.addContent(indentOptionsElement);
//            }
//        }
//
//        ArrangementSettings myArrangementSettings = getArrangementSettings();
//        if (myArrangementSettings != null) {
//            Element container = new Element(ARRANGEMENT_ELEMENT_NAME);
//            ArrangementUtil.writeExternal(container, myArrangementSettings, provider.getLanguage());
//            if (!container.getChildren().isEmpty()) {
//                element.addContent(container);
//            }
//        }
//    }

    @Override
    public CommonCodeStyleSettings clone(@NotNull CodeStyleSettings rootSettings) {
        CangJieCommonCodeStyleSettings commonSettings = new CangJieCommonCodeStyleSettings();
        copyPublicFields(this, commonSettings);

        try {
            Method setRootSettingsMethod = CommonCodeStyleSettings.class.getDeclaredMethod("setRootSettings", CodeStyleSettings.class);
            setRootSettingsMethod.setAccessible(true);
            setRootSettingsMethod.invoke(commonSettings, rootSettings);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        commonSettings.setForceArrangeMenuAvailable(isForceArrangeMenuAvailable());

        IndentOptions indentOptions = getIndentOptions();
        if (indentOptions != null) {
            IndentOptions targetIndentOptions = commonSettings.initIndentOptions();
            targetIndentOptions.copyFrom(indentOptions);
        }

        ArrangementSettings arrangementSettings = getArrangementSettings();
        if (arrangementSettings != null) {
            commonSettings.setArrangementSettings(arrangementSettings.clone());
        }

        try {
            Method setRootSettingsMethod = ArraysKt.singleOrNull(
                    CommonCodeStyleSettings.class.getDeclaredMethods(),
                    method -> "setSoftMargins".equals(method.getName()));

            if (setRootSettingsMethod != null) {

                setRootSettingsMethod.setAccessible(true);
                setRootSettingsMethod.invoke(commonSettings, getSoftMargins());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

        return commonSettings;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CangJieCommonCodeStyleSettings)) {
            return false;
        }

        if (!ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)) {
            return false;
        }

        CommonCodeStyleSettings other = (CommonCodeStyleSettings) obj;
        if (!getSoftMargins().equals(other.getSoftMargins())) {
            return false;
        }

        IndentOptions options = getIndentOptions();
        if ((options == null && other.getIndentOptions() != null) ||
            (options != null && !options.equals(other.getIndentOptions()))) {
            return false;
        }

        return arrangementSettingsEqual(other);
    }


    private void serializeInto(@NotNull List<Integer> softMargins, @NotNull Element element) {
        if (!softMargins.isEmpty()) {
            XmlSerializer.serializeInto(this, element);
        }
    }



    private static final String INDENT_OPTIONS_TAG = "indentOptions";
    private static final String ARRANGEMENT_ELEMENT_NAME = "arrangement";

}
