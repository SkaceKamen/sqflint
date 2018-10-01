package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.parser.Token;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFVariable {
	public String name;
	public boolean isPrivate;
	public List<Token> usage = new ArrayList<>();
	public List<Token> definitions = new ArrayList<>();
	public List<Token> comments = new ArrayList<>();
	
	public SQFVariable(String name) {
		this.name = name;
	}
	
	public boolean isLocal() {
		return name.startsWith("_");
	}
	
	public SQFVariable copy() {
		SQFVariable cloned = new SQFVariable(name);
		cloned.usage = new ArrayList<>();
		cloned.usage.addAll(usage);
		cloned.definitions = new ArrayList<>();
		cloned.definitions.addAll(definitions);
		cloned.comments = new ArrayList<>();
		cloned.comments.addAll(comments);
		return cloned;
	}
}
