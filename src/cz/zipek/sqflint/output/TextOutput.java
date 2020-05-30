package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SqfFile;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class TextOutput implements OutputFormatter {
	
	@Override
	public void print(SqfFile sqfFile) {		
		if (sqfFile.getLinter().getOptions().isOutputVariables()) {
			System.err.println("You can't output variables info in text mode.");
		}
		
		// Print errors
		sqfFile.getLinter().getErrors().stream().forEach((e) -> {
			System.err.println(e.getMessage());
		});
		
		// Print warnings
		sqfFile.getLinter().getWarnings().stream().forEach((e) -> {
			System.err.println(e.toString());
		});
	}
	
}
