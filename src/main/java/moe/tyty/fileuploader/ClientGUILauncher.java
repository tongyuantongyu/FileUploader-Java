package moe.tyty.fileuploader;

import com.ea.async.Async;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ClientGUILauncher extends JFrame {

    public boolean running = false;
    public boolean runningClick = false;

    static class HintTextField extends JTextField implements FocusListener {

        private final String hint;
        private boolean showingHint;

        public HintTextField(final String hint, int columns) {
            super(hint, columns);
            this.hint = hint;
            this.showingHint = true;
            super.addFocusListener(this);
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (this.getText().equals(hint)) {
                super.setText("");
                showingHint = false;
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (this.getText().isEmpty()) {
                super.setText(hint);
                showingHint = true;
            }
        }
    }

    static class DefaultTextField extends JTextField implements FocusListener {

        private final String default_value;
        private boolean nowDefault;
        private final String default_hint;

        public DefaultTextField(final String default_value, int columns) {
            super(columns);
            this.default_value = default_value;
            this.default_hint = "Default";
            this.nowDefault = true;
            super.setText(this.default_hint);
            super.addFocusListener(this);
        }

        public DefaultTextField(final String default_value, final String default_hint, int columns) {
            super(default_value, columns);
            this.default_value = default_value;
            this.default_hint = String.format("Default (%s)", default_hint);
            this.nowDefault = true;
            super.setText(this.default_hint);
            super.addFocusListener(this);
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (super.getText().equals(default_hint)) {
                super.setText("");
                nowDefault = false;
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (super.getText().isEmpty()) {
                super.setText(default_hint);
                nowDefault = true;
            }
        }

        public String getValue(int offs, int len) throws BadLocationException {
            if (super.getText().equals(default_hint)) {
                return default_value.substring(offs, offs + len);
            }
            return super.getText(offs, len);
        }

        public String getValue() {
            if (super.getText().equals(default_hint)) {
                return default_value;
            }
            return super.getText();
        }
    }

    public ClientGUILauncher() {
        setTitle("FileUploader - Java Client GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 250);
        setLayout(new BorderLayout());
        JLabel hint = new JLabel("Please Fill with right info: ");
        add(hint, BorderLayout.NORTH);
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        JPanel inputPanel = new JPanel(layout);
        JLabel status = new JLabel("Ready.");
        add(status, BorderLayout.SOUTH);

        // first line:  Server: Host, Port
        JLabel serverHint = new JLabel("Server: ");
        constraints.gridy = 0;
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        layout.setConstraints(serverHint, constraints);
        inputPanel.add(serverHint);
        JTextField hostField = new HintTextField("Host", 20);
        constraints.weightx = 0.5;
        constraints.gridwidth = 2;
        layout.setConstraints(hostField, constraints);
        inputPanel.add(hostField);
        JTextField portField = new HintTextField("Port", 5);
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        layout.setConstraints(portField, constraints);
        inputPanel.add(portField);

        // Second line:  Password: *password*
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        constraints.gridy = 1;
        JLabel passwordHint = new JLabel("Password: ");
        layout.setConstraints(passwordHint, constraints);
        inputPanel.add(passwordHint);
        JPasswordField keyField = new JPasswordField();
        constraints.weightx = 0.75;
        constraints.gridwidth = 3;
        layout.setConstraints(keyField, constraints);
        inputPanel.add(keyField);

        // Third line:  File: Path, Selector
        constraints.gridy = 2;
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        JLabel fileHint = new JLabel("File: ");
        layout.setConstraints(fileHint, constraints);
        inputPanel.add(fileHint);
        JTextField fileField = new HintTextField("Path", 20);
        constraints.weightx = 0.5;
        constraints.gridwidth = 2;
        layout.setConstraints(fileField, constraints);
        inputPanel.add(fileField);
        JButton fileSelector = new JButton("Select");
        fileSelector.addActionListener(ignored -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().toString());
            }
        });
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        layout.setConstraints(fileSelector, constraints);
        inputPanel.add(fileSelector);

        // Fourth line:  Save Path: path
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        constraints.gridy = 3;
        JLabel savePathHint = new JLabel("Save Path: ");
        layout.setConstraints(savePathHint, constraints);
        inputPanel.add(savePathHint);
        DefaultTextField savePathField = new DefaultTextField(null, "The same as file selected", 25);
        constraints.weightx = 0.75;
        constraints.gridwidth = 3;
        layout.setConstraints(savePathField, constraints);
        inputPanel.add(savePathField);

        // Fifth line:  Thread: 1 Size: 65536
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        constraints.gridy = 4;
        JLabel threadHint = new JLabel("Thread: ");
        layout.setConstraints(threadHint, constraints);
        inputPanel.add(threadHint);
        DefaultTextField threadField = new DefaultTextField("1", "1", 5);
        layout.setConstraints(threadField, constraints);
        inputPanel.add(threadField);
        JLabel sizeHint = new JLabel("Size: ");
        layout.setConstraints(sizeHint, constraints);
        inputPanel.add(sizeHint);
        DefaultTextField sizeField = new DefaultTextField("65536", "65536", 5);
        layout.setConstraints(sizeField, constraints);
        inputPanel.add(sizeField);

        // Sixth line:  Choose network mode
        constraints.weightx = 0.25;
        constraints.gridwidth = 1;
        constraints.gridy = 5;
        JLabel modeHint = new JLabel("Mode: ");
        layout.setConstraints(modeHint, constraints);
        inputPanel.add(modeHint);
        JRadioButton modeAuto = new JRadioButton("Auto", true);
        layout.setConstraints(modeAuto, constraints);
        inputPanel.add(modeAuto);
        JRadioButton modeIPv4 = new JRadioButton("IPv4 Only");
        layout.setConstraints(modeIPv4, constraints);
        inputPanel.add(modeIPv4);
        JRadioButton modeIPv6 = new JRadioButton("IPv6 Only");
        layout.setConstraints(modeIPv6, constraints);
        inputPanel.add(modeIPv6);
        ButtonGroup _group = new ButtonGroup();
        _group.add(modeAuto);
        _group.add(modeIPv4);
        _group.add(modeIPv6);

        constraints.gridy = 6;
        constraints.gridx = 2;
        JButton startButton = new JButton("Upload");
        startButton.addActionListener(ignored -> {
            if (runningClick) {
                return;
            }
            if (running) {
                runningClick = true;
                String nowText = status.getText();
                status.setText("An upload process in in progress. Please wait...");
                Timer timer = new Timer(1000, ignored2 -> {
                    if (status.getText().equals("An upload process in in progress. Please wait...")) {
                        status.setText(nowText);
                    }
                    runningClick = false;
                });
                return;
            }
            running = true;
            try {
                Client.OptionPack option = Client.buildOption(
                        hostField.getText(),
                        portField.getText(),
                        new String(keyField.getPassword()),
                        fileField.getText(),
                        threadField.getValue(),
                        sizeField.getValue(),
                        savePathField.getValue(),
                        modeIPv4.isSelected(),
                        modeIPv6.isSelected()
                );
                Client client = new Client(option);
                status.setText("Uploading... Please wait.");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            double timeConsume = client.runSession().get();
                            if (timeConsume != (double) -1) {
                                status.setText(String.format("File transfer finished in %f seconds.\n", timeConsume));
                            }
                        } catch (RuntimeException | InterruptedException | ExecutionException | IOException e) {
                            e.printStackTrace();
                            status.setText(String.format("[Failed] %s.", e.getMessage()));
                        } finally {
                            running = false;
                        }
                        return null;
                    }
                }.execute();
            } catch (RuntimeException e) {
                e.printStackTrace();
                status.setText(String.format("[Failed] %s.", e.getMessage()));
            } finally {
                running = false;
            }
        });
        constraints.weightx = 0.5;
        constraints.gridwidth = 2;
        layout.setConstraints(startButton, constraints);
        inputPanel.add(startButton);

        add(inputPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    public static void main(String[] args) {
        Async.init();
        new ClientGUILauncher();
    }
}
