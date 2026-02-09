package com.cythonfix.psi;

import com.intellij.psi.tree.IElementType;

/**
 * Custom element types for the Cython-Fix plugin.
 */
public interface FixedCythonElementTypes {
    IElementType UNNAMED_PARAMETER = FixedCythonParameterElementType.INSTANCE;
}
