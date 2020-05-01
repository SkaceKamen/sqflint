/*
 * The MIT License
 *
 * Copyright 2016 Jan Zípek <jan at zipek.cz>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cz.zipek.sqflint.sqf.operators;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFVariable;
import cz.zipek.sqflint.parser.SQFParser;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.sqf.SQFArray;
import cz.zipek.sqflint.sqf.SQFContext;
import cz.zipek.sqflint.sqf.SQFExpression;
import cz.zipek.sqflint.sqf.SQFString;

/**
 * Params operator can define new variables throught his params.
 * 
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SetVariableOperator extends Operator {
    @Override
    public void analyze(Linter source, SQFContext context, SQFExpression expression) {
        SQFExpression right = expression.getRight();
        SQFExpression left = expression.getLeft();
        if (left != null && left.getIdentifier().toLowerCase().contains("missionnamespace")) {
            if (right != null && right.getMain() != null) {
                if (right.getMain() instanceof SQFArray) {
                        handleParams(source, context, (SQFArray)right.getMain());
                }
            }
        }       
    }

    protected void handleParams(Linter source, SQFContext context, SQFArray contents) {
        if (contents.getItems().size() > 0) {
            SQFExpression item = contents.getItems().get(0);
            if (item.getMain() instanceof SQFString) {
                handleParamLiteral(source, context, (SQFString)item.getMain());
            }
        }
    }

    /**
     * Registers literal contents as variable definition
     * 
     * @param literal possible variable definition
     * @return if literal was variable definition
     */
    private boolean handleParamLiteral(Linter source, SQFContext context, SQFString literal) {
        // Load variable name without quotes and case insensitive
        String ident = literal.getStringContents().toLowerCase();

        // Load variable
        SQFVariable var = context.getVariable(ident, literal.getStringContents(), true);

        // Actual variable name token (without quotes)
        Token unquoted = new Token(SQFParser.IDENTIFIER, literal.getStringContents());
        unquoted.beginLine = literal.getContents().beginLine;
        unquoted.endLine = literal.getContents().endLine;
        unquoted.beginColumn = literal.getContents().beginColumn + 1;
        unquoted.endColumn = literal.getContents().endColumn - 1;

        var.usage.add(unquoted);
        var.definitions.add(unquoted);
        var.comments.add(null);

        return true;
    }
}
