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

/**
 *
 * @author kamen
 */
public class SQFIfStatement extends SQFUnit {
	private final SQFExpression condition;
	private final SQFBlock trueBlock;
	private final SQFBlock falseBlock;
	
	public SQFIfStatement(Linter linter, SQFExpression condition, SQFBlock t, SQFBlock f) {
		super(linter);
		this.condition = condition;
		trueBlock = t;
		falseBlock = f;
	}

	@Override
	public void analyze(Linter source, SQFBlock context) {
		condition.analyze(source, context);
		
		if (trueBlock != null) trueBlock.analyze(source, context);
		if (falseBlock != null) falseBlock.analyze(source, context);
	}
	
	public void revalidate() {
		this.condition.finish(true);
		if (trueBlock != null) trueBlock.revalidate();
		if (falseBlock != null) falseBlock.revalidate();
	}
}
