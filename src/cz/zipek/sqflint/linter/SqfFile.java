package cz.zipek.sqflint.linter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.zipek.sqflint.output.LogUtil;
import cz.zipek.sqflint.output.StreamUtil;
import cz.zipek.sqflint.preprocessor.SQFPreproccessException;
import cz.zipek.sqflint.preprocessor.SQFPreprocessor;

public class SqfFile {
    private SQFPreprocessor preprocessor;
    private Linter linter;
    private String filePath;
    private String fileContent;
    private Options options;
    private PreProcessorError preProcessorError;

    public SqfFile(Options options, String fileContent, String filePath) {
        this.options = options;
        this.fileContent = fileContent;
        this.filePath = filePath;

        this.preprocessor = new SQFPreprocessor(options);
    }

    public int process() {

        try {
            LogUtil.benchLog(options, this, filePath, "Preproc");
            this.fileContent = preprocessor.process(
                this.fileContent,
                this.filePath,
                this.filePath != null
            );
            LogUtil.benchLog(options, this, filePath, "Preproc done");
        } catch (SQFPreproccessException ex) {
            System.err.println("Preprocessor Error" + ex.getMessage());
            preProcessorError = new PreProcessorError(
                ex.getFile(),
                ex.getLine(),
                ex.getMessage()
            );
            return 1;
        }

        if (this.fileContent == null) {
            return 1;
        }

        // Create linter from preprocessed input
        this.linter = new Linter(
            options,
            preprocessor,
            StreamUtil.stringToStream(this.fileContent),
            this.filePath
        );

        try {
            return linter.start();
        } catch (IOException ex) {
            Logger.getLogger(SqfFile.class.getName()).log(Level.SEVERE, null, ex);
            return 1;
        }
    }

    public SQFPreprocessor getPreprocessor() {
        return preprocessor;
    }

    public void setPreprocessor(SQFPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public Linter getLinter() {
        return linter;
    }

    public void setLinter(Linter linter) {
        this.linter = linter;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public PreProcessorError getPreProcessorError() {
        return preProcessorError;
    }

    public void setPreProcessorError(PreProcessorError preProcessorError) {
        this.preProcessorError = preProcessorError;
    }

    
}