package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.output.LogUtil;
import cz.zipek.sqflint.parser.ParseException;
import cz.zipek.sqflint.parser.SQFParser;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.parser.TokenMgrError;
import cz.zipek.sqflint.preprocessor.SQFInclude;
import cz.zipek.sqflint.preprocessor.SQFMacro;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import cz.zipek.sqflint.sqf.SQFContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class Linter extends SQFParser {
	public static final int CODE_OK = 0;
	public static final int CODE_ERR = 1;
	
	private final List<SQFParseException> errors = new ArrayList<>();
	private final List<Warning> warnings = new ArrayList<>();
	
	private final List<SQFInclude> includes = new ArrayList<>();
	private final List<SQFMacro> macros = new ArrayList<>();
		
	private SQFPreprocessor preprocessor;
	private final Options options;
	
	private Date startTime;
	private String filePath;

	public Linter(
		Options options,
		SQFPreprocessor preprocessor,
		InputStream stream,
		String filePath
	) {
		super(stream);
		
		this.options = options;
		this.preprocessor = preprocessor;
		this.filePath = filePath;
	}
	
	public int start() throws IOException {
		setTabSize(1);
		
		SQFBlock block = null;

		startTime = new Date();
		LogUtil.benchLog(options, this, filePath, "Linter (1) ");
		try {
			block = CompilationUnit();
		} catch (ParseException | TokenMgrError  e) {
			if (e instanceof SQFParseException) {
				getErrors().add((SQFParseException)e);
			} else if (e instanceof ParseException) {
				getErrors().add(new SQFParseException((ParseException)e));
			} else if (e instanceof TokenMgrError) {
				getErrors().add(new SQFParseException((TokenMgrError)e));
			}
		} finally {
			if (block != null) {
				LogUtil.benchLog(options, this, filePath, "Linter (2)");
				block.analyze(this, null);
				LogUtil.benchLog(options, this, filePath, "Linter (3)");
			}
		}
		LogUtil.benchLog(options, this, filePath, "Linter (4)");
		
		// Always return OK if exit code is disabled
		if (!options.isExitCodeEnabled()) {
			return CODE_OK;
		}
		
		// Return ERR code when any error was encountered
		return (getErrors().size() > 0) ? CODE_ERR : CODE_OK;
	}

	public Date getStartTime() {
		return startTime;
	}
		
	/**
	 * Post parse checks, mainly for warnings.
	 * Currently checks if every used local variable is actually defined.
	 */
	// protected void postParse() {
	// 	if (options.isSkipWarnings()) return;

	// 	getWarnings().addAll(preprocessor.getWarnings());
	// }
	
	@Override
	protected void pushContext(boolean newThread) {
		context = new SQFContext(this, context, newThread);
	}

	@Override
	protected Linter getLinter() {
		return this;
	}
	
	public SQFContext getContext() {
		return context;
	}
	
	/**
	 * Adds undefined message for specified token.
	 * 
	 * @param token token of undefined variable
	 * @return 
	 */
	public SQFParseException addUndefinedMessage(Token token) {
		SQFParseException ex;
		if (options.isWarningAsError()) {
			ex = new SQFParseException(token, "Possibly undefined variable " + token);
			getErrors().add(ex);
		} else {
			ex = new Warning(token, "Possibly undefined variable " + token);
			getWarnings().add((Warning)ex);
		}
		return ex;
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
		Map<String, SQFVariable> variables = new HashMap<>();
		
		if (context != null) {
			addContextVariables(variables, context);
		} else {
			System.err.println("NO CONTEXT!");
		}
		
		return variables;
	}
	
	private void addContextVariables(Map<String, SQFVariable> container, SQFContext context) {
		mergeVariables(container, context.getVariables());
		context.getChildren().forEach(child -> { addContextVariables(container, child); });
	}
	
	private void mergeVariables(Map<String, SQFVariable> container, Map<String, SQFVariable> newVariables) {
		newVariables.keySet().forEach(key -> {
			SQFVariable added = newVariables.get(key);
			if (container.containsKey(key)) {
				SQFVariable var = container.get(key);
				var.usage.addAll(added.usage);
				var.definitions.addAll(added.definitions);
				var.comments.addAll(added.comments);
			} else {
				container.put(key, added.copy());
			}
		});
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
