package com.opentext.lre.actions.common.helpers;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationManager {
    private static final String BASE_NAME = "com.opentext.lre.actions.runtest.Messages";
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final ResourceBundle DEFAULT_BUNDLE = ResourceBundle.getBundle(BASE_NAME, DEFAULT_LOCALE);

    public static String getString(String key) {
        return DEFAULT_BUNDLE.getString(key);
    }

    public static String getString(String key, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);
        return bundle.getString(key);
    }
}