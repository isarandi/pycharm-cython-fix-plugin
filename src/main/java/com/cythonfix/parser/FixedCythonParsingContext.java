package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.python.pro.cython.parser.CythonDeclParsing;
import com.intellij.python.pro.cython.parser.CythonParsingContext;
import com.jetbrains.python.psi.LanguageLevel;

/**
 * Extended CythonParsingContext that returns our fixed CythonDeclParsing.
 */
public class FixedCythonParsingContext extends CythonParsingContext {
    private final FixedCythonDeclParsing myFixedDeclParser;

    public FixedCythonParsingContext(SyntaxTreeBuilder builder, LanguageLevel languageLevel) {
        super(builder, languageLevel);
        this.myFixedDeclParser = new FixedCythonDeclParsing(this);
    }

    @Override
    public CythonDeclParsing getDeclParser() {
        return this.myFixedDeclParser;
    }
}
