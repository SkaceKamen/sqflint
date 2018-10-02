/*
 * The MIT License
 *
 * Copyright 2017 Jan Zípek <jan at zipek.cz>.
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

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.linter.Options;
import cz.zipek.sqflint.output.ServerOutput;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Language server allowing to feed single process with multiple files.
 * @author Jan Zípek <jan at zipek.cz>
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
				}
			}
		}  catch (IOException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private boolean processMessage(JSONObject message) {
		try {
			if (message.has("type") && "exit".equals(message.getString("type"))) {
				return false;
			}
			
			String filePath = message.getString("file");
			Linter linter;
			
			if (message.has("contents")) {
				linter = parse(message.getString("contents"), filePath);
			} else {
				linter = parseFile(filePath);
			}
			
			if (message.has("options")) {
				this.applyOptions(message.getJSONObject("options"));
			}
			
			linter.start();			
		} catch (JSONException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return true;
	}
	
	private void applyOptions(JSONObject data) {
		try {
			// @TODO: Clear options?
			
			if (data.has("checkPaths")) {
				options.setCheckPaths(data.getBoolean("checkPaths"));
			}
			
			if (data.has("pathsRoot")) {
				options.setRootPath(data.getString("pathsRoot"));
			}
			
			if (data.has("ignoredVariables")) {
				JSONArray vars = data.getJSONArray("ignoredVariables");
				for (int i = 0; i < vars.length(); i++) {
					options.getSkippedVariables().add(vars.getString(i));
				}
			}
						
			if (data.has("includePrefixes")) {
				options.getIncludePaths().clear();
				
				JSONObject paths = data.getJSONObject("includePrefixes");
				Iterator keys = paths.keys();
				while (keys.hasNext()) {
					String key = (String)keys.next();
					options.getIncludePaths().put(key, paths.getString(key));
				}
			}
			
			if (data.has("contextSeparation")) {
				options.setContextSeparationEnabled(data.getBoolean("contextSeparation"));
			}
			
		} catch (JSONException ex) {
			Logger.getLogger(SQFLintServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public Linter parseFile(String path) throws Exception {
		return parse(new BufferedReader(new InputStreamReader(new FileInputStream(path))).lines().collect(Collectors.joining("\n")), path);
	}
	
	public Linter parse(String fileContents, String filePath) throws Exception {
		// Apply file specific options
		options.setOutputFormatter(new ServerOutput(filePath));
		options.setRootPath(Paths.get(filePath).toAbsolutePath().getParent().toString());
		options.getSkippedVariables().clear();

		// Preprocessor may be required
		SQFPreprocessor preprocessor = new SQFPreprocessor(options);
		
		// Create linter from preprocessed input
		Linter linter = new Linter(stringToStream(preprocessor.process(
			fileContents,
			filePath,
			true
		)), options);
		linter.setPreprocessor(preprocessor);
		
		return linter;
	}
	
	/**
	 * Creates input stream (with UTF-8 encoding) from input string.
	 * @param input
	 * @return 
	 */
	private InputStream stringToStream(String input) {
		return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
	}
}
