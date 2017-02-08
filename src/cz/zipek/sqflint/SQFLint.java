package cz.zipek.sqflint;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 *
 * @author Jan Zípek <jan at zipek.cz>
 */
public class SQFLint {	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Options options = new Options();
		CommandLineParser cmdParser = new DefaultParser();
		CommandLine cmd;

		options.addOption("j", "json", false, "output json");
		options.addOption("v", "variables", false, "output variables info (only in json mode)");
		options.addOption("e", "error", false, "stop on error");
		options.addOption("nw", "no-warning", false, "skip warnings");
		options.addOption("we", "warning-as-error", false, "output warnings as errors");
		options.addOption("oc", "output-code", false, "output ERR return code when any error is encountered");
		options.addOption("cp", "check-paths", false, "check for path existence for exevm and preprocessfile");
		options.addOption("r", "root", true, "root for path checking (path to file is used if file is specified)");
		options.addOption("h", "help", false, "");
		
		try {
			cmd = cmdParser.parse(options, args);
		} catch (org.apache.commons.cli.ParseException ex) {
			Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		
		if (cmd.hasOption("h") || cmd.getArgs().length > 1) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("sqflint [OPTIONS] [FILE]", "Scans SQF file for errors and potential problems.", options, "Spaghetti");
			return;
		}
		
		SQFPreprocessor preprocessor = new SQFPreprocessor();
		Linter linter;
		String contents = null;
		String root = null;

		if (cmd.hasOption("r")) {
			root = cmd.getOptionValue("r");
		}
		
		if (cmd.getArgs().length == 0) {
			try {
				String filename = null;
				if (root != null) {
					filename = Paths.get(root).resolve("file.sqf").toString();
				}
				
				contents = preprocessor.process(System.in, filename, false);
			} catch (Exception ex) {
				Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
				return;
			}
		} else if (cmd.getArgs().length == 1) {
			String filename = cmd.getArgs()[0];
			
			if (root == null) {
				root = Paths.get(filename).toAbsolutePath().getParent().toString();
			}
			
			try {
				contents = preprocessor.process(new java.io.FileInputStream(filename), filename, true);
			} catch (Exception ex) {
				System.out.println("SQF Parser Version 1.1:  File " + filename + " not found.");
				return;
			}
		}
		
		if (contents != null) {
			linter = new Linter(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
			linter.setRootPath(root);
			linter.setPreprocessor(preprocessor);
			
			linter.setStopOnError(cmd.hasOption("e"));
			linter.setSkipWarnings(cmd.hasOption("nw"));
			linter.setJsonOutput(cmd.hasOption("j"));
			linter.setOutputVariables(cmd.hasOption("v"));
			linter.setExitCodeEnabled(cmd.hasOption("oc"));
			linter.setWarningAsError(cmd.hasOption("we"));
			linter.setCheckPaths(cmd.hasOption("cp"));

			try {
				System.exit(linter.start());
			} catch (IOException ex) {
				Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
}
