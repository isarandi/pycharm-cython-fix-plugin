package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.python.pro.cython.parser.CythonDeclParsing;
import com.intellij.python.pro.cython.parser.CythonFunctionParsing;
import com.intellij.python.pro.cython.parser.CythonParsingContext;
import com.jetbrains.python.psi.LanguageLevel;

/**
 * Extended CythonParsingContext that returns our fixed parsers.
 */
public class FixedCythonParsingContext extends CythonParsingContext {
    private final FixedCythonDeclParsing myFixedDeclParser;
    private final FixedCythonFunctionParsing myFixedFuncParser;

    public FixedCythonParsingContext(SyntaxTreeBuilder builder, LanguageLevel languageLevel) {
        super(builder, languageLevel);
        this.myFixedDeclParser = new FixedCythonDeclParsing(this);
        this.myFixedFuncParser = new FixedCythonFunctionParsing(this);
    }

    @Override
    public CythonDeclParsing getDeclParser() {
        return this.myFixedDeclParser;
    }

    @Override
    public CythonFunctionParsing getFunctionParser() {
        return this.myFixedFuncParser;
    }

    public FixedCythonDeclParsing getFixedDeclParser() {
        return this.myFixedDeclParser;
    }
}
