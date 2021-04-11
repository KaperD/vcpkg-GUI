package ru.hse.settings;

import java.util.prefs.Preferences;

public class Settings {
    private static String executablePath;
    private static final Preferences preferences = Preferences.userRoot().node("vcpkgSettings");

    static {
        executablePath = preferences.get("path", "");
    }

    public static String getExecutablePath() {
        return executablePath;
    }

    public static void setExecutablePath(String s) {
        executablePath = s;
        preferences.put("path", executablePath);
    }
}
