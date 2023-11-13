package com.huawei.cangjie.idea.cjpm.configurations;

import com.huawei.cangjie.lang.sdk.CangJieSdkManager;
import com.huawei.cangjie.lang.sdk.CangJieSdkType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CjpmRunConfigurationEditor extends SettingsEditor<CjpmRunConfiguration> implements PanelWithAnchor {
    private JPanel mainPanel;
    private LabeledComponent command;
    private LabeledComponent moudle;
//    private LabeledComponent cjcversion;
//    private LabeledComponent cjpmversion;


    //    命令列表
    private final List<CjpmCommandItem> commandItems = new ArrayList<>();

    //    仓颉sdk列表
    private List<Sdk> sdks = new ArrayList<>();


    private final Project project;


    static final String SELECTED_COMMAND_KEY = "cjpm.selected.command";


    public CjpmRunConfigurationEditor(Project project) {


        this.project = project;
//        ((ComboBox) cjcversion.getComponent()).setMinLength(200);
//        ((ComboBox) cjpmversion.getComponent()).setMinLength(200);


        initUI();
    }


//   public  void resetSdks(){
//        initSdkItems();
//       initCjcVersion();
//       initCjpmVersion();
//   }

    private void initUI() {


//        initSdkItems();

        initComboxItems();
        initMoudleButton();
//        initCjcVersion();
//        initCjpmVersion();


    }

    public String getCommand() {
        return ((ComboBox) this.command.getComponent()).getSelectedItem().toString();
    }

    public Integer getSelectedCommandIndex() {
        return ((ComboBox) this.command.getComponent()).getSelectedIndex();
    }

    public void setSelectedCommandIndex(Integer index) {
        ((ComboBox) this.command.getComponent()).setSelectedIndex(index);
    }
//    public String getCjpmPath() {
//        int i = ((ComboBox<?>) this.cjpmversion.getComponent()).getSelectedIndex();
//        if (sdks.get(i) == null) return null;
//        return ((CangJieSdkType) sdks.get(i).getSdkType()).getSdkAdditionalData().getCjpmPath();
//
//    }

//    public String getCjcPath() {
//        int i = ((ComboBox<?>) this.cjcversion.getComponent()).getSelectedIndex();
//        if (sdks.get(i) == null) return null;
//        return ((CangJieSdkType) sdks.get(i).getSdkType()).getSdkAdditionalData().getCjcPath();
//
//    }

    private void initComboxItems() {
        initCjpmCommandItems();

    }

    private void initSdkItems() {
//        将项目所使用的sdk排到最前面，如果项目没有使用sdk，则第一个为null，并提示配置仓颉sdk
        this.sdks = CangJieSdkManager.INSTANCE.getAllCangJieSdks();
        Sdk projectSdk = CangJieSdkManager.INSTANCE.getProjectSdk();
        if (projectSdk != null) {
            sdks.remove(projectSdk);
            sdks.add(0, projectSdk);
        } else {
            sdks.add(0, null);
        }

    }

    @Override
    public boolean isReadyForApply() {
        return super.isReadyForApply();
    }


    TextComponentAccessor<JTextField> moudelJsonAccessor = new TextComponentAccessor<>() {
        @Override
        public String getText(JTextField component) {
            return component.getText();
        }

        @Override
        public void setText(JTextField component, String text) {
            component.setText(text);
        }
    };

    //    为module的按钮添加事件
    private void initMoudleButton() {

        TextFieldWithBrowseButton textFieldWithBrowseButton = (TextFieldWithBrowseButton) moudle.getComponent();
        textFieldWithBrowseButton.addBrowseFolderListener("Select moudle.json File", null, null,
                FileChooserDescriptorFactory.createSingleFileDescriptor(), moudelJsonAccessor

        );
//        判断是否有moudle.json文件
        VirtualFile moudleFile = getModuleJson();
        if (moudleFile != null) {
            textFieldWithBrowseButton.setText(moudleFile.getPath());
        } else {
            textFieldWithBrowseButton.setText("");
        }

//        textFieldWithBrowseButton.setText(this.project.getBasePath() + "/moudle.json");

        moudle.setComponent(textFieldWithBrowseButton);
//        moudle.getComponent().setEnabled(false);
        moudle.setEnabled(false);
    }

//    void initCjpmVersion() {
//        ((ComboBox) this.cjpmversion.getComponent()).setEditable(true);
//
//
//        ((ComboBox) this.cjpmversion.getComponent()).setModel(new DefaultComboBoxModel(sdks.stream().map(sdk -> {
//            if (sdk == null) {
//                return "Please configure Changjie sdk;";
//            } else {
//                return "cjpm  " + "(" + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjpmPath() + ")     "
////                        +
//                        + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjpmVersion();
////                return ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjpmPath() + "       " + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjpmVersion();
//            }
//        }).toArray()));
////        ((ComboBox) this.cjpmversion.getComponent()).setRenderer(
////                new SimpleListCellRenderer<String>() {
////                    @Override
////                    public void customize(JList<? extends String> list, String value, int index, boolean selected, boolean hasFocus) {
////                        String[] parts = value.split(";");
////                        if (parts.length == 2) {
////                            setText("<html><div style='width:100px'>" + parts[0] + "</div><div style='float:right'>" + parts[1] + "</div></html>");
////                        }
////                    }
////                }
////
////        );
//
//    }
//
//    void initCjcVersion() {
//
////      (  (CangJieSdkType)sdks.get(0).getSdkType()).getSdkAdditionalData().getCjcPath()  + (  (CangJieSdkType)sdks.get(0).getSdkType()).getSdkAdditionalData().getCjcVersion()
//        ((ComboBox) this.cjcversion.getComponent()).setModel(new DefaultComboBoxModel(sdks.stream().map(sdk -> {
//            if (sdk == null) {
//                return "Please configure Changjie sdk;";
//            } else {
//                return "cjc  " + "(" + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjcPath() + ")     " + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjcVersion();
////                return ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjcPath() + "       " + ((CangJieSdkType) sdk.getSdkType()).getSdkAdditionalData().getCjcVersion();
//            }
//        }).toArray()));
//
//        ((ComboBox) this.cjcversion.getComponent()).addItemListener(
//                e -> {
//                    if (e.getStateChange() == ItemEvent.SELECTED) {
//                        e.getItem();
//                    }
//                }
//        );
//
//    }


    public VirtualFile getModuleJson() {
        String basePath = project.getBasePath();
        if (basePath != null) {
            File moduleJson = new File(basePath, "module.json");
            if (moduleJson.exists()) {
                return LocalFileSystem.getInstance().findFileByIoFile(moduleJson);
            }
        }
        return null;
    }

    private void initCjpmCommandItems() {


        commandItems.add(new CjpmCommandItem("update", "update", "更新模块"));
        commandItems.add(new CjpmCommandItem("build", "build", "编译模块"));
        commandItems.add(new CjpmCommandItem("clean", "clean", "清理模块"));
//        commandItems.add(new CjpmCommandItem("init", "init", "初始化模块"));
        ((ComboBox) command.getComponent()).setModel(new DefaultComboBoxModel(commandItems.toArray()));
        // 恢复上次选中的命令


    }


    @Override
    protected void resetEditorFrom(@NotNull CjpmRunConfiguration s) {
        ((ComboBox) this.command.getComponent()).setSelectedIndex(s.getCommandSelectIndex());
    }

    @Override
    protected void applyEditorTo(@NotNull CjpmRunConfiguration s) throws ConfigurationException {
        System.out.println();
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
        System.out.println();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }


    //    cjpm命令 Item
    class CjpmCommandItem {
        private final String name;
        private final String command;
        private final String description;

        public CjpmCommandItem(String name, String command, String description) {
            this.name = name;
            this.command = command;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
