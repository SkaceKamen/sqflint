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
 * @author Jan Zípek (jan at zipek.cz)
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
	 * Helper method that creates sqf file with correct parameters and input.
	 * @param input
	 * @return initialized sqf file
	 * @throws Exception 
	 */
	private SqfFile parse(String input) throws Exception {
		
		Options options = new Options();
		// No stdout output should be created
		options.setOutputFormatter(new VoidOutput());
		// Some empty data
		options.setRootPath("./");
		
		SqfFile sqfFile = new SqfFile(
			options,
			input,
			"test.sqf"
		);

		return sqfFile;
	}

	/**
	 * Tests switch statement.
	 * @throws Exception 
	 */
	@Test
	public void testSwitch() throws Exception {
		SqfFile sqfFile = parse(
			"switch (A) do {\n" +
			"  case B: { X };\n" +
			"  case C: { Y };\n" +
			"};\n" +
			"_somethig = true;"
		);
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Shouldn't return any errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	/**
	 * Tests if definitions order matters.
	 * @throws Exception 
	 */
	@Test
	public void testDefinitionsOrder() throws Exception {
		SqfFile sqfFile = parse(
			"diag_log _defined;\n" +
			"_defined = true;"
		);
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Shouldn't return any errors", sqfFile.getLinter().getErrors().isEmpty());
		assertFalse("Should yield warning", sqfFile.getLinter().getWarnings().isEmpty());
	}
	
	/**
	 * Tests if definitions order matters.
	 * @throws Exception 
	 */
	@Test
	public void testDefinitionsOrderInParams() throws Exception {
		SqfFile sqfFile = parse(
			"params [\"_side\", \"_group\", \"_className\"];\n" +
			"diag_log _side;\n" +
			"_side = 1;"
		);
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Shouldn't return any errors", sqfFile.getLinter().getErrors().isEmpty());
		assertTrue("Shouldn't yield warning", sqfFile.getLinter().getWarnings().isEmpty());
	}
	
	/**
	 * Tests if result assigment.
	 * @throws Exception 
	 */
	@Test
	public void testIfAssigment() throws Exception {
		SqfFile sqfFile = parse(
			"_foo = (if (true) then { false; } else { true; });"
		);
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Shouldn't return any errors", sqfFile.getLinter().getErrors().isEmpty());
		assertTrue("Shouldn't return any warnings", sqfFile.getLinter().getWarnings().isEmpty());
	}
	
	/**
	 * Test if undefined warning works and throws correct position.
	 * @throws Exception 
	 */
	@Test
	public void testUndefinedWarning() throws Exception {
		SqfFile sqfFile = parse("\t_foo = _kamen;");
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warning", 1, sqfFile.getLinter().getWarnings().size());
		
		Warning warning = sqfFile.getLinter().getWarnings().get(0);
		
		assertEquals("Should report correct position", 9, warning.getToken().beginColumn);
		assertEquals("Should report correct position", 14, warning.getToken().endColumn);
	}
	
	@Test
	public void testEmptyFile() throws Exception {
		SqfFile sqfFile = parse("/* NOTHING \nNOTHING */");
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testEmptyBlock() throws Exception {
		SqfFile sqfFile = parse(";");
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testEmptyBlockContents() throws Exception {
		SqfFile sqfFile = parse("_test = { ; }");
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testNewSelectOperator() throws Exception {
		SqfFile sqfFile = parse(
			"(disconnectCache # 2) params [\"_uid\", \"_group\"];\n" +
			"	\n" +
			"[player] joinSilent _group;"
		);
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextSeparationForLocalFunctions() throws Exception {
		String[] lines = {
			"_func = { _i = 10; };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warnings", 1, sqfFile.getLinter().getWarnings().size());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextNotLeakingIntoFunctions() throws Exception {
		String[] lines = {
			"_i = 10;",
			"_fnc = { diag_log _i; }",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warnings", 1, sqfFile.getLinter().getWarnings().size());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextSeparationForSpawning() throws Exception {
		String[] lines = {
			"0 spawn { _i = 10; };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warnings", 1, sqfFile.getLinter().getWarnings().size());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextSeparationForBlocksAsArguments() throws Exception {
		String[] lines = {
			"player addEventHandler [\"Killed\", { _i = 10; }];",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warnings", 1, sqfFile.getLinter().getWarnings().size());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testPrivateContextSeparation() throws Exception {
		String[] lines = {
			"if (true) then { private _i = 10; };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertEquals("Should throw warnings", 1, sqfFile.getLinter().getWarnings().size());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}

	@Test
	public void testContextSeparationLeaking() throws Exception {
		String[] lines = {
			"if (true) then { _i = 10; };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextSeparationUpPropagation() throws Exception {
		String[] lines = {
			"private _i = 10;",
			"if (true) then { diag_log _i; };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
	
	@Test
	public void testContextSeparationForInlineFunctions() throws Exception {
		String[] lines = {
			"private _i = 1;",
			"{ damage _x == _i } count allUnits;",
			"allUnits findIf { damage _x == _i };",
			"allUnits apply { _i };",
			"allUnits select { damage _x == _i };",
			"diag_log _i;"
		};
		SqfFile sqfFile = parse(String.join("\n", lines));
		
		assertEquals(Linter.CODE_OK, sqfFile.process());
		assertTrue("Should not throw warnings", sqfFile.getLinter().getWarnings().isEmpty());
		assertTrue("Should not throw errors", sqfFile.getLinter().getErrors().isEmpty());
	}
}
