/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
	public List<Token> usage = new ArrayList<>();
	public List<Token> definitions = new ArrayList<>();
	public List<Token> comments = new ArrayList<>();
	
	public SQFVariable(String name) {
		this.name = name;
	}
	
	public boolean isLocal() {
		return name.startsWith("_");
	}
}
