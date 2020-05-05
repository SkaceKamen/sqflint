package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.parser.Token;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class Warning extends SQFParseException {
	private final Token token;
	private final String message;
	private final String filename;
	
	public Warning(Token token, String message) {
		super(token, message);
		
		this.filename = null;
		this.token = token;
		this.message = message;
	}
	
	public Warning(String filename, Token token, String message) {
		super(token, message);
		
		this.filename = filename;
		this.token = token;
		this.message = message;
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder()
			.append("Warning: ")
			.append(getMessage());
		
		if (filename != null) {
			out.append(" at ");
			out.append(filename);
		}
		
		out.append(" at line ")
			.append(getToken().beginLine)
			.append(" column ")
			.append(getToken().beginColumn);
	
		return out.toString();
	}

	/**
	 * @return the token
	 */
	public Token getToken() {
		return token;
	}

	/**
	 * @return the message
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
}
