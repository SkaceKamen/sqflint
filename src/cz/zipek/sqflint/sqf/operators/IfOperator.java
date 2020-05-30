/*
 * The MIT License
 *
 * Copyright 2016 Jan Zípek (jan at zipek.cz).
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
import cz.zipek.sqflint.linter.SQFParseException;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.sqf.SQFContext;
import cz.zipek.sqflint.sqf.SQFExpression;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class IfOperator extends Operator {
	
	private static final List<String> expected = Arrays.asList("then", "exitwith", "throw");
	private static final List<String> negations = Arrays.asList("!", "not");
	
	@Override
	public void analyze(Linter source, SQFContext context, SQFExpression expression) {
		// Condition required for if
		if (expression.getRight() == null) {
			source.getErrors().add(new SQFParseException(expression.getToken(), "Missing condition after if."));
			return;
		}
	
		// Skip !/not operator
		if (expression.getRight() != null &&
			expression.getRight().isOperator() &&
			negations.contains(expression.getRight().getMain().toString())) {
			expression = expression.getRight();
		}
		
		/* 
			Operation (then/exitWith/throw) required.
		
			Note, this isn't required by SQF language,
			You can have:
			
			>> _then = then { diag_log "THEN"; };
			>> if true _then;
			
			But that is very bad practice and will not by tolerated.
		*/
		if (expression.getRight() == null ||
			expression.getRight().getRight() == null) {
			Token token = expression.getToken();
			if (expression.getRight() != null) {
				token = expression.getRight().getToken();
			}
			source.getErrors().add(new SQFParseException(token, "Missing if operation after condition."));
			return;
		}
		
		// Only then, exitWith and throw can be used after if
		if (!expression.getRight().getRight().isCommand() ||
			!expected.contains(expression.getRight().getRight().getIdentifier().toLowerCase())
		) {
			source.getErrors().add(new SQFParseException(expression.getRight().getRight().getToken(), "Expected then, exitWith or throw."));
		}
	}
	
}
