/*
 * The MIT License
 *
 * Copyright 2016 kamen.
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
package cz.zipek.sqflint.sqf;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFVariable;

/**
 *
 * @author kamen
 */
public class SQFExpression extends SQFUnit {	
	private SQFUnit main;
	private SQFExpression left;
	private SQFExpression right;
	private SQFLiteral operator;
	
	public SQFExpression setOperator(SQFLiteral lit) {
		operator = lit;
		return this;
	}
	
	public SQFExpression setMain(SQFUnit expr) {
		main = expr;
		return this;
	}
	
	public SQFExpression setLeft(SQFExpression expr) {
		left = expr;
		return this;
	}
	
	public SQFExpression setRight(SQFExpression expr) {
		right = expr;
		return this;
	}

	/**
	 * @return the main
	 */
	public SQFUnit getMain() {
		return main;
	}

	/**
	 * @return the left
	 */
	public SQFExpression getLeft() {
		return left;
	}

	/**
	 * @return the right
	 */
	public SQFExpression getRight() {
		return right;
	}

	/**
	 * @return the operator
	 */
	public SQFLiteral getOperator() {
		return operator;
	}

	@Override
	public void analyze(Linter source, SQFBlock context) {
		// Do some analytics on inside (probably won't do anything)
		if (main != null) {
			main.analyze(source, context);
		}

		// If main part of expression is identifier, try to run command
		if (main != null && main instanceof SQFIdentifier) {
			// Load main part of this expression
			SQFIdentifier token = (SQFIdentifier)main;
			String ident = token.getToken().image.toLowerCase();
			
			if (source.getOperators().containsKey(ident)) {
				source.getOperators().get(ident).analyze(source, context, this);
			}
		}
		
		// We're going left to right
		if (right != null) {
			right.analyze(source, context);
		}
	}
}
