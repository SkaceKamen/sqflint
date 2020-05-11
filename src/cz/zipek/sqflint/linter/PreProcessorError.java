package cz.zipek.sqflint.linter;

import java.io.Serializable;

import cz.zipek.sqflint.parser.Token;

/**
 *
 * @author Senfo
 */
public class PreProcessorError implements Serializable {
	
	/**
	 * serial
	 */
	private static final long serialVersionUID = 1L;
	private final int line;
	private final String message;
	private final String filename;
	
	public PreProcessorError(String filename, int line, String message) {
		this.filename = filename;
		this.line = line;
		this.message = message;
	}
	
	@Override
	public String toString() {
		StringBuilder out = new StringBuilder()
			.append("Error: ")
			.append(getMessage());
		
		if (filename != null) {
			out.append(" at ");
			out.append(filename);
		}
		
		out.append(" at line ")
			.append(line);
	
		return out.toString();
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @return the line
	 */
	public int getLine() {
		return line;
	}
}
