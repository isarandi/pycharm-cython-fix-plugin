package com.cythonfix.parser;

import com.intellij.lang.SyntaxTreeBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.python.pro.cython.CythonNames;
import com.intellij.python.pro.cython.parser.CythonDeclParsing;
import com.intellij.python.pro.cython.parser.CythonParsingContext;
import com.intellij.python.pro.cython.psi.elementTypes.CythonElementTypes;
import com.jetbrains.python.PyElementTypes;
import com.jetbrains.python.PyParsingBundle;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.parsing.ParsingContext;

/**
 * Fixed version of CythonDeclParsing that properly handles keyword arguments
 * in buffer dtype syntax like np.ndarray[np.float64_t, ndim=1].
 */
public class FixedCythonDeclParsing extends CythonDeclParsing {
    protected static final String CONST_TOKEN = "const";
    protected static final String COMPLEX_TOKEN = "complex";

    public FixedCythonDeclParsing(ParsingContext context) {
        super(context);
    }

    /**
     * Override to use our fixed parseSimpleBaseTypeDecl.
     */
    @Override
    public void parseBaseTypeDecl(boolean requireNonEmpty) {
        if (atToken(PyTokenTypes.IDENTIFIER, CONST_TOKEN) &&
            getParsingContext().getBuilder().lookAhead(1) == PyTokenTypes.LPAR) {
            nextToken();
        }
        if (atToken(PyTokenTypes.LPAR)) {
            parseComplexBaseTypeDeclFixed();
        } else {
            parseSimpleBaseTypeDeclFixed(requireNonEmpty);
        }
    }

