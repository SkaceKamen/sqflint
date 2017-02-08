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
package cz.zipek.sqflint.preprocessor;

import cz.zipek.sqflint.parser.Token;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFMacro {
	private final String name;
	private final List<SQFMacroDefinition> definitions = new ArrayList<>();
	private final String source;
	
	public SQFMacro(String name, String source) {
		this.name = name;
		this.source = source;
	}

	public void addDefinition(String filename, Token token, String value) {
		definitions.add(new SQFMacroDefinition(filename, token, value));
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the definitions
	 */
	public List<SQFMacroDefinition> getDefinitions() {
		return definitions;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}
}
