package com.cythonfix.psi;

import com.intellij.psi.tree.TokenSet;
import com.jetbrains.python.PythonDialectsTokenSetContributorBase;
import org.jetbrains.annotations.NotNull;

/**
 * Registers UNNAMED_PARAMETER so that PyParameterListImpl.getParameters()
 * recognizes our custom element type as a parameter node.
 */
public class FixedCythonTokenSetContributor extends PythonDialectsTokenSetContributorBase {
    @Override
    @NotNull
    public TokenSet getParameterTokens() {
        return TokenSet.create(FixedCythonElementTypes.UNNAMED_PARAMETER);
    }
}
