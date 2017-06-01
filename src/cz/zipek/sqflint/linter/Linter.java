package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.parser.ParseException;
import cz.zipek.sqflint.parser.SQFParser;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.parser.TokenMgrError;
import cz.zipek.sqflint.preprocessor.SQFInclude;
import cz.zipek.sqflint.preprocessor.SQFMacro;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class Linter extends SQFParser {
	public static final int CODE_OK = 0;
	public static final int CODE_ERR = 1;
	
	private final List<SQFParseException> errors = new ArrayList<>();
	private final List<Warning> warnings = new ArrayList<>();
	private final Map<String, SQFVariable> variables = new HashMap<>();
	
	private final List<SQFInclude> includes = new ArrayList<>();
	private final List<SQFMacro> macros = new ArrayList<>();
		
	private SQFPreprocessor preprocessor;
	private Options options;
	
	public Linter(InputStream stream, Options options) {
		super(stream);
		
		this.options = options;
	}
	
	public int start() throws IOException {
		setTabSize(1);
		
		SQFBlock block = null;
		
		try {
			block = CompilationUnit();
		} catch (ParseException | TokenMgrError  e) {
			if (e instanceof SQFParseException) {
				getErrors().add((SQFParseException)e);
				//getErrors().add(new SQFParseException(e));
			} else if (e instanceof ParseException) {
				getErrors().add(new SQFParseException((ParseException)e));
			} else if (e instanceof TokenMgrError) {
				getErrors().add(new SQFParseException((TokenMgrError)e));
			}
		} finally {
			if (block != null) {
				block.analyze(this, null);
			}
			
			postParse();
			options.getOutputFormatter().print(this);
		}
		
		// Always return OK if exit code is disabled
		if (!options.isExitCodeEnabled()) {
			return CODE_OK;
		}
		
		// Return ERR code when any error was encountered
		return (getErrors().size() > 0) ? CODE_ERR : CODE_OK;
	}
		
	/**
	 * Post parse checks, mainly for warnings.
	 * Currently checks if every used local variable is actually defined.
	 */
	protected void postParse() {
		if (options.isSkipWarnings()) return;
		
		variables.entrySet().stream().forEach((entry) -> {
			SQFVariable var = entry.getValue();
			if (var.isLocal()
					&& !options.getSkippedVariables().contains(var.name.toLowerCase())
					&& !preprocessor.getMacros().containsKey(var.name.toLowerCase())) {
				if (var.definitions.isEmpty()) {
					var.usage.stream().forEach((u) -> {
						addUndefinedMessage(u);
					});
				} else {
					// This makes sure the definitions are properly sorted
					var.definitions.sort((Token a, Token b) -> {
						if (a.beginLine == b.beginLine) {
							return a.beginColumn - b.beginColumn;
						}
						return a.beginLine - b.beginLine;
					});
					
					Token first = var.definitions.get(0);
					var.usage.stream().forEach((u) -> {
						if (u == first) return;
						
						if (u.beginLine < first.beginLine ||
								(u.beginLine == first.beginLine &&
									u.beginColumn < first.beginColumn)) {
							addUndefinedMessage(u);
						}
					});
				}
			}
		});
		
		getWarnings().addAll(preprocessor.getWarnings());
	}
	
	/**
	 * Adds undefined message for specified token.
	 * 
	 * @param token token of undefined variable
	 */
	protected void addUndefinedMessage(Token token) {
		if (options.isWarningAsError()) {
			getErrors().add(new SQFParseException(token, "Possibly undefined variable " + token));
		} else {
			getWarnings().add(new Warning(token, "Possibly undefined variable " + token));
		}
	}
	
	/**
	 * Loads variable assigned to specified ident.
	 * If variable isn't registered yet, it will be.
	 * 
	 * @param ident
	 * @param name
	 * @return
	 */
	public SQFVariable getVariable(String ident, String name) {
		SQFVariable var;
		if (!variables.containsKey(ident)) {
			var = new SQFVariable(name);
			variables.put(ident, var);
		} else {
			var = variables.get(ident);
		}
		
		return var;
	}
	
	@Override
	protected void handleName() throws ParseException {
		// Load current token
		Token name = getToken(1);
		
		// Convert to ident (SQF is case insensitivie)
		String ident = name.toString().toLowerCase();
		
		// Otherwise, if not macro, command or ignored variable, handle variable
		if (!options.getOperators().containsKey(ident)
			&& !preprocessor.getMacros().containsKey(ident)
			&& !options.getIgnoredVariables().contains(ident)
		) {
			SQFVariable var = getVariable(ident, name.toString());

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
	 * @param skip 
	 * @return recovery point (EOF or recoveryPoint)
	 * @throws cz.zipek.sqflint.parser.ParseException 
	 */
	@Override
	protected int recover(ParseException ex, int recoveryPoint, boolean skip) throws ParseException {
		// Add to list of encountered errors
		if (!(ex instanceof SQFParseException)) {
			getErrors().add(new SQFParseException(ex));
		} else {
			getErrors().add((SQFParseException)ex);
		}
		
		// Don't actually recover if needed
		if (options.isStopOnError()) {
			throw ex;
		}
	
		// Skip token with error
		getNextToken();
		
		// Scan until we reach recovery point or EOF
		// We need to start AT recovery point, so only peek, don't consume
		// Only consume when it isn't recovery point
		Token t;
		while(true) {
			t = getToken(1);
			if (t.kind == recoveryPoint || t.kind == EOF) {
				if (skip) getNextToken();
				break;
			}
			getNextToken();
		}
		
		return t.kind;
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
	 * @return the warnings
	 */
	public List<Warning> getWarnings() {
		return warnings;
	}

	/**
	 * @return the includes
	 */
	public List<SQFInclude> getIncludes() {
		return includes;
	}

	/**
	 * @return the macros
	 */
	public List<SQFMacro> getMacros() {
		return macros;
	}

	/**
	 * @param preprocessor 
	 */
	public void setPreprocessor(SQFPreprocessor preprocessor) {
		this.preprocessor = preprocessor;
	}

	/**
	 * @return the preprocessor
	 */
	public SQFPreprocessor getPreprocessor() {
		return preprocessor;
	}

	/**
	 * @return the options
	 */
	public Options getOptions() {
		return options;
	}
}
