package cz.zipek.sqflint.output;

import java.util.Date;

import cz.zipek.sqflint.linter.Options;

public class LogUtil {
    public static void benchLog(
        Options options,
        Object module,
        String source,
        String message
    ) {
        
        if (options == null || !options.isBenchLogs()) return;

        String[] parts = module.getClass().getCanonicalName().split("\\.");
        String className = parts[parts.length - 1];
        
        String[] sourceParts = source.split("\\\\");

        System.err.println(
            Long.toString(new Date().getTime())
            + ": [" + className + "] -> " + sourceParts[sourceParts.length - 1] + ": "
            + message
        );
    }
}