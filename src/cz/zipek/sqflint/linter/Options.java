/*
 * The MIT License
 *
 * Copyright 2017 Jan Zípek (jan at zipek.cz).
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
package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.output.OutputFormatter;
import cz.zipek.sqflint.output.TextOutput;
import cz.zipek.sqflint.sqf.operators.CountOperator;
import cz.zipek.sqflint.sqf.operators.ExitWithOperator;
import cz.zipek.sqflint.sqf.operators.ForEachOperator;
import cz.zipek.sqflint.sqf.operators.GenericOperator;
import cz.zipek.sqflint.sqf.operators.IfOperator;
import cz.zipek.sqflint.sqf.operators.Operator;
import cz.zipek.sqflint.sqf.operators.ParamsOperator;
import cz.zipek.sqflint.sqf.operators.PathLoader;
import cz.zipek.sqflint.sqf.operators.SetVariableOperator;
import cz.zipek.sqflint.sqf.operators.ThenOperator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public final class Options {
	private OutputFormatter outputFormatter = new TextOutput();
	private boolean stopOnError = false;
	private boolean skipWarnings = false;
	private boolean jsonOutput = false;
	private boolean outputVariables = false;
	private boolean exitCodeEnabled = false;
	private boolean warningAsError = false;
	private boolean checkPaths = false;
	private boolean contextSeparationEnabled = true;
	private boolean benchLogs = false;
	private String rootPath = null;
	
	private final Map<String, String> includePaths = new HashMap<>();
	
	private final Set<String> ignoredVariables;
	private final Set<String> skippedVariables;
	
	private final Map<String, Operator> operators = new HashMap<>();

	public Options() throws IOException {
		skippedVariables = new HashSet<>();
		ignoredVariables = new HashSet<>();
		
		ignoredVariables.addAll(Arrays.asList(new String[] {
			"_this", "_x", "_foreachindex", "_exception",
			"__file__", "__line__", "_thiseventhandler"
		}));
		
		operators.put("params", new ParamsOperator());
		operators.put("setvariable", new SetVariableOperator());
		operators.put("execvm", new PathLoader());
		operators.put("preprocessfile", new PathLoader());
		operators.put("preprocessfilelinenumbers", new PathLoader());
		operators.put("loadfile", new PathLoader());
		operators.put("if", new IfOperator());
		operators.put("then", new ThenOperator());
		operators.put("exitwith", new ExitWithOperator());
		operators.put("foreach", new ForEachOperator());
		operators.put("count", new CountOperator());
		
		loadCommands();
		
		operators.put("&&", operators.get("and"));
		operators.put("||", operators.get("or"));
	}
	
	/**
	 * Creates a copy of the options with a new output formatter
     *  (they cannot be copied and have to be provided on a per file basis i.e. serveroutput)
	 */
	public Options(Options old, OutputFormatter output) {
		this.stopOnError = old.stopOnError;
		this.skipWarnings = old.skipWarnings;
		this.jsonOutput = old.jsonOutput;
		this.outputVariables = old.outputVariables;
		this.exitCodeEnabled = old.exitCodeEnabled;
		this.warningAsError = old.warningAsError;
		this.checkPaths = old.checkPaths;
		this.contextSeparationEnabled = old.contextSeparationEnabled;
		this.benchLogs = old.benchLogs;
		this.rootPath = old.rootPath;
		
        for (String key : old.includePaths.keySet()) {
			this.includePaths.put(key, old.includePaths.get(key));
		}
		
		this.ignoredVariables = new HashSet<String>();
		this.ignoredVariables.addAll(old.ignoredVariables);
		this.skippedVariables = new HashSet<String>();
		this.skippedVariables.addAll(old.skippedVariables);
		
		for (String key : old.operators.keySet()) {
			this.operators.put(key, old.operators.get(key));
        }
        
        this.outputFormatter = output;
	}

	/**
	 * Loads commands list from resources.
	 * 
	 * @throws IOException 
	 */
	private void loadCommands() throws IOException {
		// Binary commands
		Pattern bre = Pattern.compile("(?i)b:([a-z0-9,]*) ([a-z0-9_]*) ([a-z0-9,]*)");
		// Unary commands
		Pattern ure = Pattern.compile("(?i)u:([a-z0-9_]*) ([a-z0-9,]*)");
		// Noargs commands
		Pattern nre = Pattern.compile("(?i)n:([a-z0-9_]*)");
		
		// Load commands list from jar file
		InputStream in = getClass().getResourceAsStream("/res/commands.txt"); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		// Read line by line
		String line;
		while((line = reader.readLine()) != null) {
			String ident = null;
			String[] left = null;
			String[] right = null;
			
			// Try to match one if the command regexp
			Matcher m = bre.matcher(line);
			if (m.find()) {
				ident = m.group(2).toLowerCase();
				
				left = m.group(1).split(",");
				right = m.group(3).split(",");
			}
			
			m = ure.matcher(line);
			if (m.find()) {
				ident = m.group(1).toLowerCase();
				
				right = m.group(2).split(",");
			}
			
			m = nre.matcher(line);
			if (m.find()) {
				ident = m.group(1).toLowerCase();
			}
			
			if (ident != null) {
				if (!operators.containsKey(ident)) {
					operators.put(ident, new GenericOperator(ident));
				}
				
				Operator op = operators.get(ident);
				if (op instanceof GenericOperator) {					
					GenericOperator genop = (GenericOperator)op;
					for(GenericOperator.Type ttype : convertToTypes(left)) {
						genop.addLeft(ttype);
					}
					for(GenericOperator.Type ttype : convertToTypes(right)) {
						genop.addRight(ttype);
					}
					
					if (left == null) {
						genop.allowLeftEmpty(true);
					}
					
					if (right == null) {
						genop.allowRightEmpty(true);
					}
				}
			}
		}
        
        try {
            reader.close();
        } catch (Exception e) {
            // log?
        }
    }
	
	/**
	 * Converts string type definitions to enums.
	 * @param values
	 * @return 
	 */
	private GenericOperator.Type[] convertToTypes(String[] values) {
		if (values == null) {
			return new GenericOperator.Type[0];
		}
		
		Set<GenericOperator.Type> types = new HashSet<>();
		for(String tname : values) {
			GenericOperator.Type ttype;
			
			try {
				ttype = GenericOperator.Type.valueOf(tname.toUpperCase());
			} catch(IllegalArgumentException e) {
				ttype = GenericOperator.Type.ANY;
			}
			
			types.add(ttype);
		}
		
		return types.toArray(new GenericOperator.Type[0]);
	}

	/**
	 * @return the outputFormatter
	 */
	public OutputFormatter getOutputFormatter() {
		return outputFormatter;
	}

	/**
	 * @param outputFormatter the outputFormatter to set
	 */
	public void setOutputFormatter(OutputFormatter outputFormatter) {
		this.outputFormatter = outputFormatter;
	}

	/**
	 * @return the stopOnError
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * @param stopOnError the stopOnError to set
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * @return the skipWarnings
	 */
	public boolean isSkipWarnings() {
		return skipWarnings;
	}

	/**
	 * @param skipWarnings the skipWarnings to set
	 */
	public void setSkipWarnings(boolean skipWarnings) {
		this.skipWarnings = skipWarnings;
	}

	/**
	 * @return the jsonOutput
	 */
	public boolean isJsonOutput() {
		return jsonOutput;
	}

	/**
	 * @param jsonOutput the jsonOutput to set
	 */
	public void setJsonOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
	}

	/**
	 * @return the outputVariables
	 */
	public boolean isOutputVariables() {
		return outputVariables;
	}

	/**
	 * @param outputVariables the outputVariables to set
	 */
	public void setOutputVariables(boolean outputVariables) {
		this.outputVariables = outputVariables;
	}

	/**
	 * @return the exitCodeEnabled
	 */
	public boolean isExitCodeEnabled() {
		return exitCodeEnabled;
	}

	/**
	 * @param exitCodeEnabled the exitCodeEnabled to set
	 */
	public void setExitCodeEnabled(boolean exitCodeEnabled) {
		this.exitCodeEnabled = exitCodeEnabled;
	}

	/**
	 * @return the warningAsError
	 */
	public boolean isWarningAsError() {
		return warningAsError;
	}

	/**
	 * @param warningAsError the warningAsError to set
	 */
	public void setWarningAsError(boolean warningAsError) {
		this.warningAsError = warningAsError;
	}

	/**
	 * @return the checkPaths
	 */
	public boolean isCheckPaths() {
		return checkPaths;
	}

	/**
	 * @param checkPaths the checkPaths to set
	 */
	public void setCheckPaths(boolean checkPaths) {
		this.checkPaths = checkPaths;
	}

	/**
	 * @return the rootPath
	 */
	public String getRootPath() {
		return rootPath;
	}

	/**
	 * @param rootPath the rootPath to set
	 */
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	/**
	 * @return the ignoredVariables
	 */
	public Set<String> getIgnoredVariables() {
		return ignoredVariables;
	}

	/**
	 * @return the skippedVariables
	 */
	public Set<String> getSkippedVariables() {
		return skippedVariables;
	}

	/**
	 * @return the operators
	 */
	public Map<String, Operator> getOperators() {
		return operators;
	}
	
	/**
	 * Adds list of variables to be ignored by definition checker.
	 * @param vars 
	 */
	public void addIgnoredVariables(String[] vars) {
		for(String var : vars) {
			skippedVariables.add(var.toLowerCase());
		}
	}
	
	public Map<String, String> getIncludePaths() {
		return this.includePaths;
	}

	/**
	 * @return the contextSeparationEnabled
	 */
	public boolean isContextSeparationEnabled() {
		return contextSeparationEnabled;
	}

	/**
	 * @param contextSeparationEnabled the contextSeparationEnabled to set
	 */
	public void setContextSeparationEnabled(boolean contextSeparationEnabled) {
		this.contextSeparationEnabled = contextSeparationEnabled;
	}
	
	public boolean isVariableSkipped(String name) {
		if (getIgnoredVariables().contains(name)) {
			return true;
		}
		
		for (String skipped : getSkippedVariables()) {
			if (skipped.indexOf('*') < 0) {
				if (skipped.equals(name)) {
					return true;
				}
			} else {
				String[] values = (String[])Pattern.compile("\\*")
					.splitAsStream(skipped)
					.map(v -> Pattern.quote(v))
					.toArray();
				String matcher = String.join("(.*)", values);
				if (Pattern.compile(matcher).matcher(name).matches()) {
					return true;
				}
			}
		}
		
		return false;
	}

	public boolean isBenchLogs() {
		return benchLogs;
	}

	public void setBenchLogs(boolean benchLogs) {
		this.benchLogs = benchLogs;
	}
}
