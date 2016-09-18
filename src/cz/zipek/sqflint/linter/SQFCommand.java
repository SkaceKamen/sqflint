package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.parser.ParseException;
import cz.zipek.sqflint.parser.SQFParser;
import cz.zipek.sqflint.parser.SQFParserConstants;
import cz.zipek.sqflint.parser.Token;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFCommand implements SQFParserConstants {
	public enum Type {
		Unary, Binary, Noargs
	};
	
	public Type type;
	public String name;
	
	public SQFCommand(String name, Type type) {
		this.name = name;
		this.type = type;
	}
	
	public void test(Token token, SQFParser parser) throws ParseException {
		// We will use next token for some validation
		Token next = parser.getToken(2);
		if (next == null) {
			return;
		}
		
		// Check if noargs is called with something
		if (type == Type.Noargs) {
			switch(next.kind) {
				case LBRACE:
				case LBRACKET:
				case DOT:
				case INTEGER_LITERAL:
				case DECIMAL_LITERAL:
				case STRING_LITERAL:
				case STRING_LITERAL_OTHER:
					throw new SQFParseException(token, "Unexpected " + next + ". This command has no arguments.");
			}
		}

		// Check that we have next argument
		if (type == Type.Unary || type == Type.Binary) {
			switch(next.kind) {
				case RPAREN:
				case RBRACE:
				case RBRACKET:
				case SEMICOLON:
					throw new SQFParseException(token, "Unexpected " + next + ". Was expecting second argument.");
			}
		}
	}
}