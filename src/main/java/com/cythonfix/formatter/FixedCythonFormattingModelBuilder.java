package com.cythonfix.formatter;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.python.pro.cython.CythonLanguageDialect;
import com.intellij.python.pro.cython.psi.elementTypes.CythonElementTypes;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.formatter.PythonFormattingModelBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Custom formatting model builder for Cython that fixes spacing issues.
 *
 * <h2>Problems Fixed</h2>
 * <ul>
 *   <li>Pointer declarations: {@code float *x} not {@code float * x}</li>
 *   <li>Type casts: {@code <RLE *>} not {@code < RLE * >}</li>
 *   <li>Address-of: {@code &var} not {@code & var}</li>
 * </ul>
 *
 * <h2>Approach</h2>
 * Uses two complementary mechanisms:
 * <ol>
 *   <li><b>SpacingBuilder</b> for simple token-level rules (casts, address-of)</li>
 *   <li><b>Block wrapper</b> for complex cases where pointer is inside a composite element</li>
 * </ol>
 */
public final class FixedCythonFormattingModelBuilder extends PythonFormattingModelBuilder {

    private static final TokenSet POINTER_OPERATOR_TOKENS = TokenSet.create(
            PyTokenTypes.MULT,
            PyTokenTypes.EXP
    );

    private static final TokenSet NAME_ELEMENT_TYPES = TokenSet.create(
            CythonElementTypes.NAME_DECL,
            CythonElementTypes.VARIABLE,
            PyTokenTypes.IDENTIFIER
    );

    private static final TokenSet TYPE_DECLARATION_ELEMENT_TYPES = TokenSet.create(
            CythonElementTypes.SIMPLE_BASE_TYPE_DECL,
            CythonElementTypes.COMPLEX_BASE_TYPE_DECL
    );

    // ========================================================================
    // SpacingBuilder - handles simple token-level spacing rules
    // ========================================================================

