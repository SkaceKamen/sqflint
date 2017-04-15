package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.SQFVariable;
import cz.zipek.sqflint.parser.Token;
import cz.zipek.sqflint.preprocessor.SQFMacro;
import java.util.ArrayList;
import java.util.List;
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
	protected List<JSONObject> build(Linter linter) {
		List<JSONObject> result = new ArrayList<>();
		
		// Print errors
		linter.getErrors().stream().forEach((e) -> {
			try {
				Token pos = e.currentToken;
				if (e.currentToken.next != null) {
					pos = e.currentToken.next;
				}
				
				JSONObject error = getRange(pos);
				error.put("type", "error");
				error.put("message", e.getJSONMessage());

				result.add(error);
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
				
				result.add(error);
			} catch (JSONException ex) {
				Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
			}
		});
		
		if (linter.getOptions().isOutputVariables()) {
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

					result.add(var);
				} catch (JSONException ex) {
					Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
				}
			});
			
			// Print includes
			linter.getPreprocessor().getIncludes().stream().forEach((entry) -> {
				try {
					JSONObject info = new JSONObject();
					info.put("type", "include");
					info.put("include", entry.getFile());
					info.put("from", entry.getSource());
					
					result.add(info);
				} catch (JSONException ex) {
					Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
				}
			});
			
			// Print macros info
			linter.getPreprocessor().getMacros().entrySet().stream().forEach((entry) -> {
				try {
					SQFMacro macro = entry.getValue();
					
					JSONArray definitions = new JSONArray();
					macro.getDefinitions().stream().forEach((item) -> {
						try {
							JSONObject def = new JSONObject();
							def.put("range", getRange(item.getToken()));
							def.put("value", item.getValue());
							def.put("filename", item.getFilename());
							definitions.put(def);
						} catch (JSONException ex) {
							Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
						}
					});
					
					JSONObject info = new JSONObject();
					info.put("type", "macro");
					info.put("macro", macro.getName());
					info.put("definitions", definitions);
					
					result.add(info);
				} catch (JSONException ex) {
					Logger.getLogger(JSONOutput.class.getName()).log(Level.SEVERE, null, ex);
				}
			});
		}
		
		return result;
	}
	
	@Override
	public void print(Linter linter) {		
		build(linter).stream().forEach((item) -> {
			System.out.println(item.toString());
		});
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
