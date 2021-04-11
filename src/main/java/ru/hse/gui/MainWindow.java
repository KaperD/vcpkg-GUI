package ru.hse.gui;

import ru.hse.settings.Settings;
import ru.hse.vcpkg.VcPackage;
import ru.hse.vcpkg.VcpkgController;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

class CellRendererWithIcon extends DefaultListCellRenderer {
    Icon icon;
    CellRendererWithIcon(Icon icon) {
        this.icon = icon;
    }
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setIcon(icon);
        return this;
    }
}

public class MainWindow extends JFrame {
    private final JList<VcPackage> listInstalled = new JList<>();
    private final JList<VcPackage> listAll = new JList<>();

    private final JLabel status = new JLabel();
    private final JTextArea log = new JTextArea(15, 0);

    private final JButton install = new JButton("Install");
    private final JButton remove = new JButton("Remove");
    private final JButton settings = new JButton("Settings");

    private final JTextArea version = new JTextArea();
    private final JTextArea description = new JTextArea(4, 0);
    private final JTextArea name = new JTextArea();

    public JLabel getStatus() {
        return status;
    }

    public void updateListInstalled() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            final DefaultListModel<VcPackage> newModelInstalled = new DefaultListModel<>();
            @Override
            protected Void doInBackground() {
                newModelInstalled.addAll(VcpkgController.getInstalledPackages());
                return null;
            }

            @Override
            protected void done() {
                listInstalled.setModel(newModelInstalled);
            }
        };
        worker.execute();
    }

    public void updateListAll() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            final DefaultListModel<VcPackage> newModelAll = new DefaultListModel<>();
            @Override
            protected Void doInBackground() {
                newModelAll.addAll(VcpkgController.getAllPackages());
                return null;
            }

            @Override
            protected void done() {
                listAll.setModel(newModelAll);
            }
        };
        worker.execute();
    }

    private void updateInfo(VcPackage vcPackage) {
        name.setText(vcPackage.getName());
        name.updateUI();
        version.setText(vcPackage.getVersion());
        version.updateUI();
        description.setText(vcPackage.getDescription());
        description.updateUI();
    }

    public MainWindow() {
        super("Vcpkg GUI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JSplitPane splitHorizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitHorizontal.setDividerSize(4);
        splitHorizontal.setDividerLocation(0.4);

        ImageIcon icon;
        try (InputStream is = MainWindow.class.getResourceAsStream("/package.png")) {
            if (is != null) {
                icon = new ImageIcon(is.readAllBytes());
            } else {
                icon = null;
            }
        } catch (IOException e) {
            icon = null;
        }

        JTabbedPane pane = new JTabbedPane();

        updateListInstalled();
        updateListAll();

        listInstalled.setCellRenderer(new CellRendererWithIcon(icon));
        listInstalled.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listInstalled.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    VcPackage vcPackage = listInstalled.getSelectedValue();
                    if (vcPackage != null) {
                        install.setEnabled(false);
                        remove.setEnabled(true);
                        updateInfo(vcPackage);
                    }
                }
            }
        });

        listAll.setCellRenderer(new CellRendererWithIcon(icon));
        listAll.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listAll.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    VcPackage vcPackage = listAll.getSelectedValue();
                    if (vcPackage != null) {
                        install.setEnabled(true);
                        remove.setEnabled(false);
                        updateInfo(vcPackage);
                    }
                }
            }
        });

        pane.addTab("Installed", new JScrollPane(listInstalled));
        pane.addTab("   All   ", new JScrollPane(listAll));

        JPanel infoPanel = setupInfoPanel();

        splitHorizontal.setLeftComponent(pane);
        splitHorizontal.setRightComponent(infoPanel);

        if (Settings.getExecutablePath().isEmpty()) {
            log.setText("Please specify path to vcpkg executable in settings");
        }

        getContentPane().add(splitHorizontal);
        setSize(800, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel setupButtonsPanel() {
        install.setEnabled(false);
        install.addActionListener(e -> {
            String packageName = name.getText();
            if (packageName != null && !packageName.isEmpty()) {
                status.setText("Installing");
                status.setForeground(Color.ORANGE);
                log.setText("");
                VcpkgController.installPackage(packageName, log, this);
            }
        });

        remove.setEnabled(false);
        remove.addActionListener(e -> {
            String packageName = name.getText();
            if (packageName != null && !packageName.isEmpty()) {
                status.setText("Removing");
                status.setForeground(Color.ORANGE);
                log.setText("");
                VcpkgController.deletePackage(packageName, log, this);
            }
        });

        settings.addActionListener(e -> {
            String result = JOptionPane.showInputDialog(
                    MainWindow.this,
                    "Path to vcpkg executable",
                    Settings.getExecutablePath());
            if (result != null) {
                log.setText("");
                Settings.setExecutablePath(result);
                updateListInstalled();
                updateListAll();
            }
        });


        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(install);
        buttonsPanel.add(remove);
        buttonsPanel.add(settings);

        return buttonsPanel;
    }

    private JPanel setupInfoPanel() {
        JLabel nameLabel = new JLabel("Name:");
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setEditable(false);

        JLabel versionLabel = new JLabel("Version:");
        version.setLineWrap(true);
        version.setWrapStyleWord(true);
        version.setEditable(false);

        JLabel descriptionLabel = new JLabel("Description:");
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);

        JLabel logLabel = new JLabel("Log:");
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setEditable(false);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        class Util {
            void addLabel(JLabel label, int row) {
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.1;
                c.gridx = 0;
                c.gridy = row;
                c.gridwidth = 1;
                c.insets = new Insets(5, 5, 5, 5);
                c.anchor = GridBagConstraints.FIRST_LINE_START;
                infoPanel.add(label, c);
            }

            void addTextArea(JTextArea area, int row) {
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0.9;
                c.gridx = 1;
                c.gridy = row;
                c.gridwidth = 2;
                JPanel p = new JPanel(new BorderLayout());
                p.add(new JScrollPane(area), BorderLayout.CENTER);
                infoPanel.add(p, c);
            }
        }

        Util util = new Util();

        util.addLabel(nameLabel, 0);
        util.addTextArea(name, 0);

        util.addLabel(versionLabel, 1);
        util.addTextArea(version, 1);

        util.addLabel(descriptionLabel, 2);
        util.addTextArea(description, 2);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.1;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.insets = new Insets(20, 5, 5, 5);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        infoPanel.add(logLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 4;
        c.insets = new Insets(0, 5, 5, 5);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(log), BorderLayout.CENTER);
        infoPanel.add(p, c);

        JLabel statusLabel = new JLabel("Status:");
        util.addLabel(statusLabel, 5);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.9;
        c.gridx = 1;
        c.gridwidth = 2;
        c.gridy = 5;
        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(status, BorderLayout.CENTER);
        infoPanel.add(p1, c);

        JPanel buttonsPanel = setupButtonsPanel();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.PAGE_END;
        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy = 6;
        c.weighty = 0.1;
        infoPanel.add(buttonsPanel, c);

        return infoPanel;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                ex.printStackTrace();
                System.err.println("Can't even use the default theme");
                System.exit(1);
            }
        }

        new MainWindow();
    }
}