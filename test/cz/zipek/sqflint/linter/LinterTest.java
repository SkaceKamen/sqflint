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
package cz.zipek.sqflint.linter;

import cz.zipek.sqflint.output.VoidOutput;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class LinterTest {
	
	public LinterTest() {
	}
	
	@BeforeClass
	public static void setUpClass() {
	}
	
	@AfterClass
	public static void tearDownClass() {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}
	
	/**
	 * Helper method that creates linter with correct parameters and input.
	 * @param input
	 * @return initialized linter
	 * @throws Exception 
	 */
	private Linter parse(String input) throws Exception {
		// Preprocessor may be required
		SQFPreprocessor preprocessor = new SQFPreprocessor(new Options());
		// Create linter from preprocessed input
		Linter linter = new Linter(stringToStream(preprocessor.process(
			input,
			"file",
			true
		)), new Options());
		
		// Assign preprocessor for futher usage
		linter.setPreprocessor(preprocessor);
		// No stdout output should be created
		linter.getOptions().setOutputFormatter(new VoidOutput());
		// Some empty data
		linter.getOptions().setRootPath("./");
		
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

	/**
	 * Tests switch statement.
	 * @throws Exception 
	 */
	@Test
	public void testSwitch() throws Exception {
		Linter linter = parse(
			"switch (A) do {\n" +
			"  case B: { X };\n" +
			"  case C: { Y };\n" +
			"};\n" +
			"_somethig = true;"
		);
		assertEquals(Linter.CODE_OK, linter.start());
		assertTrue("Shouldn't return any errors", linter.getErrors().isEmpty());
	}
	
	/**
	 * Tests if definitions order matters.
	 * @throws Exception 
	 */
	@Test
	public void testDefinitionsOrder() throws Exception {
		Linter linter = parse(
			"diag_log _defined;\n" +
			"_defined = true;"
		);
		assertEquals(Linter.CODE_OK, linter.start());
		assertTrue("Shouldn't return any errors", linter.getErrors().isEmpty());
		assertFalse("Should yield warning", linter.getWarnings().isEmpty());
	}
	
	/**
	 * Tests if definitions order matters.
	 * @throws Exception 
	 */
	@Test
	public void testDefinitionsOrderInParams() throws Exception {
		Linter linter = parse(
			"params [\"_side\", \"_group\", \"_className\"];\n" +
			"diag_log _side;\n" +
			"_side = 1;"
		);
		assertEquals(Linter.CODE_OK, linter.start());
		assertTrue("Shouldn't return any errors", linter.getErrors().isEmpty());
		assertTrue("Shouldn't yield warning", linter.getWarnings().isEmpty());
	}
	
	/**
	 * Tests if result assigment.
	 * @throws Exception 
	 */
	@Test
	public void testIfAssigment() throws Exception {
		Linter linter = parse(
			"_foo = (if (true) then { false; } else { true; });"
		);
		
		assertEquals(Linter.CODE_OK, linter.start());
		assertTrue("Shouldn't return any errors", linter.getErrors().isEmpty());
		assertTrue("Shouldn't return any warnings", linter.getWarnings().isEmpty());
	}
	
	/**
	 * Test if undefined warning works and throws correct position.
	 * @throws Exception 
	 */
	@Test
	public void testUndefinedWarning() throws Exception {
		Linter linter = parse("\t_foo = _kamen;");
		
		assertEquals(Linter.CODE_OK, linter.start());
		assertEquals("Should throw warning", 1, linter.getWarnings().size());
		
		Warning warning = linter.getWarnings().get(0);
		
		assertEquals("Should report correct position", 9, warning.getToken().beginColumn);
		assertEquals("Should report correct position", 14, warning.getToken().endColumn);
	}
	
	@Test
	public void testEmptyFile() throws Exception {
		Linter linter = parse("/* NOTHING \nNOTHING */");
		
		assertEquals(Linter.CODE_OK, linter.start());
		assertTrue("Should not throw warnings", linter.getWarnings().isEmpty());
		assertTrue("Should not throw errors", linter.getErrors().isEmpty());
	}
}
