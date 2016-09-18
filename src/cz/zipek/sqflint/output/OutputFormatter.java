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
public interface OutputFormatter {
	public void print(Linter linter);
}
