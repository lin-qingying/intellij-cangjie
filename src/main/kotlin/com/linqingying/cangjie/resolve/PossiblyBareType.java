package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.CastDiagnosticsUtil;
import com.linqingying.cangjie.types.TypeConstructor;
import com.linqingying.cangjie.types.TypeReconstructionResult;
import com.linqingying.cangjie.types.util.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 裸类型类似于原始类型，但在 CangJie 中仅允许在 is/as 操作的右侧使用。
 * 例如：
 * <p>
 * fun foo(a: Any) {
 * if (a is List) {
 * // 在这里 a 被认为是 List<Any>
 * }
 * }
 * <p>
 * 另一个例子：
 * <p>
 * fun foo(a: Collection<String>) {
 * if (a is List) {
 * // 在这里 a 被认为是 List<String>
 * }
 * }
 * <p>
 * 可以调用 reconstruct(supertype) 从裸类型获取实际类型
 */

public class PossiblyBareType {
    // 实际类型，如果为裸类型则可能为 null。
    private final CangJieType actualType;
    // 裸类型的类型构造器，用于类型重建。
    private final TypeConstructor bareTypeConstructor;
    // 标记此类型是否可选。
    private final boolean optional;

    /**
     * 私有构造函数，用于创建 PossiblyBareType 实例。
     *
     * @param actualType 实际类型，可以为 null
     * @param bareTypeConstructor 裸类型的类型构造器，可以为 null
     * @param optional 标记此类型是否可选
     */
    private PossiblyBareType(@Nullable CangJieType actualType, @Nullable TypeConstructor bareTypeConstructor, boolean optional) {
        this.actualType = actualType;
        this.bareTypeConstructor = bareTypeConstructor;
        this.optional = optional;
    }

    /**
     * 检查裸类型是否可为空。
     *
     * @return 如果裸类型可为空则返回 true，否则返回 false
     */
    private boolean isBareTypeNullable() {
        return optional;
    }

    /**
     * 检查此类型是否可选。
     *
     * @return 如果类型可选则返回 true，否则返回 false
     */
    public boolean isOptional() {
        if (isBare()) return isBareTypeNullable();
        return getActualType().isMarkedOption();
    }

    /**
     * 创建一个裸类型的实例。
     *
     * @param bareTypeConstructor 裸类型的类型构造器
     * @param optional 标记此类型是否可选
     * @return 新的 PossiblyBareType 实例
     */
    @NotNull
    public static PossiblyBareType bare(@NotNull TypeConstructor bareTypeConstructor, boolean optional) {
        return new PossiblyBareType(null, bareTypeConstructor, optional);
    }

    /**
     * 从裸类型重建实际类型。
     *
     * @param subjectType 主题类型
     * @return 重建后的类型结果
     */
    @NotNull
    public TypeReconstructionResult reconstruct(@NotNull CangJieType subjectType) {
        if (!isBare()) return new TypeReconstructionResult(getActualType(), true);

        // 查找静态已知的子类型
        TypeReconstructionResult reconstructionResult = CastDiagnosticsUtil.findStaticallyKnownSubtype(
                TypeUtils.makeNotNullable(subjectType),
                getBareTypeConstructor()
        );
        CangJieType type = reconstructionResult.getResultingType();
        // 如果类型不存在，直接返回重建结果
        if (type == null) return reconstructionResult;

        // 根据是否可空调整结果类型
        CangJieType resultingType = TypeUtils.makeOptionalAsSpecified(type, isBareTypeNullable());
        return new TypeReconstructionResult(resultingType, reconstructionResult.isAllArgumentsInferred());
    }

    /**
     * 将当前类型标记为可选。
     *
     * @return 新的 PossiblyBareType 实例，标记为可选
     */
    public PossiblyBareType makeOptional() {
        if (isBare()) {
            return isBareTypeNullable() ? this : bare(getBareTypeConstructor(), true);
        }

        return type(TypeUtils.makeOptional(getActualType()));
    }

    /**
     * 获取裸类型的类型构造器。
     *
     * @return 裸类型的类型构造器
     */
    @NotNull
    public TypeConstructor getBareTypeConstructor() {
        //noinspection ConstantConditions
        return bareTypeConstructor;
    }

    /**
     * 获取实际类型。
     *
     * @return 实际类型
     */
    @NotNull
    public CangJieType getActualType() {
        //noinspection ConstantConditions
        return actualType;
    }

    /**
     * 检查当前类型是否为裸类型。
     *
     * @return 如果是裸类型则返回 true，否则返回 false
     */
    public boolean isBare() {
        return actualType == null;
    }

    /**
     * 创建一个具有实际类型的实例。
     *
     * @param actualType 实际类型
     * @return 新的 PossiblyBareType 实例
     */
    @NotNull
    public static PossiblyBareType type(@NotNull CangJieType actualType) {
        return new PossiblyBareType(actualType, null, false);
    }
}
