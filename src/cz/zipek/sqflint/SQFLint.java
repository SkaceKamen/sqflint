package cz.zipek.sqflint;

import cz.zipek.sqflint.linter.Linter;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;
import java.io.FileNotFoundException;
import java.io.IOException;
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
			
			try {
				linter = new Linter(new java.io.FileInputStream(filename));
			} catch (FileNotFoundException ex) {
				System.out.println("SQF Parser Version 1.1:  File " + filename + " not found.");
			}
		}
		
		if (linter != null) {
			linter.setStopOnError(cmd.hasOption("e"));
			linter.setSkipWarnings(cmd.hasOption("nw"));
			linter.setJsonOutput(cmd.hasOption("j"));
			linter.setOutputVariables(cmd.hasOption("v"));
			
			try {
				linter.start();
			} catch (IOException ex) {
				Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
}
