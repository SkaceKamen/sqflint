package cz.zipek.sqflint;

import cz.zipek.sqflint.linter.SqfFile;
import cz.zipek.sqflint.output.JSONOutput;
import cz.zipek.sqflint.output.StreamUtil;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
 * @author Jan Zípek (jan at zipek.cz)
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
		options.addOption("iv", "ignore-variables", true, "ignored variables are treated as internal command");
		options.addOption("s", "server", false, "run as server");
		options.addOption("ip", "include-prefix", true, "adds include prefix override, format: prefix,path_to_use");
		options.addOption("ncs", "no-context-separation", true, "disable context separation");
		options.addOption("bl", "bench-logs", false, "output benchlog to stderr");
		
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
		
		String root = null;
		String[] ignoredVariables = new String[0];

		if (cmd.hasOption("r")) {
			root = cmd.getOptionValue("r");
		}
		
		if (cmd.hasOption("iv")) {
			ignoredVariables = cmd.getOptionValues("iv");
		}
		
		cz.zipek.sqflint.linter.Options linterOptions;
		try {
			linterOptions = new cz.zipek.sqflint.linter.Options();
		} catch (IOException ex) {
			Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		
		if (cmd.hasOption("ip")) {
			for (String value : cmd.getOptionValues("ip")) {
				String[] split = value.split(",");
				if (split.length != 2) {
					System.out.println("Invalid include prefix : " + value);
					System.out.println("Include prefix format is: prefix,include_path");
					return;
				}
				
				linterOptions.getIncludePaths().put(split[0], split[1]);
			}
		}

		linterOptions.setBenchLogs(cmd.hasOption("bl"));

		linterOptions.setRootPath(root);
		linterOptions.addIgnoredVariables(ignoredVariables);

		if (cmd.hasOption("j")) {
			linterOptions.setOutputFormatter(new JSONOutput());
		}

		linterOptions.setStopOnError(cmd.hasOption("e"));
		linterOptions.setSkipWarnings(cmd.hasOption("nw"));
		linterOptions.setOutputVariables(cmd.hasOption("v"));
		linterOptions.setExitCodeEnabled(cmd.hasOption("oc"));
		linterOptions.setWarningAsError(cmd.hasOption("we"));
		linterOptions.setCheckPaths(cmd.hasOption("cp"));
		
		if (!cmd.hasOption("s")) {
			
			InputStream contents = null;
			String filename = null;
			if (cmd.getArgs().length == 0) {
				try {
					if (root != null) {
						filename = Paths.get(root).resolve("file.sqf").toString();
					}
					contents = System.in;
				} catch (Exception ex) {
					Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, null, ex);
					return;
				}
			} else if (cmd.getArgs().length == 1) {
				filename = cmd.getArgs()[0];
				if (root == null) {
					root = Paths.get(filename).toAbsolutePath().getParent().toString();
				}

				try {
					contents = new FileInputStream(filename);
				} catch (FileNotFoundException e) {
					Logger.getLogger(SQFLint.class.getName()).log(Level.SEVERE, filename + " not found", e);
					System.exit(1);
				}
			}
			
			linterOptions.setRootPath(root);

			SqfFile sqfFile = new SqfFile(
				linterOptions,
				StreamUtil.streamToString(contents),
				filename
			);

			System.exit(sqfFile.process());
		} else {
			SQFLintServer server = new SQFLintServer(linterOptions);
			server.start();
		}
	}
	
}
