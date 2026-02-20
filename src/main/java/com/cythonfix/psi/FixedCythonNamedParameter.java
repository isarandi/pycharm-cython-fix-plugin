package com.cythonfix.psi;

import com.intellij.lang.ASTNode;
import com.intellij.python.pro.cython.psi.CythonNamedParameter;
import com.jetbrains.python.psi.stubs.PyNamedParameterStub;

/**
 * A CythonNamedParameter that returns a unique synthetic name for unnamed parameters,
 * preventing false "duplicate parameter name" errors in cdef extern declarations
 * like: void f(qhT *, char *, char *) nogil
 */
public class FixedCythonNamedParameter extends CythonNamedParameter {
    public FixedCythonNamedParameter(ASTNode node) {
        super(node);
    }

    public FixedCythonNamedParameter(PyNamedParameterStub stub) {
        super(stub);
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name == null) {
            return "__unnamed_" + getTextOffset();
        }
        return name;
    }

    @Override
    public boolean hasDefaultValue() {
        // Type bracket parameters (e.g., map[void_ptr, void*]) are not real function
        // parameters and never have default values. Without this override, the base
        // implementation finds expression children (like the REFERENCE_EXPRESSION for
        // void_ptr) and misidentifies them as default values, causing false
        // "non-default parameter follows default parameter" warnings.
        return false;
    }
}