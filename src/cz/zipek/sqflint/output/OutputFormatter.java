package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.SqfFile;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public interface OutputFormatter {
	public void print(SqfFile sqfFile);
}
