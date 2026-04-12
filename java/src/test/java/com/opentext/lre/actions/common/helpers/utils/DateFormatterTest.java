package com.opentext.lre.actions.common.helpers.utils;

import junit.framework.TestCase;

public class DateFormatterTest extends TestCase {

    public void testEmptyPatternFallsBackToDefault() {
        DateFormatter formatter = new DateFormatter("");

        assertEquals(DateFormatter.DEFAULT_PATTERN, formatter.getPattern());
        assertNotNull(formatter.getDate());
        assertFalse(formatter.getDate().isEmpty());
    }

    public void testSetPatternUpdatesFormatter() {
        DateFormatter formatter = new DateFormatter("yyyy");
        formatter.setPattern("yyyy-MM-dd");

        assertEquals("yyyy-MM-dd", formatter.getPattern());

        String date = formatter.getDate();
        assertNotNull(date);
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    public void testSetEmptyPatternResetsToDefault() {
        DateFormatter formatter = new DateFormatter("yyyy-MM-dd");
        formatter.setPattern("");

        assertEquals(DateFormatter.DEFAULT_PATTERN, formatter.getPattern());
    }
}

