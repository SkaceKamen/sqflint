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
package cz.zipek.sqflint.preprocessor;

import cz.zipek.sqflint.SQFLint;
import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.Options;
import cz.zipek.sqflint.linter.Warning;
import cz.zipek.sqflint.parser.Token;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class SQFPreprocessor {
	private final Map<String, SQFMacro> macros = new HashMap<>();
	private final List<SQFInclude> includes = new ArrayList<>();
	private final List<SQFMacro> sortedMacros = new ArrayList<>();
	
	private final List<Warning> warnings = new ArrayList<>();
	
	private final Options options;
	
	private int readUntilIndex;
	
	public SQFPreprocessor(Options options) {
		this.options = options;
	}
	
	public String process(InputStream stream, String source, boolean include_filename) throws Exception {
		return process(new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n")), source, include_filename);
	}
	
	
	public String process(String input, String source, boolean include_filename) throws Exception {
		Path root = Paths.get(source).toAbsolutePath().getParent();
		
		// Fixes escaped newlines
		int index = 0;
		while (index + 2 < input.length()) {
			if (input.substring(index, index + 2).equals("\\\n")) {
				int lines = 0;
				int end = index;
				while (end < input.length()) {
					if (end < input.length() - 1 && input.substring(end, end + 2).equals("\\\n")) {
						lines++;
						end += 2;
					}
					if (input.charAt(end) == '\n') {
						break;
					}
					end++;
				}
				
				input = input.substring(0, index) +
					input.substring(index + 2, end).replaceAll("\\\\\n", "") +
					String.join("", Collections.nCopies(lines, "\n")) +
					input.substring(end);
			}
			
			index++;
		}
		
		String[] lines = input
			.replaceAll("\r", "")
			// Join all lines with trailing backslash. 
			// This isn't perfect: it would join a comment line with a trailing \ as well.
			// ? Also it messes with line numbers maybe ?
			.replaceAll("\\\\\n", "")
			.split("\n");
		
		String output = input;
		int lineIndex = 0;
		
		Pattern whitespaceAtStart = Pattern.compile("^\\s*");
		Pattern doubleWhitespace = Pattern.compile("\\s{1,}");
		Pattern comments = Pattern.compile("(\\/\\*.*?\\*\\/)|(\\/\\/.*)");
		
		boolean inComment = false;
                
		for (String line : lines) {	
			// Remove whitespaces at beginning
			String lineUpdated = whitespaceAtStart
					.matcher(line)
					.replaceAll("");
			
			// Remove doubled whitespaces
			lineUpdated = doubleWhitespace
					.matcher(lineUpdated)
					.replaceAll(" ");
			
			// Remove comments
			lineUpdated = comments
					.matcher(lineUpdated)
					.replaceAll("");
			
			// Handle multiline comments
			// @TODO: Escapes in comments?
			// Is there beginning of the comment and not the end on this line?
			if (lineUpdated.contains("/*")) {
				// Remove everyhing past the comment start
				lineUpdated = lineUpdated
					.substring(0, lineUpdated.indexOf("/*"));
				// Next line is inside comment
				inComment = true;
			}
			
			boolean replaceInComment = false;
			// @TODO: Escapes in comments?
			if (inComment) {
				// Make sure the macro replacer knows it is starting in a comment.
				replaceInComment = true;
				if (lineUpdated.contains("*/")) {
					lineUpdated = lineUpdated
						.substring(lineUpdated.indexOf("*/") + 2);
					inComment = false;
				} else {
					lines[lineIndex++] = line;
					continue;
				}
			}
			
			// Remove tabs
			lineUpdated = lineUpdated.replaceAll("\t", " ");
		
			if (lineUpdated.length() > 0 && lineUpdated.charAt(0) == '#') {
				// Remove line for grammar parser
				line = "";

				// Parse the line
				String word = readUntil(lineUpdated, 1, ' ', false, false);
				String values = readUntil(lineUpdated, 2 + word.length(), '\n', true, false);
					
				switch(word.toLowerCase()) {
					case "define":
						String ident = readUntil(values, 0, new char[] { ' ', '\t' }, true, true);
						String value = null;
						String arguments = null;
						
						// Only load value if there is any
						if (values.length() > ident.length() + 1) {
							value = values.substring(ident.length() + 1).trim();
						}

						// Parse argumented macro
						if (ident.indexOf('(') >= 0) {
							arguments = ident.substring(ident.indexOf('(') + 1);
							if (arguments.indexOf(')') >= 0) {
								arguments = arguments.substring(0, arguments.indexOf(')'));
							}
							ident = ident.substring(0, ident.indexOf('('));
						}
						
						Token token = new Token(Linter.STRING_LITERAL);
						token.beginLine = lineIndex + 1;
						token.endLine = lineIndex + 1;
						token.beginColumn = 1;
						token.endColumn = values.length() + 1;
						
						if (!macros.containsKey(ident)) {
							macros.put(ident, new SQFMacro(ident, arguments, source, lineIndex));
							sortedMacros.add(macros.get(ident));
						}
						
						macros.get(ident).addDefinition(
							include_filename ? source : null,
							token,
							value
						);
						
						break;
					case "include":
						values = readUntil(lineUpdated, 2 + word.length(), '\n', false, false);
						String filename = values.trim();
						if (filename.length() > 0) {
							String originalPath = filename.substring(1, filename.length() - 1);
							String actualPath = resolvePath(originalPath).replaceAll("\\\\", "/");							
							Path path = root.resolve(actualPath);

							getIncludes().add(
								new SQFInclude(originalPath, actualPath, source)
							);

							if (Files.exists(path) && !Files.isDirectory(path)) {
								process(new FileInputStream(path.toString()), path.toString(), true);
							} else if (options.isCheckPaths()) {
								warnings.add(
									new Warning(
										include_filename ? source : null,
										buildToken(
											lineIndex + 1,
											lineIndex + 1,
											1 + "#include ".length(),
											line.length() + 1
										),
										String.format(
											"File %s doesn't seem to exists.",
											path.toString()
										)
									)
								);
							}
						}
						
						break;
					case "ifdef": break;
					case "ifndef": break;
					case "undef": break;
					case "else": break;
				}
			} else if (!inComment) {
				try {
					sortedMacros.sort((a, b) -> b.getName().length() - a.getName().length());
					
					int replaceIndex = 0;
					boolean replaceInString = false;
					String stringLimiter = "\"";
					while (replaceIndex < line.length()) {
						if (replaceInString) {
							if (replaceIndex < line.length() - 2 && line.substring(replaceIndex, replaceIndex + 2).equals(stringLimiter + stringLimiter)) {
								replaceIndex += 2;
							} else if (line.substring(replaceIndex, replaceIndex + 1).equals(stringLimiter)) {
								replaceInString = false;
							}
							replaceIndex++;
						} else if (replaceInComment) {
							if (replaceIndex < line.length() - 2 && line.substring(replaceIndex, replaceIndex + 2).equals("*/")) {
								replaceIndex += 2;
								replaceInComment = false;
							} else {
								replaceIndex++;
							}
						} else if (replaceIndex < line.length() - 2 && line.substring(replaceIndex, replaceIndex + 2).equals("/*")) {
							replaceIndex += 2;
							replaceInComment = true;
						} else if (line.substring(replaceIndex, replaceIndex + 1).equals("\"")) {
							stringLimiter = "\"";
							replaceIndex++;
							replaceInString = true;
						} else if (line.substring(replaceIndex, replaceIndex + 1).equals("'")) {
							stringLimiter = "'";
							replaceIndex++;
							replaceInString = true;
						} else if (replaceIndex < line.length() - 2 && line.substring(replaceIndex, replaceIndex + 2).equals("//")) {
							// Rest of the line is a comment, so skip it
							break;
						} else {
							boolean replaced = true;

							// Cap macro recursion depth to 10.
							int depth = 0;
							while (replaced && depth < 20) {
								replaced = false;
								for (SQFMacro macro : sortedMacros) {
									if (macro.matchAt(line, replaceIndex)) {
										line = line.substring(0, replaceIndex) +
											replaceMacro(line.substring(replaceIndex), macro);
										replaced = true;
										depth++;
										break;
									}
								}
							}

							// We only increment the index if we didn't perform any macro replacement.
							// If we DID then we need to check for strings etc.
							if (depth == 0) {
								replaceIndex++;
							}
						}
					}
					

					// System.out.println("#" + lineIndex + "\t" + line);
					
				} catch (Exception ex) {
					Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, "Failed to parse line " + lineIndex + " of " + source, ex);
					System.exit(1);
				}
				
				// System.out.println("#" + lineIndex + "\t" + line);
			}
			
			lines[lineIndex++] = line;
		}
		
		
		return String.join("\n", lines);
	}
	
	private Token buildToken(int lineStart, int lineEnd, int columnStart, int columnEnd) {
		Token token = new Token(Linter.STRING_LITERAL);
		token.beginLine = lineStart;
		token.endLine = lineEnd;
		token.beginColumn = columnStart;
		token.endColumn = columnEnd;
		return token;
	}
	
	/**
	 * Tries to match specified path against include paths.
	 * @param path
	 * @return updated path or original if no include path has been matched
	 */
	private String resolvePath(String path) {
		for (String key : options.getIncludePaths().keySet()) {
			if (path.toLowerCase().indexOf(key.toLowerCase()) == 0) {
				return options.getIncludePaths().get(key) +
						path.substring(key.length());
			}
		}
		
		return path;
	}
	
	private int parseParams(String input, ArrayList<String> params) {
		int bracket = 0;
		int index = 0;
		boolean inString = false;
		boolean inComment = false;
		String stringLimiter = "\"";
		int paramStart = 0;
		while (index < input.length()) {
			if (inString) {
				if (index < input.length() - 2 && input.substring(index, index + 2).equals(stringLimiter + stringLimiter)) {
					index += 2;
				} else if (input.substring(index, index + 1).equals(stringLimiter)) {
					inString = false;
				}
				index++;
			} else if (inComment) {
				if (index < input.length() - 2 && input.substring(index, index + 2).equals("*/")) {
					index += 2;
					inComment = false;
				} else {
					index++;
				}
			} else if (index < input.length() - 2 && input.substring(index, index + 2).equals("/*")) {
				index += 2;
				inComment = true;
			} else if (input.substring(index, index + 1).equals("\"")) {
				stringLimiter = "\"";
				index++;
				inString = true;
			} else if (input.substring(index, index + 1).equals("'")) {
				stringLimiter = "'";
				index++;
				inString = true;
			} else if (index < input.length() - 2 && input.substring(index, index + 2).equals("//")) {
				// Rest of the input is a comment, so skip it
				break;
			} else {
				char ch = input.charAt(index);
				if (ch == '(') {
					bracket++;
				} else if (ch == ')') {
					bracket--;
					if (bracket < 0) {
						params.add(input.substring(paramStart, index));
						break;
					}
				}
				
				if(bracket == 0 && ch == ',') {
					params.add(input.substring(paramStart, index));
					paramStart = index + 1;
				}
				index++;
			}
				
		}
		return index;
	}
        
	private int walkToEnd(String input) {
		int index = 0;
		int bracket = 0;

		while (index < input.length()) {
			if (input.charAt(index) == '(') {
				bracket++;
			} else if (input.charAt(index) == ')') {
				bracket--;
			}

			if (bracket < 0) {
				return index;
			}

			index++;
		}
		return -1;
	}
	
	private String replaceMacro(String line, SQFMacro macro) {
		int index = line.indexOf(macro.getName());
		String value = null;
		
		if (!macro.getDefinitions().isEmpty()) {
			value = macro.getDefinitions().get(macro.getDefinitions().size() - 1).getValue();
		}
		
		if (value == null) {
			value = "";
		}
		
		if (macro.getArguments() == null) {
			/*
			System.out.println("At line: '" + line + "'");
			System.out.println("MACRO: " + macro.getName());
			System.out.println("VALUE: " + value);
			*/
			line = line.substring(0, index) + value + line.substring(index + macro.getName().length());
		} else {
			String[] arguments = macro.getArguments().split(",");
			int startArgs = line.indexOf('(', index + macro.getName().length());
			if(startArgs == -1 || !line.substring(index + macro.getName().length(), startArgs).trim().isEmpty())
			{
				// Failure, the macro expects arguments but they have 
				// not been correctly provided (params across more than 
				// one line for a macro is not supported).
				// We will remove the macro name to avoid it recursing
				return line.substring(index + macro.getName().length());
			}
			String values = line.substring(startArgs + 1);

			ArrayList<String> args = new ArrayList<String>();
			int argsClose = parseParams(values, args);
			if(args.size() != arguments.length) {
				return line.substring(index + macro.getName().length());
			}

			// Index of next char after closing paren of argument list
			int pastEndOfMacro = startArgs + 1 + argsClose + 1;
			
			// This is completely wrong, but #YOLO
			// (I actually don't want to spend much time on this, because #YOLO)
			// This works somewhat, so deal with it
			for (int i = 0; i < arguments.length && i < args.size(); i++) {
				String argName = arguments[i].trim();
				String argValue = args.get(i).trim();
				String noletter = "([^a-zA-Z_#])";
				
				// @TODO: There has to be other way :O
				value = value.replaceAll("##" + argName + "##", argValue);
				
				value = value.replaceAll("^" + argName + "##", argValue);
				value = value.replaceAll(noletter + argName + "##", "$1" + argValue);
				
				value = value.replaceAll("##" + argName + "$", argValue);
				value = value.replaceAll("##" + argName + noletter, argValue + "$1");
				
				value = value.replaceAll("#" + argName + "$", '"' + argValue + '"');
				value = value.replaceAll("#" + argName + noletter, '"' + argValue + "\"$1");
				
				value = value.replaceAll("^" + argName + "$", argValue);
				value = value.replaceAll("^" + argName + noletter, argValue + "$1");
				value = value.replaceAll(noletter + argName + "$", "$1" + argValue);
				value = value.replaceAll(noletter + argName + noletter, "$1" + argValue + "$2");
			}
			
			value = value.replaceAll("##", "");

			/*
			System.out.println("At line: '" + line + "'");
			System.out.println("MACRO: '" + macro.getName() + "'");
			System.out.println("ARGS: '" + macro.getArguments() + "'");
			System.out.println("VALS: '" + values + "'");
			System.out.println("REPLACE: " + value);
			*/
			
			String left = line.substring(0, index);
			String right = line.substring(pastEndOfMacro);
			
			/*
			System.out.println("LEFT: " + left);
			System.out.println("RIGHT: " + right);
			*/
			
			line = left + value + right;
		}
		
		return line;
	}

	private String readUntil(String input, int from, char exit, boolean escape, boolean brackets) {
		return readUntil(input, from, new char[] { exit }, escape, brackets);
	}
		
	private boolean inCharList(char f, char[] list) {
		for (int i = 0; i < list.length; i++) {
			if (list[i] == f) return true;
		}
		return false;
	}
	
	private String readUntil(String input, int from, char[] exit, boolean escape, boolean brackets) {
		StringBuilder res = new StringBuilder();
		boolean escaped = false;
		
		while(input.length() > from && (escaped || !inCharList(input.charAt(from), exit))) {	
			if (!escaped && (!escape || input.charAt(from) != '\\')) {
				res.append(input.charAt(from));
			}

			escaped = false;
			if (escape && input.charAt(from) == '\\') {
				escaped = true;
			}
			
			if (brackets && input.charAt(from) == '(') {
				int endIndex = walkToEnd(input.substring(from + 1));
				if (endIndex >= 0) {
					res.append(input.substring(from + 1, from + 2 + endIndex));
					from += endIndex + 1;
				}
			}
			
			from++;
		}
		
		readUntilIndex = from;
		
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

	/**
	 * @return the warnings
	 */
	public List<Warning> getWarnings() {
		return warnings;
	}
}
