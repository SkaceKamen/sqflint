/*
 * The MIT License
 *
 * Copyright 2018 Jan Zípek (jan at zipek.cz).
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
 * @author Jan Zípek (jan at zipek.cz)
 */
public class SQFWithStatement extends SQFUnit {
	private final SQFExpression with;
	private final SQFBlock block;
	
	public SQFWithStatement(Linter linter, SQFExpression cond, SQFBlock bl) {		
		super(linter);
		
		with = cond;
		block = bl;
	}

	@Override
	public void analyze(Linter source, SQFBlock context) {
		if (with != null) with.analyze(source, context);
		if (block != null) block.analyze(source, context);
	}

	@Override
	public void revalidate() {
		if (with != null) with.revalidate();
		if (block != null) block.revalidate();
	}
}
