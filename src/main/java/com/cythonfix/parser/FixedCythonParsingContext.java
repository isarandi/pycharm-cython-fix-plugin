package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.python.pro.cython.parser.CythonDeclParsing;
import com.intellij.python.pro.cython.parser.CythonExpressionParsing;
import com.intellij.python.pro.cython.parser.CythonFunctionParsing;
import com.intellij.python.pro.cython.parser.CythonParsingContext;
import com.jetbrains.python.psi.LanguageLevel;

/**
 * Extended CythonParsingContext that returns our fixed parsers.
 */
public class FixedCythonParsingContext extends CythonParsingContext {
    private final FixedCythonDeclParsing myFixedDeclParser;
    private final FixedCythonFunctionParsing myFixedFuncParser;
    private final FixedCythonExpressionParsing myFixedExprParser;

    public FixedCythonParsingContext(SyntaxTreeBuilder builder, LanguageLevel languageLevel) {
        super(builder, languageLevel);
        this.myFixedDeclParser = new FixedCythonDeclParsing(this);
        this.myFixedFuncParser = new FixedCythonFunctionParsing(this);
        this.myFixedExprParser = new FixedCythonExpressionParsing(this);
    }

    @Override
    public CythonDeclParsing getDeclParser() {
        return this.myFixedDeclParser;
    }

    @Override
    public CythonFunctionParsing getFunctionParser() {
        return this.myFixedFuncParser;
    }

    @Override
    public CythonExpressionParsing getExpressionParser() {
        return this.myFixedExprParser;
    }

    public FixedCythonDeclParsing getFixedDeclParser() {
        return this.myFixedDeclParser;
    }
}
