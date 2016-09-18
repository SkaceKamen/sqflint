/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class TextOutput implements OutputFormatter {
	
	@Override
	public void print(Linter linter) {		
		if (linter.isOutputVariables()) {
			System.err.println("You can't output variables info in text mode.");
		}
		
		// Print errors
		linter.getErrors().stream().forEach((e) -> {
			System.err.println(e.getMessage());
		});
		
		// Print warnings
		linter.getWarnings().stream().forEach((e) -> {
			System.err.println(e.toString());
		});
	}
	
}
