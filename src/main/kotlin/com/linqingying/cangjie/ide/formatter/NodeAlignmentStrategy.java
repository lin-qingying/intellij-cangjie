
package com.linqingying.cangjie.ide.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NodeAlignmentStrategy extends CommonAlignmentStrategy {

    private static final NodeAlignmentStrategy NULL_STRATEGY = fromTypes(AlignmentStrategy.wrap(null));


    public static NodeAlignmentStrategy getNullStrategy() {
        return NULL_STRATEGY;
    }

    public static NodeAlignmentStrategy fromTypes(AlignmentStrategy strategy) {
        return new AlignmentStrategyWrapper(strategy);
    }

    @Override
    @Nullable
    public abstract Alignment getAlignment(@NotNull ASTNode node);

    private static class AlignmentStrategyWrapper extends NodeAlignmentStrategy {
        private final AlignmentStrategy internalStrategy;

        public AlignmentStrategyWrapper(@NotNull AlignmentStrategy internalStrategy) {
            this.internalStrategy = internalStrategy;
        }

        @Nullable
        @Override
        public Alignment getAlignment(@NotNull ASTNode node) {
            ASTNode parent = node.getTreeParent();
            if (parent != null) {
                return internalStrategy.getAlignment(parent.getElementType(), node.getElementType());
            }

            return internalStrategy.getAlignment(node.getElementType());
        }
    }
}
