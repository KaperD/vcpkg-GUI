package ru.hse.vcpkg;

import ru.hse.gui.MainWindow;
import ru.hse.settings.Settings;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VcpkgController {
    public static List<VcPackage> getAllPackages() {
        String executablePath = Settings.getExecutablePath();
        List<VcPackage> list = new ArrayList<>();
        try {
            Process process = new ProcessBuilder().command(executablePath, "search").start();
            try (
                    Reader reader = new InputStreamReader(process.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line = bufferedReader.readLine();

                while (line != null && !line.isEmpty()) {
                    int spacePosition = line.indexOf(" ");
                    String name = line.substring(0, spacePosition).trim();
                    int descriptionStart = Math.min(line.length(), 20 + 16 + 2); // Found in vcpkg sources
                    String version = line.substring(spacePosition, descriptionStart).trim();
                    String description = line.substring(descriptionStart).trim();
                    list.add(new VcPackage(name, version, description));

                    line = bufferedReader.readLine();
                }

            }
            if (process.waitFor() != 0) {
                return List.of();
            }
        } catch (IOException | InterruptedException e) {
            return List.of();
        }
        return list;
    }

    public static List<VcPackage> getInstalledPackages() {
        String executablePath = Settings.getExecutablePath();
        List<VcPackage> list = new ArrayList<>();
        try {
            Process process = new ProcessBuilder().command(executablePath, "list", "--x-full-desc").start();
            try (
                    Reader reader = new InputStreamReader(process.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(reader)) {
                String a = bufferedReader.readLine();

                while (a != null && !a.isEmpty() && !a.startsWith("No packages")) {
                    int spacePosition = a.indexOf(" ");
                    String name = a.substring(0, spacePosition).trim();
                    int descriptionStart = Math.min(a.length(), 50 + 16 + 2); // Found in vcpkg sources
                    String version = a.substring(spacePosition, descriptionStart).trim();
                    String description = a.substring(descriptionStart).trim();
                    list.add(new VcPackage(name, version, description));

                    a = bufferedReader.readLine();
                }

            }
            if (process.waitFor() != 0) {
                return List.of();
            }
        } catch (IOException | InterruptedException e) {
            return List.of();
        }
        return list;
    }

    public static void installPackage(String name, JTextArea log, MainWindow window) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                String executablePath = Settings.getExecutablePath();
                try {
                    Process process = new ProcessBuilder().command(executablePath, "install", name).start();
                    try (
                            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                            BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String line = reader.readLine();
                        while (line != null) {
                            publish(line);
                            line = reader.readLine();
                        }
                    }
                    return process.waitFor() == 0;
                } catch (IOException | InterruptedException e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        window.updateListInstalled();
                        window.getStatus().setText("Successfully installed");
                        window.getStatus().setForeground(Color.GREEN);
                        return;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    window.getStatus().setText("Failed to install");
                    window.getStatus().setForeground(Color.RED);
                }
                window.getStatus().setText("Failed to install");
                window.getStatus().setForeground(Color.RED);
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(s -> { log.append(s); log.append("\n"); });
            }
        };
        worker.execute();
    }

    public static void deletePackage(String name, JTextArea log, MainWindow window) {
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                String executablePath = Settings.getExecutablePath();
                try {
                    Process process = new ProcessBuilder().command(executablePath, "remove", name, "--recurse").start();
                    try (
                            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                            BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String line = reader.readLine();
                        while (line != null) {
                            publish(line);
                            line = reader.readLine();
                        }
                    }
                    return process.waitFor() == 0;
                } catch (IOException | InterruptedException e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        window.updateListInstalled();
                        window.getStatus().setText("Successfully removed");
                        window.getStatus().setForeground(Color.GREEN);
                        return;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    window.getStatus().setText("Failed to remove");
                    window.getStatus().setForeground(Color.RED);
                }
                window.getStatus().setText("Failed to remove");
                window.getStatus().setForeground(Color.RED);
            }

            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(s -> { log.append(s); log.append("\n"); });
            }
        };
        worker.execute();
    }
}
