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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFPreprocessor {
	private List<SQFMacro> macros = new ArrayList<>();
	private List<SQFInclude> includes = new ArrayList<>();
	
	public SQFPreprocessor() {
		
	}
	
	public String process(InputStream stream) throws Exception {
		return process(new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n")));
	}
	
	public String process(String input) throws Exception {
		String[] lines = input.replace("\r", "").split("\n");
		String output = input;
		
		for(String line : lines) {
			if (line.length() > 0 && line.charAt(0) == '#') {
				String word = readUntil(line, 1, ' ');
				String values = readUntil(line, 1 + word.length(), '\n', true);
				switch(word.toLowerCase()) {
					case "define":
						Pattern wht = Pattern.compile("\\s{1,}");
						String[] parts = wht.matcher(values).replaceAll(" ").split(" ");
						String ident = parts[0];
						String value = "";
						for(int i = 1; i < parts.length; i++) {
							value += parts[i] + " ";
						}
						value = value.trim();
						getMacros().add(new SQFMacro(ident, value));
						break;
					case "include":
						getIncludes().add(new SQFInclude(values.trim()));
						break;
					case "ifdef": break;
					case "ifndef": break;
					case "undef": break;
					case "else": break;
				}
			}
		}
		
		return output;
	}
	
	private String readUntil(String input, int from, char exit) {
		return readUntil(input, from, exit, false);
	}
	
	private String readUntil(String input, int from, char exit, boolean escape) {
		StringBuilder res = new StringBuilder();
		boolean escaped = false;
		while(input.length() > from && (escaped || input.charAt(from) != exit)) {
			res.append(input.charAt(from));
			escaped = false;
			if (escape && input.charAt(from) == '\\') {
				escaped = true;
			}
			from++;
		}
		return res.toString();
	}

	/**
	 * @return the macros
	 */
	public List<SQFMacro> getMacros() {
		return macros;
	}

	/**
	 * @return the includes
	 */
	public List<SQFInclude> getIncludes() {
		return includes;
	}
}
