/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFVariable;
import cz.zipek.sqflint.parser.Token;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class JSONOutput implements OutputFormatter {

	@Override
	public void print(Linter linter) {		
		// Print errors
		linter.getErrors().stream().forEach((e) -> {
			try {
				JSONObject error = getRange(e.currentToken.next);
				error.put("type", "error");
				error.put("message", e.getJSONMessage());

				System.out.println(error.toString());
			} catch (JSONException ex) {
				Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
			}
		});
		
		// Print warnings
		linter.getWarnings().stream().forEach((e) -> {
			try {
				JSONObject error = getRange(e.getToken());
				error.put("type", "warning");
				error.put("message", e.getMessage());
				
				System.out.println(error.toString());
			} catch (JSONException ex) {
				Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
			}
		});
		
		if (linter.isOutputVariables()) {
			// Print variables info
			linter.getVariables().entrySet().stream().forEach((entry) -> {
				try {
					SQFVariable v = entry.getValue();
					String comment = null;
					if (v.comments.size() > 0 && v.comments.get(0) != null) {
						comment = v.comments.get(0).toString();
					}

					JSONArray definitions = new JSONArray();
					JSONArray usage = new JSONArray();

					// Build usage array containing positions of usages
					v.usage.stream().forEach((u) -> {
						try {
							usage.put(getRange(u));
						} catch (JSONException ex) {
							Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
						}
					});

					// Build definitions array containing positions of definitions
					v.definitions.stream().forEach((d) -> {
						try {
							definitions.put(getRange(d));
						} catch (JSONException ex) {
							Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
						}
					});

					// Build result message
					JSONObject var = new JSONObject();
					var.put("type", "variable");
					var.put("variable", v.name);
					var.put("usage", usage);
					var.put("definitions", definitions);
					var.put("comment", comment);

					System.out.println(var.toString());
				} catch (JSONException ex) {
					Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
				}
			});
		}
	}
	
	/**
	 * Builds json containing info about token position.
	 * 
	 * @param token
	 * @return info about token position
	 * @throws JSONException 
	 */
	private JSONObject getRange(Token token) throws JSONException {
		JSONObject range = new JSONObject();
		
		range.put("line", new JSONArray(new int[] {
			token.beginLine, token.endLine
		}));
		range.put("column", new JSONArray(new int[] {
			token.beginColumn, token.endColumn
		}));
		
		return range;
	}
	
}
