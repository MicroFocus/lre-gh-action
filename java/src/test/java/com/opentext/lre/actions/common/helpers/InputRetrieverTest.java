package com.opentext.lre.actions.common.helpers;

import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class InputRetrieverTest extends TestCase {

    public void testGetLreActionDefaultsToExecuteLreTestWhenMissing() throws Exception {
        Path config = writeConfig("{\"lre_server\":\"server\"}");

        InputRetriever inputRetriever = new InputRetriever(new String[]{config.toString()});

        assertEquals("ExecuteLreTest", inputRetriever.getLreAction());
    }

    public void testGetLreActionUsesConfiguredValue() throws Exception {
        Path config = writeConfig("{\"lre_action\":\"WorkspaceSync\"}");

        InputRetriever inputRetriever = new InputRetriever(new String[]{config.toString()});

        assertEquals("WorkspaceSync", inputRetriever.getLreAction());
    }

    public void testParseIntStrictlyPositive() {
        assertEquals(Integer.valueOf(10), InputRetriever.ParseIntStrictlyPositive("10"));
        assertNull(InputRetriever.ParseIntStrictlyPositive("0"));
        assertNull(InputRetriever.ParseIntStrictlyPositive("-5"));
        assertNull(InputRetriever.ParseIntStrictlyPositive("abc"));
        assertNull(InputRetriever.ParseIntStrictlyPositive(""));
        assertNull(InputRetriever.ParseIntStrictlyPositive(null));
    }

    private Path writeConfig(String json) throws Exception {
        Path file = Files.createTempFile("lre-input-retriever", ".json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }
}

