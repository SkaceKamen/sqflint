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
import cz.zipek.sqflint.linter.Warning;
import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.sqf.SQFExpression;
import cz.zipek.sqflint.sqf.SQFString;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class PathLoader extends Operator {
	@Override
	public void analyze(Linter source, SQFBlock context, SQFExpression expression) {
		// Check for existence of loaded file, if allowed
		if (source.isCheckPaths() && source.getRootPath() != null) {
			if (expression.getRight() != null
					&& expression.getRight().getMain() != null
					&& expression.getRight().getMain() instanceof SQFString
			) {
				SQFString param = (SQFString)expression.getRight().getMain();
				String path = param.getStringContents();
				
				// Don't check absolute paths (referring addons and internal arma files)
				// @TODO: Create list of internal files?
				if (path.charAt(0) != '/' && !Files.exists(Paths.get(source.getRootPath(), path))) {
					source.getWarnings().add(new Warning(param.getContents(), "File '" + param.getStringContents() + "' doesn't exists."));
				}
			}
		}
	}
}