    @Override
    protected SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, CythonLanguageDialect.getInstance())
                // Type casts: <Type> - no space inside angle brackets
                .afterInside((IElementType) PyTokenTypes.LT, (IElementType) CythonElementTypes.TYPECAST_EXPRESSION).none()
                .beforeInside((IElementType) PyTokenTypes.GT, (IElementType) CythonElementTypes.TYPECAST_EXPRESSION).none()
                // Address-of operator: &expr - no space after &
                .afterInside((IElementType) PyTokenTypes.AND, (IElementType) CythonElementTypes.ADDRESS_EXPRESSION).none()
                // Pointer in name declaration: *name - no space after * or **
                .afterInside((IElementType) PyTokenTypes.MULT, (IElementType) CythonElementTypes.NAME_DECL).none()
                .afterInside((IElementType) PyTokenTypes.EXP, (IElementType) CythonElementTypes.NAME_DECL).none()
                // Inherit parent spacing rules
                .append(super.createSpacingBuilder(settings));
    }

    // ========================================================================
    // Block wrapper - handles complex cases (pointer inside SIMPLE_BASE_TYPE_DECL)
    // ========================================================================

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        FormattingModel baseModel = super.createModel(formattingContext);
        return new PointerSpacingFormattingModel(baseModel);
    }

    /**
     * FormattingModel wrapper that applies Block-level spacing fixes.
     */
    private static final class PointerSpacingFormattingModel implements FormattingModel {
        private final FormattingModel delegate;
        private final Block wrappedRootBlock;

        PointerSpacingFormattingModel(FormattingModel delegate) {
            this.delegate = delegate;
            this.wrappedRootBlock = new PointerSpacingBlock(delegate.getRootBlock());
        }

        @Override
        public @NotNull Block getRootBlock() {
            return wrappedRootBlock;
        }

        @Override
        public @NotNull FormattingDocumentModel getDocumentModel() {
            return delegate.getDocumentModel();
        }

        @Override
        public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
            return delegate.replaceWhiteSpace(textRange, whiteSpace);
        }

        @Override
        public TextRange shiftIndentInsideRange(ASTNode node, TextRange range, int indent) {
            return delegate.shiftIndentInsideRange(node, range, indent);
        }

        @Override
        public void commitChanges() {
            delegate.commitChanges();
        }
    }

    /**
     * Block wrapper that intercepts spacing decisions.
     *
     * <p>Handles the case where a pointer operator (* or **) is embedded inside
     * a SIMPLE_BASE_TYPE_DECL, which SpacingBuilder cannot handle because the
     * operator is not a direct child of the spacing context.
     */
    private static final class PointerSpacingBlock implements Block {
        private final Block delegate;
        private List<Block> wrappedSubBlocks;

        PointerSpacingBlock(Block delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NotNull TextRange getTextRange() {
            return delegate.getTextRange();
        }

        @Override
        public @NotNull List<Block> getSubBlocks() {
            if (wrappedSubBlocks == null) {
                wrappedSubBlocks = delegate.getSubBlocks().stream()
                        .map(PointerSpacingBlock::new)
                        .map(block -> (Block) block)
                        .toList();
            }
            return wrappedSubBlocks;
        }

        @Override
        public @Nullable Wrap getWrap() {
            return delegate.getWrap();
        }

        @Override
        public @Nullable Indent getIndent() {
            return delegate.getIndent();
        }

        @Override
        public @Nullable Alignment getAlignment() {
            return delegate.getAlignment();
        }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            if (shouldRemoveSpaceBetween(child1, child2) || shouldRemoveSpaceInTypecast(child1, child2)) {
                return Spacing.createSpacing(0, 0, 0, false, 0);
            }
            return delegate.getSpacing(unwrap(child1), unwrap(child2));
        }

        /**
         * Inside a TYPECAST_EXPRESSION ({@code <Type>}), remove space after {@code <} and before {@code >}.
         *
         * <p>The SpacingBuilder rules for this don't take effect because the base Python
         * formatter treats {@code <} and {@code >} as comparison operators and forces
         * spaces around them at the Block level.
         */
        private boolean shouldRemoveSpaceInTypecast(@Nullable Block child1, @NotNull Block child2) {
            ASTNode parentNode = getASTNode(this);
            if (parentNode == null || parentNode.getElementType() != CythonElementTypes.TYPECAST_EXPRESSION) {
                return false;
            }

            if (child1 != null) {
                ASTNode leftNode = getASTNode(child1);
                if (leftNode != null && leftNode.getElementType() == PyTokenTypes.LT) {
                    return true;
                }
            }

            ASTNode rightNode = getASTNode(child2);
            return rightNode != null && rightNode.getElementType() == PyTokenTypes.GT;
        }

        @Override
        public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            return delegate.getChildAttributes(newChildIndex);
        }

        @Override
        public boolean isIncomplete() {
            return delegate.isIncomplete();
        }

        @Override
        public boolean isLeaf() {
            return delegate.isLeaf();
        }

        private static Block unwrap(Block block) {
            return block instanceof PointerSpacingBlock wrapper ? wrapper.delegate : block;
        }
    }

    // ========================================================================
    // Spacing detection logic
    // ========================================================================

    /**
     * Determines whether spacing should be removed between two adjacent blocks.
     *
     * <p>This handles the case where a type declaration (SIMPLE_BASE_TYPE_DECL)
     * ends with a pointer operator and is followed by a name element.
     * SpacingBuilder cannot handle this because the pointer is nested inside
     * the type declaration, not a direct sibling of the name.
     */
    private static boolean shouldRemoveSpaceBetween(@Nullable Block left, @NotNull Block right) {
        if (left == null) {
            return false;
        }

        ASTNode leftNode = getASTNode(left);
        ASTNode rightNode = getASTNode(right);

        if (leftNode == null || rightNode == null) {
            return false;
        }

        return endsWithPointerOperator(leftNode) && isNameElement(rightNode);
    }

    private static boolean isNameElement(ASTNode node) {
        return NAME_ELEMENT_TYPES.contains(node.getElementType());
    }

    private static boolean endsWithPointerOperator(ASTNode node) {
        IElementType elementType = node.getElementType();

        // Direct pointer token
        if (POINTER_OPERATOR_TOKENS.contains(elementType)) {
            return true;
        }

        // Only check inside type declaration elements
        if (!TYPE_DECLARATION_ELEMENT_TYPES.contains(elementType)) {
            return false;
        }

        // Find the rightmost non-whitespace leaf
        ASTNode rightmostLeaf = findRightmostNonWhitespaceLeaf(node);
        return rightmostLeaf != null && POINTER_OPERATOR_TOKENS.contains(rightmostLeaf.getElementType());
    }

    private static @Nullable ASTNode findRightmostNonWhitespaceLeaf(ASTNode node) {
        ASTNode current = node;

        while (current != null) {
            ASTNode rightmostChild = null;
            for (ASTNode child = current.getLastChildNode(); child != null; child = child.getTreePrev()) {
                if (child.getElementType() != TokenType.WHITE_SPACE) {
                    rightmostChild = child;
                    break;
                }
            }

            if (rightmostChild == null) {
                return current.getElementType() != TokenType.WHITE_SPACE ? current : null;
            }

            current = rightmostChild;
        }

        return null;
    }

    private static @Nullable ASTNode getASTNode(Block block) {
        Block unwrapped = block instanceof PointerSpacingBlock wrapper ? wrapper.delegate : block;
        if (unwrapped instanceof ASTBlock astBlock) {
            return astBlock.getNode();
        }
        return null;
    }
}
