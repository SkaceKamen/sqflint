/*
 * The MIT License
 *
 * Copyright 2017 Jan Zípek (jan at zipek.cz).
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
package cz.zipek.sqflint;

import cz.zipek.sqflint.linter.Options;
import cz.zipek.sqflint.linter.SqfFile;
import cz.zipek.sqflint.output.LogUtil;
import cz.zipek.sqflint.output.ServerOutput;
import cz.zipek.sqflint.output.StreamUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Language server allowing to feed single process with multiple files.
 * @author Jan Zípek (jan at zipek.cz)
 */
public class SQFLintServer {
	private final Options options;
	
	public SQFLintServer(Options options) {
		this.options = options;
	}
	
	public void start() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				try {
					if (!processMessage(new JSONObject(br.readLine()))) {
						break;
					}
				} catch (JSONException ex) {
					Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
					System.err.println("Error parsing client message");
				}
			}
		}  catch (IOException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private boolean processMessage(JSONObject message) {
		LogUtil.benchLog(options, this, "/ClientMessage", "Client message received");
		
		String filePath = null; // declare here to use in catch block
		try {
			if (message.has("type") && "exit".equals(message.getString("type"))) {
				return false;
			}

			// read filepath
			filePath = message.getString("file");
			
			LogUtil.benchLog(options, this, filePath, "Starting");
			
			// Apply file specific options
			Options fileOptions = new Options(options);
			fileOptions.setOutputFormatter(new ServerOutput(filePath));
			fileOptions.setRootPath(Paths.get(filePath).toAbsolutePath().getParent().toString());
			fileOptions.getSkippedVariables().clear();

			if (message.has("options")) {
				this.applyOptions(message.getJSONObject("options"), fileOptions);
			}

			SqfFile sqfFile = new SqfFile(
				fileOptions,
				message.has("contents") ?
					message.getString("contents")
					:
					StreamUtil.streamToString(new FileInputStream(filePath)),
				filePath
			);

			sqfFile.process();

			fileOptions.getOutputFormatter().print(sqfFile);

			LogUtil.benchLog(options, this, filePath, "Done");

		} catch (JSONException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, "Error when parsing {0}", filePath);
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return true;
	}
	
	private void applyOptions(JSONObject data, Options fileOptions) {
		try {
			// @TODO: Clear options?
			
			if (data.has("checkPaths")) {
				fileOptions.setCheckPaths(data.getBoolean("checkPaths"));
			}
			
			if (data.has("pathsRoot")) {
				fileOptions.setRootPath(data.getString("pathsRoot"));
			}
			
			if (data.has("ignoredVariables")) {
				JSONArray vars = data.getJSONArray("ignoredVariables");
				for (int i = 0; i < vars.length(); i++) {
					fileOptions.getSkippedVariables().add(vars.getString(i));
				}
			}
						
			if (data.has("includePrefixes")) {
				fileOptions.getIncludePaths().clear();
				JSONObject paths = data.getJSONObject("includePrefixes");
				for (String key : paths.keySet()) {
					fileOptions.getIncludePaths().put(key, paths.getString(key));
				}
			}
			
			if (data.has("contextSeparation")) {
				fileOptions.setContextSeparationEnabled(data.getBoolean("contextSeparation"));
			}
			
		} catch (JSONException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
}
