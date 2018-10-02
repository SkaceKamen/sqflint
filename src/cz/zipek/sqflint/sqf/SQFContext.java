/*
 * The MIT License
 *
 * Copyright 2018 Jan Zípek <jan at zipek.cz>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cz.zipek.sqflint.sqf;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFParseException;
import cz.zipek.sqflint.linter.SQFVariable;
import cz.zipek.sqflint.parser.Token;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFContext {
	private final Linter linter;
	private final SQFContext previous;
	private boolean newThread;
	private final List<SQFContext> children = new ArrayList<>();
	
	private final Map<String, SQFVariable> variables = new HashMap<>();
	
	public SQFContext(Linter linter, SQFContext previous, boolean newThread) {
		this.linter = linter;
		this.previous = previous;
		this.newThread = newThread;
		if (previous != null) {
			previous.addChild(this);
		}
	}
	
	public void addChild(SQFContext child) {
		children.add(child);
	}
	
	/**
	 * Loads variable assigned to specified ident.
	 * If variable isn't registered yet, it will be.
	 * 
	 * @param ident
	 * @param name
	 * @param privateAssigment
	 * @return
	 */
	public SQFVariable getVariable(String ident, String name, boolean privateAssigment) {
		SQFVariable var;

		if (!variables.containsKey(ident)) {
			if (!privateAssigment &&
					previous != null &&
					(!newThread || !linter.getOptions().isContextSeparationEnabled())
			) {
				return previous.getVariable(ident, name, false);
			} else {
				var = new SQFVariable(name);
				variables.put(ident, var);
			}
		} else {
			var = variables.get(ident);
		}
		
		return var;
	}
	
	public SQFParseException handleName(Token name, boolean isAssigment, boolean isPrivate) {
		SQFVariable var = getVariable(
			name.toString().toLowerCase(),
			name.toString(),
			isAssigment && isPrivate
		);
		SQFParseException ex = null;
		
		if (var.isLocal() && !isAssigment && var.definitions.isEmpty()) {
			ex = linter.addUndefinedMessage(name);
		}
		
		var.usage.add(name);
		
		if (isAssigment) {
			var.isPrivate = var.isPrivate || isPrivate;
			var.definitions.add(name);

			if (name.specialToken != null) {
				var.comments.add(name.specialToken);
			} else {
				var.comments.add(null);
			}
		}
		
		return ex;
	}

	/**
	 * @return the previous
	 */
	public SQFContext getPrevious() {
		return previous;
	}

	public Map<String, SQFVariable> getVariables() {
		return variables;
	}

	public List<SQFContext> getChildren() {
		return children;
	}

	/**
	 * @return the newThread
	 */
	public boolean isNewThread() {
		return newThread;
	}

	/**
	 * @param newThread the newThread to set
	 */
	public void setNewThread(boolean newThread) {
		this.newThread = newThread;
	}
	
	public void clear() {
		variables.clear();
		children.forEach(c -> c.clear());
	}
}
