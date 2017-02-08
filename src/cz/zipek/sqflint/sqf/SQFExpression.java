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
import cz.zipek.sqflint.parser.Token;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author kamen
 */
public class SQFExpression extends SQFUnit {	
	private Token token;
	
	private SQFUnit main;
	private SQFExpression left;
	private SQFExpression right;
	
	private final List<String> signOperators = Arrays.asList("+", "-");
	
	public SQFExpression(Token token) {
		this.token = token;
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
	 * @return contents of expression
	 */
	public SQFUnit getMain() {
		if (main instanceof SQFExpression &&
			((SQFExpression)main).getLeft() == null &&
			((SQFExpression)main).getRight() == null) {
			return ((SQFExpression)main).getMain();
		}
		
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

	public Token getToken() {
		return token;
		
		/*
		if (main != null && main instanceof SQFIdentifier) {
			// Load main part of this expression
			SQFIdentifier token = (SQFIdentifier)main;
			return token.getToken();
		}
		return null;
		*/
	}
	
	public String getIdentifier() {
		if (main != null && main instanceof SQFIdentifier) {
			// Load main part of this expression
			SQFIdentifier token = (SQFIdentifier)main;
			return token.getToken().image.toLowerCase();
		}
		return null;
	}
	
	public boolean isCommand(Linter source) {
		return (getIdentifier() != null && source.getOperators().containsKey(getIdentifier()));
	}
	
	public boolean isVariable(Linter source) {
		return (getIdentifier() != null && !isCommand(source));
	}
	
	public boolean isOperator() {
		return main != null && main instanceof SQFOperator;
	}
	
	public boolean isSignOperator() {
		return isOperator() && signOperators.contains(main.toString());
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
		
		// Check right side for some cases
		if (!isCommand(source)) {
			if (right != null && right.getToken() != null && !right.isCommand(source)) {
				// source.getErrors().add(new SQFParseException(new SQFParseException(right.getToken(), "Unexpected " + right.getToken().toString())));
			}
			if (right != null && right.main != null && right.main instanceof SQFArray) {
				// source.getErrors().add(new SQFParseException(new SQFParseException(getToken(), "Unexpected " + getToken().toString())));
			}
		}
		
		// We're going left to right
		if (right != null) {
			right.analyze(source, context);
		}
	}
}
