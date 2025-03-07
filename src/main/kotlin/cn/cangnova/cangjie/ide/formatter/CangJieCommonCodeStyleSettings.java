
/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.formatter;


import cn.cangnova.cangjie.lang.CangJieLanguage;
import cn.cangnova.cangjie.utils.ReflectionUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.arrangement.ArrangementSettings;
import com.intellij.util.xmlb.XmlSerializer;
import kotlin.collections.ArraysKt;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

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


//    TODO 功能是否与父类一致？
//    @Override
//    public void writeExternal(@NotNull Element element, @NotNull LanguageCodeStyleProvider provider) {
//        CommonCodeStyleSettings defaultSettings = provider.getDefaultCommonSettings();
//        FormatterUtilKt.applyCangJieCodeStyle(CODE_STYLE_DEFAULTS, defaultSettings, false);
//
//        writeExternalBase(element, defaultSettings, provider);
//    }


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
        if (!(obj instanceof CangJieCommonCodeStyleSettings other)) {
            return false;
        }

        if (!ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)) {
            return false;
        }

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
