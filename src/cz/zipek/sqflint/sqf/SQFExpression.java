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
import cz.zipek.sqflint.linter.SQFParseException;
import cz.zipek.sqflint.linter.Warning;
import cz.zipek.sqflint.parser.Token;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kamen
 */
public class SQFExpression extends SQFUnit {
	static int idCounter = 0;
	static Map<String, SQFExpression> called = new HashMap<>();
	
	private final Token token;
	private final int id;
	
	private SQFUnit main;
	private SQFExpression left;
	private SQFExpression right;
	
	private final List<String> signOperators = Arrays.asList("+", "-", "!");
	
	private SQFParseException sentError;
	
	public SQFExpression(Linter linter, Token token) {
		super(linter);
		id = idCounter++;
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

	public SQFExpression finish() {
		return finish(false);
	}
	
	public SQFExpression finish(boolean revalidate) {
		if (revalidate) {
			if (getRight() != null) {
				getRight().finish(revalidate);
			}
			
			if (main instanceof SQFExpression) {
				((SQFExpression)main).finish(revalidate);
			}
			
			if (isBlock()) {
				getBlock().revalidate();
			}
		}
		
		// Remove previous error if there is any
		if (sentError != null) {
			if (sentError instanceof Warning) {
				linter.getWarnings().remove((Warning)sentError);
			} else {
				linter.getErrors().remove(sentError);
			}
		}
		
		// If main part of expression is identifier, try to run command
		if (main != null && main instanceof SQFIdentifier) {
			// Load main part of this expression
			SQFIdentifier mainIdent = (SQFIdentifier)main;
			String ident = mainIdent.getToken().image.toLowerCase();
			
			if (linter.getOptions().getOperators().containsKey(ident)) {
				linter.getOptions().getOperators().get(ident).analyze(
					linter,
					context,
					this
				);
			} else if (
				!linter.getPreprocessor().getMacros().containsKey(ident)
				&& !linter.getOptions().getIgnoredVariables().contains(ident)
			) {
				boolean isPrivate = left != null && left.isPrivate();
				boolean isAssigment = right != null && right.isAssignOperator();
				
				sentError = context.handleName(
					mainIdent.getToken(),
					isAssigment,
					isPrivate
				);
			}
		}
		
		if (!isCommand() && !isOperator()) {
			if (right != null && !right.isOperator() && !right.isCommand()) {
				linter.getErrors().add(new SQFParseException(
					right.getToken(),
					main + " and " + right.main + " is not a valid combination of expressions."
				));
			}
		}
		
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
			SQFIdentifier mainToken = (SQFIdentifier)main;
			return mainToken.getToken().image.toLowerCase();
		}
		return null;
	}
	
	public boolean isCommand() {
		return (getIdentifier() != null && linter.getOptions().getOperators().containsKey(getIdentifier()));
	}
	
	public boolean isBlock() {
		return main != null && main instanceof SQFBlock;
	}
	
	public SQFBlock getBlock() {
		if (isBlock()) {
			return (SQFBlock)main;
		}
		return null;
	}
	
	public boolean isVariable(Linter source) {
		return (getIdentifier() != null && !isCommand());
	}
	
	public boolean isOperator() {
		return main != null && main instanceof SQFOperator;
	}
	
	public boolean isSignOperator() {
		return isOperator() && signOperators.contains(main.toString());
	}
	
	public boolean isPrivate() {
		return getIdentifier() != null && getIdentifier().equals("private");
	}
	
	public boolean isAssignOperator() {
		return isOperator() && main.toString().equals("=");
	}

	@Override
	public void analyze(Linter source, SQFBlock context) {
		// Do some analytics on inside (probably won't do anything)
		if (main != null) {
			main.analyze(source, context);
		}
		
		// We're going left to right
		if (right != null) {
			right.analyze(source, context);
		}
	}

	@Override
	public String toString() {
		return "Expression(" + main + ", #" + id + ")";
	}
}
