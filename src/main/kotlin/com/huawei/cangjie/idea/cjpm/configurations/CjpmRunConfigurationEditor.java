package com.huawei.cangjie.idea.cjpm.configurations;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PanelWithAnchor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CjpmRunConfigurationEditor extends SettingsEditor<CjpmRunConfiguration> implements PanelWithAnchor {
    private JPanel mainPanel;
    private JLabel labeltest;

    public CjpmRunConfigurationEditor(Project project) {

    }

    @Override
    protected void resetEditorFrom(@NotNull CjpmRunConfiguration s) {

    }

    @Override
    protected void applyEditorTo(@NotNull CjpmRunConfiguration s) throws ConfigurationException {

    }

    @Override
    protected @NotNull JComponent createEditor() {
        return mainPanel;
    }

    @Override
    public JComponent getAnchor() {
        return null;
    }

    @Override
    public void setAnchor(@Nullable JComponent anchor) {

    }
}
