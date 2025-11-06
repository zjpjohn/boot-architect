package com.cloud.arch.aggregate.reflection;

public class ExceptionUtilities {

    private ExceptionUtilities() {
        super();
    }

    /**
     * Safely Ignore a Throwable or rethrow if it is a Throwable that should not be ignored.
     *
     * @param t Throwable to possibly ignore (OutOfMemory are not ignored).
     */
    public static void safelyIgnoreException(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            throw (OutOfMemoryError)t;
        }
    }

    /**
     * @return Throwable representing the actual cause (most nested exception).
     */
    public static Throwable getDeepestException(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

}
