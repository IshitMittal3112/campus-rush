package com.campusrush;

import com.campusrush.ui.MainApp;

/**
 * Main launcher entry point for Campus Rush.
 * Delegates to MainApp.main(args) to avoid JavaFX module launch issues.
 */
public class Main {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
