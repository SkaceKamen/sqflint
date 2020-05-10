/*
 * The MIT License
 *
 * Copyright 2018 Jan Zípek (jan at zipek.cz).
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
package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.linter.Options;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Jan Zípek (jan at zipek.cz)
 */
public class PreprocessorTest {
		/**
	 * Helper method that creates linter with correct parameters and input.
	 * @param input
	 * @return initialized linter
	 * @throws Exception 
	 */
	private String parse(String input) throws Exception {
		return new SQFPreprocessor(new Options()).process(input, "test.sqf", false);
	}
	
	@Test
	public void testMacroParsing() throws Exception {
		String result = parse(
			"#define TEST _test\n" +
			"#define QUOTE(a) #a\n" +
			"#define ERROR(a) diag_log text(format[\"ERROR(a) occured at %1\", #a])\n" +
			"\n" +
			"diag_log TEST;\n" +
			"systemChat QUOTE(Hello);\n" +
			"ERROR(Now);"
		);
		
		// @TODO: This is actually wrong, but fine for the moment
		Assert.assertEquals(
			"\n" +
			"\n" +
			"\n" +
			"\n" +
			"diag_log _test;\n" +
			"systemChat \"Hello\";\n" +
			"diag_log text(format[\"ERROR(Now) occured at %1\", \"Now\"]);",
			result
		);
	}
}
