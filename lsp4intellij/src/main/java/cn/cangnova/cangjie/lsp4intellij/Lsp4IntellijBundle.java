package cn.cangnova.cangjie.lsp4intellij;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class Lsp4IntellijBundle extends DynamicBundle {
    public static final String BUNDLE_NAME = "messages.Lsp4IntellijBundle";

    public Lsp4IntellijBundle() {
        super(Lsp4IntellijBundle.class, BUNDLE_NAME);
    }

    public static final Lsp4IntellijBundle INSTANCE = new Lsp4IntellijBundle();

    public static String message(@NonNls @PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
