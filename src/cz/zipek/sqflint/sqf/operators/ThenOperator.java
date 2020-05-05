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
import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.sqf.SQFContext;
import cz.zipek.sqflint.sqf.SQFExpression;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class ThenOperator extends Operator {

	@Override
	public void analyze(Linter source, SQFContext context, SQFExpression expression) {
		// Expect argument
		if (expression.getRight() == null) {
			source.getErrors().add(new SQFParseException(expression.getToken(), "Expected block after then."));
		}
		
		// Expect only block
		if (!(expression.getRight().getMain() instanceof SQFBlock)) {
			source.getErrors().add(new SQFParseException(expression.getRight().getToken(), "Expected block after then."));
		}
		
		// Expect else or nothing after block
		if (expression.getRight().getRight() != null &&
			!expression.getRight().getRight().getIdentifier().equalsIgnoreCase("else")) {
			source.getErrors().add(new SQFParseException(expression.getRight().getRight().getToken(), "Expected else or nothing."));
		}
	}
	
}
