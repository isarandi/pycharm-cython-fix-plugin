package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.python.pro.cython.parser.CythonParser;
import com.jetbrains.python.parsing.ParsingContext;
import com.jetbrains.python.psi.LanguageLevel;

/**
 * Extended CythonParser that creates our fixed parsing context.
 */
public class FixedCythonParser extends CythonParser {
    @Override
    protected ParsingContext createParsingContext(SyntaxTreeBuilder builder, LanguageLevel languageLevel) {
        return new FixedCythonParsingContext(builder, languageLevel);
    }
}
