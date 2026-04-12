package com.opentext.lre.actions.common.helpers.utils;

import junit.framework.TestCase;

public class ResultTest extends TestCase {

    public void testCombineReturnsWorseResult() {
        assertEquals(Result.FAILURE, Result.SUCCESS.combine(Result.FAILURE));
        assertEquals(Result.ABORTED, Result.NOT_BUILT.combine(Result.ABORTED));
        assertEquals(Result.UNSTABLE, Result.UNSTABLE.combine(Result.SUCCESS));
    }

    public void testCombineWithNullKeepsCurrentResult() {
        assertEquals(Result.SUCCESS, Result.SUCCESS.combine(null));
    }

    public void testOrderingHelpers() {
        assertTrue(Result.SUCCESS.isBetterThan(Result.FAILURE));
        assertTrue(Result.FAILURE.isWorseThan(Result.UNSTABLE));
        assertTrue(Result.UNSTABLE.isBetterOrEqualTo(Result.UNSTABLE));
        assertTrue(Result.ABORTED.isWorseOrEqualTo(Result.NOT_BUILT));
    }

    public void testIsCompleteBuild() {
        assertTrue(Result.SUCCESS.isCompleteBuild());
        assertTrue(Result.UNSTABLE.isCompleteBuild());
        assertTrue(Result.FAILURE.isCompleteBuild());
        assertFalse(Result.NOT_BUILT.isCompleteBuild());
        assertFalse(Result.ABORTED.isCompleteBuild());
    }
}