    private void parseComplexBaseTypeDeclFixed() {
        SyntaxTreeBuilder builder = getParsingContext().getBuilder();
        SyntaxTreeBuilder.Marker marker = builder.mark();
        nextToken();
        parseBaseTypeDecl(false);
        SyntaxTreeBuilder.Marker afterType = builder.mark();
        parseNameDecl(true, false, false);
        if (atToken(PyTokenTypes.COMMA)) {
            afterType.rollbackTo();
            while (atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP)) {
                nextToken();
            }
            while (atToken(PyTokenTypes.COMMA)) {
                nextToken();
                if (atToken(PyTokenTypes.RPAR)) {
                    nextToken();
                    marker.done(CythonElementTypes.COMPLEX_BASE_TYPE_DECL);
                    return;
                }
                parseBaseTypeDecl(false);
                while (atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP)) {
                    nextToken();
                }
            }
        } else {
            afterType.drop();
        }
        checkMatches(PyTokenTypes.RPAR, PyParsingBundle.message("PARSE.expected.rpar"));
        marker.done(CythonElementTypes.COMPLEX_BASE_TYPE_DECL);
    }

    private void parseSimpleBaseTypeDeclFixed(boolean requireNonEmpty) {
        SyntaxTreeBuilder builder = getParsingContext().getBuilder();
        if (atToken(PyTokenTypes.IDENTIFIER, CONST_TOKEN)) {
            nextToken();
        }
        SyntaxTreeBuilder.Marker marker = builder.mark();

        if (!atToken(PyTokenTypes.IDENTIFIER)) {
            builder.error(PyParsingBundle.message("PARSE.expected.identifier"));
            nextToken();
            marker.drop();
            return;
        }

        if (atToken(PyTokenTypes.IDENTIFIER, CONST_TOKEN)) {
            nextToken();
        }

        if (atBaseTypeToken()) {
            if (CythonNames.BASE_CYTHON_TYPES.contains(builder.getTokenText())) {
                nextToken();
            } else {
                while (atToken(PyTokenTypes.IDENTIFIER) &&
                       CythonNames.BASE_TYPE_MODIFIERS.contains(builder.getTokenText())) {
                    nextToken();
                }
                if (atToken(PyTokenTypes.IDENTIFIER) &&
                    CythonNames.BASE_C_TYPES.contains(builder.getTokenText())) {
                    nextToken();
                }
                if (atToken(PyTokenTypes.IDENTIFIER, COMPLEX_TOKEN)) {
                    nextToken();
                }
            }
        } else if (atDottedName()) {
            parseDottedName();
        } else {
            SyntaxTreeBuilder.Marker ref = builder.mark();
            nextToken();
            if (requireNonEmpty && !atToken(PyTokenTypes.IDENTIFIER)) {
                if (atToken(PyTokenTypes.LPAR)) {
                    SyntaxTreeBuilder.Marker atLeftParen = builder.mark();
                    nextToken();
                    if (!atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP, PyTokenTypes.AND)) {
                        ref.drop();
                        atLeftParen.drop();
                        marker.rollbackTo();
                        return;
                    }
                    ref.drop();
                    atLeftParen.rollbackTo();
                } else if (!atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP, PyTokenTypes.LBRACKET, PyTokenTypes.AND)) {
                    ref.drop();
                    marker.rollbackTo();
                    return;
                } else {
                    ref.done(getReferenceType());
                }
            } else {
                ref.done(getReferenceType());
            }
        }

        // This is the key part - use our fixed type parameters parsing
        if (atToken(PyTokenTypes.LBRACKET)) {
            if (atMemoryView()) {
                parseMemoryViewFixed();
            } else {
                while (atToken(PyTokenTypes.LBRACKET)) {
                    parseTypeParametersDeclFixed();
                }
            }
        }

        if (atToken(PyTokenTypes.DOT)) {
            nextToken();
            parseIdentifierFixed();
        }
        if (atToken(PyTokenTypes.MULT)) {
            nextToken();
        }
        marker.done(CythonElementTypes.SIMPLE_BASE_TYPE_DECL);
    }

    private void parseMemoryViewFixed() {
        SyntaxTreeBuilder.Marker exprStart = myBuilder.mark();
        nextToken();
        getParsingContext().getExpressionParser().parseSubscriptionIndexArgumentList();
        exprStart.done(PyElementTypes.SUBSCRIPTION_EXPRESSION);
    }

    private boolean atMemoryView() {
        boolean result = false;
        SyntaxTreeBuilder.Marker marker = getParsingContext().getBuilder().mark();
        nextToken();
        if (atToken(PyTokenTypes.INTEGER_LITERAL)) {
            nextToken();
        }
        if (atToken(PyTokenTypes.COLON)) {
            result = true;
        }
        marker.rollbackTo();
        return result;
    }

    private void parseTypeParametersDeclFixed() {
        SyntaxTreeBuilder.Marker marker = myBuilder.mark();
        nextToken();
        parsePositionalAndKeywordArgsFixed();
        checkMatches(PyTokenTypes.RBRACKET, PyParsingBundle.message("PARSE.expected.rbracket"));
        marker.done(PyElementTypes.PARAMETER_LIST);
    }

    /**
     * FIXED: This method now properly handles keyword arguments like ndim=1.
     */
    private void parsePositionalAndKeywordArgsFixed() {
        while (!atToken(PyTokenTypes.RBRACKET)) {
            SyntaxTreeBuilder.Marker namedParameter = myBuilder.mark();

            // Check for keyword argument pattern: identifier =
            boolean isKeywordArg = false;
            if (atToken(PyTokenTypes.IDENTIFIER)) {
                SyntaxTreeBuilder.Marker lookahead = myBuilder.mark();
                nextToken();
                if (atToken(PyTokenTypes.EQ)) {
                    // This is a keyword argument like ndim=1
                    lookahead.rollbackTo();
                    isKeywordArg = true;
                } else {
                    lookahead.rollbackTo();
                }
            }

            if (isKeywordArg) {
                // Parse keyword argument: create proper structure with name and value
                SyntaxTreeBuilder.Marker nameDecl = myBuilder.mark();
                nextToken(); // consume identifier (e.g., 'ndim')
                nameDecl.done(CythonElementTypes.NAME_DECL);

                nextToken(); // consume '='

                // Parse the value expression
                if (atExpressionFixed()) {
                    getExpressionParser().parseSingleExpression(false);
                } else {
                    myBuilder.error(PyParsingBundle.message("PARSE.expected.expression"));
                }
            } else {
                // Original logic for positional arguments (type expressions)
                if (atExpressionFixed()) {
                    getExpressionParser().parseSingleExpression(false);
                } else {
                    SyntaxTreeBuilder.Marker typeDecl = myBuilder.mark();
                    parseBaseTypeDecl(true);
                    parseNameDecl(true, false, false);
                    typeDecl.done(CythonElementTypes.COMPLEX_BASE_TYPE_DECL);
                }
            }

            namedParameter.done(CythonElementTypes.NAMED_PARAMETER);

            if (!atToken(PyTokenTypes.COMMA)) {
                if (atToken(PyTokenTypes.RBRACKET)) break;
                myBuilder.error(PyParsingBundle.message("PARSE.expected.symbols", ",", "]"));
                break;
            }
            nextToken();
        }
    }

    private void parseDottedName() {
        SyntaxTreeBuilder builder = getParsingContext().getBuilder();
        SyntaxTreeBuilder.Marker expr = builder.mark();
        assertCurrentToken(PyTokenTypes.IDENTIFIER);
        buildTokenElement(getReferenceType(), builder);
        while (atToken(PyTokenTypes.DOT)) {
            nextToken();
            parseIdentifierFixed();
            expr.done(getReferenceType());
            expr = expr.precede();
        }
        expr.drop();
    }

    private boolean atExpressionFixed() {
        if (atBaseTypeToken()) {
            return false;
        }
        if (atToken(PyTokenTypes.IDENTIFIER)) {
            boolean isType = false;
            SyntaxTreeBuilder.Marker marker = myBuilder.mark();
            nextToken();
            while (atToken(PyTokenTypes.DOT)) {
                nextToken();
                parseIdentifierFixed();
            }
            if (atToken(PyTokenTypes.IDENTIFIER)) {
                isType = true;
            } else if (atAnyOfTokens(PyTokenTypes.MULT, PyTokenTypes.EXP)) {
                nextToken();
                isType = atAnyOfTokens(PyTokenTypes.RPAR, PyTokenTypes.RBRACKET);
            } else if (atToken(PyTokenTypes.LPAR)) {
                nextToken();
                isType = atToken(PyTokenTypes.MULT);
            } else if (atToken(PyTokenTypes.LBRACKET)) {
                nextToken();
                isType = atToken(PyTokenTypes.RBRACKET);
            }
            marker.rollbackTo();
            return !isType;
        }
        return true;
    }

    private boolean atDottedName() {
        boolean result = false;
        SyntaxTreeBuilder.Marker marker = getParsingContext().getBuilder().mark();
        if (atToken(PyTokenTypes.IDENTIFIER)) {
            nextToken();
            result = atToken(PyTokenTypes.DOT);
        }
        marker.rollbackTo();
        return result;
    }

    private boolean atBaseTypeToken() {
        String s = getParsingContext().getBuilder().getTokenText();
        return CythonNames.BASE_C_TYPES.contains(s) ||
               CythonNames.BASE_CYTHON_TYPES.contains(s) ||
               CythonNames.BASE_TYPE_MODIFIERS.contains(s);
    }

    private boolean parseIdentifierFixed() {
        return checkMatches(PyTokenTypes.IDENTIFIER, PyParsingBundle.message("PARSE.expected.identifier"));
    }

    @Override
    protected IElementType getReferenceType() {
        return CythonElementTypes.REFERENCE_EXPRESSION;
    }
}
