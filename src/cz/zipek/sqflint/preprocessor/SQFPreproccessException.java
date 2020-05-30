package cz.zipek.sqflint.preprocessor;

public class SQFPreproccessException extends Exception {

    /**
     * Serial
     */
    private static final long serialVersionUID = 8731489076129673925L;
    
    private final String file;

    private final int line;

    private final String additionalMessage;

    public SQFPreproccessException(
        String file,
        int line,
        String message
    ) {
        super(
            "[SQFPreprocessor Exception] in " + file + " at " + line + ": " + message
        );
        this.file = file;
        this.line = line;
        this.additionalMessage = message;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getAdditionalMessage() {
        return additionalMessage;
    }
}