package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.parser.ParseException;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.parser.TokenMgrError;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class SQFParseException extends ParseException {

	protected String jsonMessage;
	private String originFilename;

	public SQFParseException(Token token, String message) {
		super(message);
		currentToken = token;
		jsonMessage = message;
		
		if (token == null) {
			throw new IllegalArgumentException("Token can't be null.");
		}
	}
	
	public SQFParseException(String filename, Token token, String message) {
		super(message);
		originFilename = filename;
		currentToken = token;
		jsonMessage = message;
		
		if (token == null) {
			throw new IllegalArgumentException("Token can't be null.");
		}
	}
	
	public SQFParseException(ParseException ex) {
		super(buildMessage(ex, true));
		
		currentToken = ex.currentToken;
		jsonMessage = buildMessage(ex, false);
	}
	
	public SQFParseException(TokenMgrError ex) {
		super(ex.getMessage());
		
		currentToken = null;
		jsonMessage = ex.getMessage();
	}
	
	public String getJSONMessage() {
		return jsonMessage;
	}

	static String buildMessage(ParseException ex, boolean position) {
		if (ex.expectedTokenSequences == null) {
			return ex.getMessage();
		}
			
		Token currentToken = ex.currentToken;
		int[][] expectedTokenSequences = ex.expectedTokenSequences;
		String[] tokenImage = ex.tokenImage;
		
		String line = System.getProperty("line.separator", "\n");
		StringBuilder expected = new StringBuilder();
		int maxSize = 0;
		for (int[] expectedTokenSequence : expectedTokenSequences) {
			if (maxSize < expectedTokenSequence.length) {
				maxSize = expectedTokenSequence.length;
			}
			for (int j = 0; j < expectedTokenSequence.length; j++) {
				expected.append(tokenImage[expectedTokenSequence[j]]).append(' ');
			}
			if (expectedTokenSequence[expectedTokenSequence.length - 1] != 0) {
				expected.append("...");
			}
			expected.append(line).append("    ");
		}
		String retval = "Encountered \"";
		Token tok = currentToken.next;
		/*
		for (int i = 0; i < maxSize; i++) {
			if (i != 0) {
				retval += " ";
			}
			if (tok.kind == 0) {
				retval += tokenImage[0];
				break;
			}
			retval += " " + tokenImage[tok.kind];
			retval += " \"";
			retval += add_escapes(tok.image);
			retval += " \"";
			tok = tok.next;
		}
		*/
		retval += add_escapes(tok.toString());
		retval += "\"";
		if (position) {
			retval += " at line " + currentToken.next.beginLine + ", column " + currentToken.next.beginColumn;
		}
		retval += "." + line;
		if (expectedTokenSequences.length == 1) {
			retval += "Was expecting:" + line + "    ";
		} else {
			retval += "Was expecting one of:" + line + "    ";
		}
		retval += expected.toString();
		return retval;
	}
	
	static String add_escapes(String str) {
		StringBuilder retval = new StringBuilder();
		char ch;
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
				case 0:
					continue;
				case '\b':
					retval.append("\\b");
					continue;
				case '\t':
					retval.append("\\t");
					continue;
				case '\n':
					retval.append("\\n");
					continue;
				case '\f':
					retval.append("\\f");
					continue;
				case '\r':
					retval.append("\\r");
					continue;
				case '\"':
					retval.append("\\\"");
					continue;
				case '\'':
					retval.append("\\\'");
					continue;
				case '\\':
					retval.append("\\\\");
					continue;
				default:
					if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
						String s = "0000" + Integer.toString(ch, 16);
						retval.append("\\u" + s.substring(s.length() - 4, s.length()));
					} else {
						retval.append(ch);
					}
					continue;
			}
		}
		return retval.toString();
	}

	/**
	 * @return the originFilename
	 */
	public String getOriginFilename() {
		return originFilename;
	}
}
