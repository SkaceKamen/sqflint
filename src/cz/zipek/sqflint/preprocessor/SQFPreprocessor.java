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

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.parser.Token;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFPreprocessor {
	private final Map<String, SQFMacro> macros = new HashMap<>();
	private final List<SQFInclude> includes = new ArrayList<>();
	
	public SQFPreprocessor() {
		
	}
	
	public String process(InputStream stream, String source, boolean include_filename) throws Exception {
		return process(new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n")), source, include_filename);
	}
	
	
	public String process(String input, String source, boolean include_filename) throws Exception {
		Path root = Paths.get(source).toAbsolutePath().getParent();
		
		String[] lines = input.replace("\r", "").split("\n");
		String output = input;
		int lineIndex = 0;
		
		Pattern whitespaceAtStart = Pattern.compile("^\\s*");
		Pattern doubleWhitespace = Pattern.compile("\\s{1,}");
		
		for(String line : lines) {
			// Remove whitespaces at beginning
			line = whitespaceAtStart.matcher(line).replaceAll("");
			if (line.length() > 0 && line.charAt(0) == '#') {
				// Parse the line
				String word = readUntil(line, 1, ' ');
				String values = readUntil(line, 2 + word.length(), '\n', true);
				
				switch(word.toLowerCase()) {
					case "define":
						String ident = readUntil(values, 0, ' ');
						String value = null;
						
						if (values.length() > ident.length() + 1) {
							value = values.substring(ident.length() + 1).trim();
						}

						Token token = new Token(Linter.STRING_LITERAL);
						token.beginLine = lineIndex + 1;
						token.endLine = lineIndex + 1;
						token.beginColumn = 1;
						token.endColumn = values.length() + 1;
						
						if (!macros.containsKey(ident.toLowerCase())) {
							macros.put(ident.toLowerCase(), new SQFMacro(ident, source));
						}
						
						macros.get(ident.toLowerCase()).addDefinition(
							include_filename ? source : null,
							token,
							value
						);
						
						break;
					case "include":
						String filename = values.trim();
						SQFInclude include = new SQFInclude(filename.substring(1, filename.length() - 1), source);
						Path path = root.resolve(include.getFile());
						
						getIncludes().add(include);
						
						if (Files.exists(path)) {
							process(new FileInputStream(path.toString()), path.toString(), true);
						}
						
						break;
					case "ifdef": break;
					case "ifndef": break;
					case "undef": break;
					case "else": break;
				}
			}
			lineIndex++;
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
	public Map<String, SQFMacro> getMacros() {
		return macros;
	}

	/**
	 * @return the includes
	 */
	public List<SQFInclude> getIncludes() {
		return includes;
	}
}
