package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.output.JSONOutput;
import cz.zipek.sqflint.output.OutputFormatter;
import cz.zipek.sqflint.output.TextOutput;
import cz.zipek.sqflint.parser.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
 * @author Jan Zípek <jan at zipek.cz>
 */
public class Linter extends SQFParser {
	private boolean stopOnError = false;
	private boolean skipWarnings = false;
	private boolean jsonOutput = false;
	private boolean outputVariables = false;
	
	private final Map<String, SQFCommand> commands = new HashMap<>();
	private final Set<String> ignoredVariables = new HashSet<>();
	
	private final List<SQFParseException> errors = new ArrayList<>();
	private final List<Warning> warnings = new ArrayList<>();
	private final Map<String, SQFVariable> variables = new HashMap<>();
	
	public Linter(InputStream stream) {
		super(stream);
		
		ignoredVariables.addAll(Arrays.asList(new String[] { "_this", "_x", "_foreachindex" }));
	}
	
	public void start() throws IOException {
		if (jsonOutput)
			setTabSize(1);
		
		loadCommands();
		
		try {
			CompilationUnit();
			postParse();
			
			OutputFormatter out;
			if (jsonOutput) {
				out = new JSONOutput();
			} else {
				out = new TextOutput();
			}
			
			out.print(this);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}
	}
	
	protected void postParse() {
		if (skipWarnings)
			return;
		
		variables.entrySet().stream().forEach((entry) -> {
			SQFVariable var = entry.getValue();
			if (var.isLocal() && var.definitions.isEmpty()) {
				var.usage.stream().forEach((u) -> {
					getWarnings().add(new Warning(u, "Possibly undefined variable " + u));
				});
			}
		});
	}
	
	@Override
	protected void handleName() throws ParseException {
		// Load current token
		Token name = getToken(1);
		
		// Convert to ident (SQF is case insensitivie)
		String ident = name.toString().toLowerCase();
		
		// If name is exisiting command, do some tests
		// Otherwise, handle variable
		if (getCommands().containsKey(ident)) {
			SQFCommand cmd = getCommands().get(ident);
			cmd.test(name, this);
		} else if (!ignoredVariables.contains(ident)) {
			SQFVariable var;
			if (!variables.containsKey(ident)) {
				var = new SQFVariable(ident);
				variables.put(ident, var);
			} else {
				var = getVariables().get(ident);
			}
			
			var.usage.add(name);

			if (getToken(2).kind == ASSIGN) {
				var.definitions.add(name);

				if (name.specialToken != null) {
					var.comments.add(name.specialToken);
				} else {
					var.comments.add(null);
				}
			}
		}
	}
	
	/**
	 * Tries to recover from error if enabled.
	 * This allows us to catch more errors per file. Adds error to list of encountered problems.
	 * 
	 * @param ex
	 * @param recoveryPoint 
	 * @throws cz.zipek.sqflint.parser.ParseException 
	 */
	@Override
	protected void recover(ParseException ex, int recoveryPoint) throws ParseException {
		// Add to list of encountered errors
		if (!(ex instanceof SQFParseException)) {
			getErrors().add(new SQFParseException(ex));
		} else {
			getErrors().add((SQFParseException)ex);
		}
		
		// Don't actually recover if needed
		if (stopOnError) {
			throw ex;
		}
		
		// Try to move to next token of specified kind
		Token t;
		do {
			t = getNextToken ();
		} while (t.kind != EOF && t.kind != recoveryPoint);
	}
	
	/**
	 * Loads commands list from resources.
	 * 
	 * @throws IOException 
	 */
	protected void loadCommands() throws IOException {
		// Binary commands
		Pattern bre = Pattern.compile("(?i)b:([a-z,]*) ([a-z0-9_]*) ([a-z0-9,]*)");
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
			SQFCommand.Type type = null;
			
			// Try to match one if the command regexp
			Matcher m = bre.matcher(line);
			if (m.find()) {
				ident = m.group(2).toLowerCase();
				type = SQFCommand.Type.Binary;
			}
			
			m = ure.matcher(line);
			if (m.find()) {
				ident = m.group(1).toLowerCase();
				type = SQFCommand.Type.Unary;
			}
			
			m = nre.matcher(line);
			if (m.find()) {
				ident = m.group(1).toLowerCase();
				type = SQFCommand.Type.Noargs;
			}
			
			if (ident != null) {
				getCommands().put(ident, new SQFCommand(ident, type));
			}
		}
	}
	
	/**
	 * @param stopOnError the stopOnError to set
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * @param skipWarnings the skipWarnings to set
	 */
	public void setSkipWarnings(boolean skipWarnings) {
		this.skipWarnings = skipWarnings;
	}

	/**
	 * @param jsonOutput the jsonOutput to set
	 */
	public void setJsonOutput(boolean jsonOutput) {
		this.jsonOutput = jsonOutput;
	}

	/**
	 * @return the skipWarnings
	 */
	public boolean isSkipWarnings() {
		return skipWarnings;
	}

	/**
	 * @return the commands
	 */
	public Map<String, SQFCommand> getCommands() {
		return commands;
	}

	/**
	 * @return the ignoredVariables
	 */
	public Set<String> getIgnoredVariables() {
		return ignoredVariables;
	}

	/**
	 * @return the errors
	 */
	public List<SQFParseException> getErrors() {
		return errors;
	}

	/**
	 * @return the variables
	 */
	public Map<String, SQFVariable> getVariables() {
		return variables;
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
	 * @return the warnings
	 */
	public List<Warning> getWarnings() {
		return warnings;
	}
}
