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
import cz.zipek.sqflint.sqf.SQFArray;
import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.sqf.SQFExpression;
import cz.zipek.sqflint.sqf.SQFString;

/**
 * Params operator can define new variables throught his params.
 * 
 * @author Jan Zípek <jan at zipek.cz>
 */
public class ParamsOperator extends Operator {
	@Override
	public void analyze(Linter source, SQFBlock context, SQFExpression expression) {
		SQFExpression right = expression.getRight();
		if (right != null && right.getMain() != null) {
			if (right.getMain() instanceof SQFArray) {
				handleParams(source, (SQFArray)right.getMain());
			}
		}
	}
	
	protected void handleParams(Linter source, SQFArray contents) {
		// List each item of params
		for(SQFExpression item : contents.getItems()) {
			// Empty expression can happen
			if (item.getMain() != null) {
				// Literal, presumably string = variable ident
				if (item.getMain() instanceof SQFString) {
					handleParamLiteral(source, (SQFString)item.getMain());
				}

				// Array = param with additional options, variable ident is first
				if (item.getMain() instanceof SQFArray) {
					SQFArray array = (SQFArray)item.getMain();
					if (!array.getItems().isEmpty()) {
						SQFExpression subitem = array.getItems().get(0);
						if (subitem.getMain() != null && subitem.getMain() instanceof SQFString) {
							handleParamLiteral(source, (SQFString)subitem.getMain());
						}
					}
				}
			}
		}
	}
	
	/**
	 * Registers literal contents as variable definition
	 * 
	 * @param literal possible variable definition
	 * @return if literal was variable definition
	 */
	private boolean handleParamLiteral(Linter source, SQFString literal) {
		// Load variable name without quotes and case insensitive
		String ident = literal.getStringContents().toLowerCase();

		// Load variable
		SQFVariable var = source.getVariable(ident);

		var.usage.add(literal.getContents());
		var.definitions.add(literal.getContents());
		var.comments.add(null);

		return true;
	}
}
