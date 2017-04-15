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
package cz.zipek.sqflint.output;

import cz.zipek.sqflint.linter.Linter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONStringer;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class ServerOutput extends JSONOutput {
	private final String filename;
	
	public ServerOutput(String filename) {
		this.filename = filename;
	}
	
	@Override
	public void print(Linter linter) {
		try {
			System.out.println(
				new JSONStringer()
					.object()
						.key("file")
						.value(this.filename)
						.key("messages")
						.value(build(linter))
					.endObject()
			);
			System.out.flush();
		} catch (JSONException ex) {
			Logger.getLogger(ServerOutput.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
