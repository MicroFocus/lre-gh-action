package com.opentext.lre.actions.common.helpers.utils;

/**
 * Represents the result status of a script upload operation.
 * Similar to Jenkins Result but standalone without external dependencies.
 */
public enum Result {
    /**
     * The operation completed successfully.
     */
    SUCCESS(0),

    /**
     * The operation completed but with warnings or non-critical issues.
     */
    UNSTABLE(1),

    /**
     * The operation failed.
     */
    FAILURE(2),

    /**
     * The operation was not executed.
     */
    NOT_BUILT(3),

    /**
     * The operation was aborted.
     */
    ABORTED(4);

    private final int ordinal;

    Result(int ordinal) {
        this.ordinal = ordinal;
    }

    /**
     * Combines this result with another result, returning the worse of the two.
     * The ordering is: SUCCESS &lt; UNSTABLE &lt; FAILURE &lt; NOT_BUILT &lt; ABORTED
     *
     * @param other the result to combine with
     * @return the worse of the two results
     */
    public Result combine(Result other) {
        if (other == null) {
            return this;
        }
        return this.ordinal > other.ordinal ? this : other;
    }

    /**
     * Checks if this result is better than or equal to the given result.
     *
     * @param other the result to compare with
     * @return true if this result is better or equal
     */
    public boolean isBetterOrEqualTo(Result other) {
        return this.ordinal <= other.ordinal;
    }

    /**
     * Checks if this result is worse than or equal to the given result.
     *
     * @param other the result to compare with
     * @return true if this result is worse or equal
     */
    public boolean isWorseOrEqualTo(Result other) {
        return this.ordinal >= other.ordinal;
    }

    /**
     * Checks if this result is better than the given result.
     *
     * @param other the result to compare with
     * @return true if this result is better
     */
    public boolean isBetterThan(Result other) {
        return this.ordinal < other.ordinal;
    }

    /**
     * Checks if this result is worse than the given result.
     *
     * @param other the result to compare with
     * @return true if this result is worse
     */
    public boolean isWorseThan(Result other) {
        return this.ordinal > other.ordinal;
    }

    /**
     * Checks if the operation completed (successfully or not).
     *
     * @return true if the result is SUCCESS, UNSTABLE, or FAILURE
     */
    public boolean isCompleteBuild() {
        return this == SUCCESS || this == UNSTABLE || this == FAILURE;
    }
}

