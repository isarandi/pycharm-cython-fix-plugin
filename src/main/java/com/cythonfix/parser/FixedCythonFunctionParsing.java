package com.cythonfix.parser;

import com.cythonfix.psi.FixedCythonElementTypes;
import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.python.pro.cython.parser.CythonDeclParsing;
import com.intellij.python.pro.cython.parser.CythonFunctionParsing;
import com.intellij.python.pro.cython.parser.CythonTokenTypes;
import com.intellij.python.pro.cython.psi.elementTypes.CythonElementTypes;
import com.jetbrains.python.PyParsingBundle;
import com.jetbrains.python.PyTokenTypes;

/**
 * Fixed version of CythonFunctionParsing that distinguishes unnamed parameters
 * (like in "void f(qhT *, char *, char *) nogil") from named ones, using a
 * custom UNNAMED_PARAMETER element type that provides unique synthetic names
 * to avoid false "duplicate parameter name" errors.
 */
public class FixedCythonFunctionParsing extends CythonFunctionParsing {
    protected static final String TOK_NONE = "None";

    public FixedCythonFunctionParsing(FixedCythonParsingContext context) {
        super(context);
    }

    @Override
    public FixedCythonParsingContext getParsingContext() {
        return (FixedCythonParsingContext) myContext;
    }

    @Override
    protected boolean parseParameter(IElementType endToken, boolean isLambda) {
        // Handle single star parameter: just "*" followed by comma or end
        if (parseSingleStarParameterFixed(endToken)) {
            return true;
        }

        // Lambda or *args/**kwargs: delegate to parent
        if (isLambda || atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP)) {
            return super.parseParameter(endToken, isLambda);
        }

        // At end token: nothing to parse
        if (atToken(endToken)) {
            return false;
        }

        SyntaxTreeBuilder.Marker marker = myBuilder.mark();

        if (atToken(PyTokenTypes.DOT)) {
            // Ellipsis parameter
            parseEllipsisFixed();
            marker.done(CythonElementTypes.ELLIPSIS_DECL);
        } else {
            // Cython typed parameter: parse type, then name
            FixedCythonDeclParsing declParser = getParsingContext().getFixedDeclParser();
            declParser.parseBaseTypeDecl(true);
            CythonDeclParsing.DeclaratorType declType =
                    declParser.parseNameDeclPublic(false, false, false);

            // Handle "not None" / "or None" constraints
            if (atAnyOfTokens(PyTokenTypes.NOT_KEYWORD, PyTokenTypes.OR_KEYWORD)) {
                nextToken();
                if (atToken(PyTokenTypes.NONE_KEYWORD)
                        || atToken(PyTokenTypes.IDENTIFIER, TOK_NONE)) {
                    nextToken();
                } else {
                    myBuilder.error(
                            PyParsingBundle.message("PARSE.0.expected", TOK_NONE));
                }
            }

            parseParameterAnnotation();

            // Handle default value
            if (atToken(PyTokenTypes.EQ)) {
                nextToken();
                if (atAnyOfTokens(CythonTokenTypes.QUESTION, PyTokenTypes.MULT)) {
                    nextToken();
                } else {
                    getExpressionParser().parseSingleExpression(false);
                }
            }

            // Key fix: use UNNAMED_PARAMETER for params without a name
            if (declType == CythonDeclParsing.DeclaratorType.EMPTY) {
                marker.done(FixedCythonElementTypes.UNNAMED_PARAMETER);
            } else {
                marker.done(CythonElementTypes.NAMED_PARAMETER);
            }
        }
        return true;
    }

    private boolean parseSingleStarParameterFixed(IElementType endToken) {
        SyntaxTreeBuilder.Marker marker = myBuilder.mark();
        if (atToken(PyTokenTypes.MULT)) {
            nextToken();
            if (atAnyOfTokens(endToken, PyTokenTypes.COMMA)) {
                marker.drop();
                return true;
            }
        }
        marker.rollbackTo();
        return false;
    }

    private void parseEllipsisFixed() {
        String msg = PyParsingBundle.message("PARSE.expected.ellipsis");
        if (checkMatches(PyTokenTypes.DOT, msg) && checkMatches(PyTokenTypes.DOT, msg)) {
            checkMatches(PyTokenTypes.DOT, msg);
        }
    }
}
