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
	private final String arguments;
	private final List<SQFMacroDefinition> definitions = new ArrayList<>();
	private final String source;
	private final int line;
	
	public SQFMacro(String name, String arguments, String source, int line) {
		this.name = name;
		this.arguments = arguments;
		this.source = source;
		this.line = line;
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

	/**
	 * @return the arguments
	 */
	public String getArguments() {
		return arguments;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return line;
	}
        
        public boolean matchAt(String matchLine, int index) {
                // If the remaining text in the line is less than the macro name length we abort
                if(matchLine.length() <= index + name.length()) return false;
                // If the next or prev char is a valid identifier char then we are looking in the middle of an identifier and
                // should not do macro replacement. Do it before we do any more expensive string manipulation.
                if(index > 0 && isIdentifierChar(matchLine.charAt(index-1))) return false;
                if(index + name.length() < matchLine.length()  && isIdentifierChar(matchLine.charAt(index + name.length()))) return false;
                // Get the part of the line that we want to check and compare it
                String part = matchLine.substring(index, index + name.length());
                if(!part.equals(name)) return false;
                return true;
        }
               
        private boolean isIdentifierChar(char f) {
                return Character.isLetterOrDigit(f) || f == '_';
	}
}
