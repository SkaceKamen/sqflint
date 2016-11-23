package cz.zipek.sqflint;

import cz.zipek.sqflint.linter.Linter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
		
		Linter linter = null;

		if (cmd.getArgs().length == 0) {
			linter =  new Linter(System.in);
		} else if (cmd.getArgs().length == 1) {
			String filename = cmd.getArgs()[0];
			String root = Paths.get(filename).toAbsolutePath().getParent().toString();
			
			try {
				linter = new Linter(new java.io.FileInputStream(filename));
				linter.setRootPath(root);
			} catch (FileNotFoundException ex) {
				System.out.println("SQF Parser Version 1.1:  File " + filename + " not found.");
			}
		}
		
		if (linter != null) {
			linter.setStopOnError(cmd.hasOption("e"));
			linter.setSkipWarnings(cmd.hasOption("nw"));
			linter.setJsonOutput(cmd.hasOption("j"));
			linter.setOutputVariables(cmd.hasOption("v"));
			linter.setExitCodeEnabled(cmd.hasOption("oc"));
			linter.setWarningAsError(cmd.hasOption("we"));
			linter.setCheckPaths(cmd.hasOption("cp"));
			
			if (cmd.hasOption("r")) {
				linter.setRootPath(cmd.getOptionValue("r"));
			}
			
			try {
				System.exit(linter.start());
			} catch (IOException ex) {
				Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
}
