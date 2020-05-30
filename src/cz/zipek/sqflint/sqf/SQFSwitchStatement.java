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
package cz.zipek.sqflint.sqf;

import cz.zipek.sqflint.linter.Linter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class SQFSwitchStatement extends SQFUnit {
	private final List<SQFCaseStatement> cases = new ArrayList<>();
	private SQFExpression expression;
	private SQFBlock defaultBlock;

	public SQFSwitchStatement(Linter linter) {
		super(linter);
	}

	public void setExpression(SQFExpression exp) {
		expression = exp;
	}
	
	public void setDefault(SQFBlock blo) {
		defaultBlock = blo;
	}
	
	public SQFSwitchStatement add(SQFCaseStatement c) {
		cases.add(c);
		return this;
	}

	@Override
	public void analyze(Linter source, SQFBlock context) {
		if (expression != null) expression.analyze(source, context);
		
		for (SQFCaseStatement c : cases) {
			c.analyze(source, context);
		}
		
		if (defaultBlock != null) defaultBlock.analyze(source, context);
	}

	@Override
	public void revalidate() {
		if (expression != null) expression.revalidate();
		
		for (SQFCaseStatement c : cases) {
			c.revalidate();
		}
		
		if (defaultBlock != null) defaultBlock.revalidate();
	}
}
