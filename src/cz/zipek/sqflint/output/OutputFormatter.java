package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public interface OutputFormatter {
	public void print(Linter linter);
}
