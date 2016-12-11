/*
 * The MIT License
 *
 * Copyright 2016 Jan Zípek <jan at zipek.cz>.
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
package cz.zipek.sqflint.sqf.operators;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFParseException;
import cz.zipek.sqflint.sqf.SQFArray;
import cz.zipek.sqflint.sqf.SQFBlock;
import cz.zipek.sqflint.sqf.SQFExpression;
import cz.zipek.sqflint.sqf.SQFUnit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class GenericOperator extends Operator {
	public enum Type {
		ANY, ARRAY, CODE
	}
	
	private final String name;
	private final List<Type> left;
	private final List<Type> right;

	public GenericOperator(String name) {
		this.name = name;
		this.left = new ArrayList<>();
		this.right = new ArrayList<>();
	}
	
	public void addLeft(Type type) {
		left.add(type);
	}
	
	public void addRight(Type type) {
		right.add(type);
	}

	@Override
	public void analyze(Linter source, SQFBlock context, SQFExpression expression) {
		// Prepare arrays for both sides
		Side[] sides = new Side[] {
			new Side(expression.getLeft(), "left", left),
			new Side(expression.getRight(), "right", right)
		};
		
		// Analyze both sides in loop
		for(Side item : sides) {
			SQFExpression side = item.unit;
			String sideName = item.name;
			List<Type> values = item.values;
			
			if (values == null || values.isEmpty()) {
				if (side != null && !side.isOperator() && !sideName.equals("left")) {
					System.out.println("Side: '" + side.getToken().toString() + "'");
					
					source.getErrors().add(new SQFParseException(
						expression.getToken(),
						String.format("%s doesn't expect %s argument.", name, sideName)
					));
				}
			} else {
				if (side == null || side.isOperator()) {
					source.getErrors().add(new SQFParseException(
						expression.getToken(),
						String.format("Expected %s argument for %s", sideName, name)
					));
				} else if (!side.isVariable(source)) {
					boolean match = false;
					SQFUnit main = side.getMain();

					// @TODO: More type checking
					for(Type type : values) {
						if (type == Type.ANY ||
							(type == Type.CODE && main instanceof SQFBlock) ||
							(type == Type.ARRAY && main instanceof SQFArray)
						) {
							match = true;
							break;
						}
					}

					if (!match) {
						source.getErrors().add(new SQFParseException(
							expression.getToken(), 
							String.format(
								"Expected %s argument of %s to be one of: %s",
								sideName,
								name,
								String.join(", ", typesToStrings(values.toArray(new Type[0])))
							)
						));
					}
				}
			}
		}
	}
	
	/**
	 * Converts array of types to array of strings.
	 * This is used to print list of types to user.
	 * 
	 * @param types
	 * @return types converted to strings
	 */
	private String[] typesToStrings(Type[] types) {
		String[] res = new String[types.length];
		for(int i = 0; i < types.length; i++) {
			res[i] = types[i].name();
		}
		return res;
	}
	
	/**
	 * Represents one side of expression. Used to simplify analytics of
	 * both sides.
	 */
	private class Side {
		public final SQFExpression unit;
		public final String name;
		public final List<Type> values;

		public Side(SQFExpression unit, String name, List<Type> values) {
			this.unit = unit;
			this.name = name;
			this.values = values;
		}
	}
}
