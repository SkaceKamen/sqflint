package cz.zipek.sqflint.preprocessor;

public class SQFPreproccessException extends Exception {

    /**
     * Serial
     */
    private static final long serialVersionUID = 8731489076129673925L;
    
    public SQFPreproccessException(String file, int line, String message) {
        super(
            "[SQFPreprocessor Exception] in " + file + " at " + line + ": " + message
        );
    }
}