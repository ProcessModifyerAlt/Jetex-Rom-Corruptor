import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

public class JetexRomCorruptor extends JFrame {

    private static final String APP_TITLE = "Jetex ROM Corruptor 1.0.1";
    // Fixed auto-save location (Windows-style), created if missing
    private static final File AUTO_DIR = new File("C:/jetexrom");
    private static final File AUTO_OUT = new File(AUTO_DIR, "JetexCorruptedROM.z64");

    // ROM / Emulator
    private File loadedRom;
    private File lastRomDir = null;
    private File lastSavedRom;
    private final JTextField romPathField = new JTextField(32);
    private final JButton openRomBtn = new JButton("Open ROM…");

    private final JTextField emulatorField = new JTextField(28);
    private final JButton browseEmuBtn = new JButton("Choose Emulator…");
    private final JButton launchEmuBtn = new JButton("Launch Emulator");

    // Builder
    private final JComboBox<Mode> modeBox = new JComboBox<>(Mode.values());
    private final JTextField startField = new JTextField("0x000000", 10);
    private final JTextField endField   = new JTextField("0x000000", 10);
    private final JCheckBox  autoEndChk = new JCheckBox("Auto End (ROM size)");
    private final JTextField everyField = new JTextField("1", 6);

    private final JLabel valueALabel = new JLabel("Value A:");
    private final JLabel valueBLabel = new JLabel("Value B:");
    private final JTextField valueAField = new JTextField(8);
    private final JTextField valueBField = new JTextField(8);

    private final JButton addTaskBtn = new JButton("Add to Queue");

    // Queue controls
    private final JCheckBox useQueueChk = new JCheckBox("Use Queue", true);
    private final JCheckBox mergeAnnotateChk = new JCheckBox("Merge & Annotate");
    private final JTextField tagField = new JTextField(16);

    private final DefaultListModel<CorruptionTask> tasksModel = new DefaultListModel<>();
    private final JList<CorruptionTask> tasksList = new JList<>(tasksModel);
    private final JButton removeTaskBtn = new JButton("Remove");
    private final JButton clearQueueBtn = new JButton("Clear");
    private final JButton upBtn = new JButton("▲");
    private final JButton dnBtn = new JButton("▼");

    // Actions
    private final JButton applyBtn = new JButton("Corrupt & Save…");
    private final JButton quickCorruptLaunchBtn = new JButton("Corrupt + Launch Emulator"); // NEW
    private final JButton savePresetBtn = new JButton("Save Preset…");
    private final JButton loadPresetBtn = new JButton("Load Preset…");

    // Log
    private final JTextArea log = new JTextArea(12, 90);

    public JetexRomCorruptor() {
        super(APP_TITLE + " — GUI");
        try {
            URL url = getClass().getResource("/JT_corruptor_32x32.png"); // note leading slash
            if (url == null) {
                System.err.println("Icon resource not found: /JT_corruptor_32x32.png");
            } else {
                Image img = ImageIO.read(url);
                setIconImage(img);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }        // ROM row
        setLocationRelativeTo(null); // center on screen
        setResizable(false);         // optional: lock the size
        JPanel romRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        romPathField.setEditable(false);
        romRow.add(new JLabel("ROM:"));
        romRow.add(romPathField);
        romRow.add(openRomBtn);
        romRow.setBorder(new TitledBorder("ROM"));

        // Emulator row
        JPanel emuRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        emuRow.add(new JLabel("Emulator:"));
        emuRow.add(emulatorField);
        emuRow.add(browseEmuBtn);
        emuRow.add(launchEmuBtn);
        emuRow.setBorder(new TitledBorder("Emulator"));

        // Builder
        JPanel builder = new JPanel(new GridBagLayout());
        builder.setBorder(new TitledBorder("Create Corruption Task"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        c.gridx=0; c.gridy=row; builder.add(new JLabel("Mode:"), c);
        c.gridx=1; c.gridy=row; builder.add(modeBox, c);

        row++;
        c.gridx=0; c.gridy=row; builder.add(new JLabel("Start:"), c);
        c.gridx=1; c.gridy=row; builder.add(startField, c);
        c.gridx=2; c.gridy=row; builder.add(new JLabel("End:"), c);
        c.gridx=3; c.gridy=row; builder.add(endField, c);
        c.gridx=4; c.gridy=row; builder.add(new JLabel("Every Nth:"), c);
        c.gridx=5; c.gridy=row; builder.add(everyField, c);

        row++;
        c.gridx=3; c.gridy=row; c.gridwidth=2; builder.add(autoEndChk, c); c.gridwidth=1;

        row++;
        c.gridx=0; c.gridy=row; builder.add(valueALabel, c);
        c.gridx=1; c.gridy=row; builder.add(valueAField, c);
        c.gridx=2; c.gridy=row; builder.add(valueBLabel, c);
        c.gridx=3; c.gridy=row; builder.add(valueBField, c);
        c.gridx=5; c.gridy=row; builder.add(addTaskBtn, c);

        // Queue panel
        JPanel queuePanel = new JPanel(new BorderLayout(6,6));
        queuePanel.setBorder(new TitledBorder("Corruption Queue (applied in order)"));
        tasksList.setVisibleRowCount(8);
        tasksList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queuePanel.add(new JScrollPane(tasksList), BorderLayout.CENTER);

        JPanel qTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        qTop.add(useQueueChk);
        qTop.add(mergeAnnotateChk);
        qTop.add(new JLabel("Tag:"));
        qTop.add(tagField);
        queuePanel.add(qTop, BorderLayout.NORTH);

        JPanel qBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        qBtns.add(removeTaskBtn);
        qBtns.add(clearQueueBtn);
        qBtns.add(new JLabel("Reorder:"));
        qBtns.add(upBtn);
        qBtns.add(dnBtn);
        queuePanel.add(qBtns, BorderLayout.SOUTH);

        // Bottom actions
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bottom.add(applyBtn);
        bottom.add(quickCorruptLaunchBtn); // NEW action
        bottom.add(savePresetBtn);
        bottom.add(loadPresetBtn);

        // Log
        log.setEditable(false);
        JScrollPane logPane = new JScrollPane(log);
        logPane.setBorder(new TitledBorder("Log"));

        // Main layout
        JPanel north = new JPanel(new BorderLayout(8,8));
        JPanel topRows = new JPanel(new GridLayout(1,2,8,8));
        topRows.add(romRow);
        topRows.add(emuRow);
        north.add(topRows, BorderLayout.NORTH);

        JPanel midRows = new JPanel(new BorderLayout(8,8));
        midRows.add(builder, BorderLayout.NORTH);
        midRows.add(queuePanel, BorderLayout.CENTER);
        north.add(midRows, BorderLayout.CENTER);

        setLayout(new BorderLayout(8,8));
        add(north, BorderLayout.NORTH);
        add(logPane, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Listeners
        openRomBtn.addActionListener(this::onOpenRom);
        browseEmuBtn.addActionListener(this::onChooseEmu);
        launchEmuBtn.addActionListener(this::onLaunchEmu);

        autoEndChk.addActionListener(e -> setAutoEndIfPossible());

        modeBox.addActionListener(e -> updateParamFields());
        addTaskBtn.addActionListener(this::onAddTask);
        removeTaskBtn.addActionListener(e -> { int i = tasksList.getSelectedIndex(); if (i>=0) tasksModel.remove(i); });
        clearQueueBtn.addActionListener(e -> tasksModel.clear());
        upBtn.addActionListener(e -> moveSelected(-1));
        dnBtn.addActionListener(e -> moveSelected(+1));

        applyBtn.addActionListener(this::onApplyDialogSave);
        quickCorruptLaunchBtn.addActionListener(this::onCorruptAndLaunch); // NEW
        savePresetBtn.addActionListener(this::onSavePreset);
        loadPresetBtn.addActionListener(this::onLoadPreset);
     // --- Right-click context menu for queue items ---
        JPopupMenu queueMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Corruption Queue");
        deleteItem.addActionListener(ev -> {
            int i = tasksList.getSelectedIndex();
            if (i >= 0) tasksModel.remove(i);
        });
        queueMenu.add(deleteItem);

        tasksList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            private void showMenu(java.awt.event.MouseEvent e) {
                int row = tasksList.locationToIndex(e.getPoint());
                if (row >= 0) {
                    tasksList.setSelectedIndex(row);
                    queueMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        updateParamFields();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ===== Actions =====

    private void onOpenRom(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open ROM");
        if (lastRomDir != null && lastRomDir.isDirectory()) {
            fc.setCurrentDirectory(lastRomDir);
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadedRom = fc.getSelectedFile();
            romPathField.setText(loadedRom.getAbsolutePath());
            lastRomDir = loadedRom.getParentFile(); // remember
            logln("Opened ROM: " + loadedRom.getName() + " (" + loadedRom.length() + " bytes)");
            setAutoEndIfPossible();
        }
    }

    private void onChooseEmu(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Emulator Executable");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File emu = fc.getSelectedFile();
            emulatorField.setText(emu.getAbsolutePath());
            logln("Emulator set: " + emu.getAbsolutePath());
        }
    }

    private void onLaunchEmu(ActionEvent e) {
        if (emulatorField.getText().trim().isEmpty()) { warn("Choose an emulator first."); return; }
        File romToRun = (lastSavedRom != null && lastSavedRom.exists()) ? lastSavedRom : loadedRom;
        if (romToRun == null) { warn("No ROM to run. Open or corrupt & save first."); return; }
        try {
            logln("Launching emulator…");
            new ProcessBuilder(emulatorField.getText().trim(), romToRun.getAbsolutePath())
                    .inheritIO().start();
        } catch (Exception ex) { err("Launch failed: " + ex.getMessage()); }
    }

    private void onAddTask(ActionEvent e) {
        if (loadedRom == null) { warn("Open a ROM first."); return; }
        try {
            CorruptionTask t = buildTaskFromUI();
            tasksModel.addElement(t);
            logln("Added: " + t.summary());
        } catch (Exception ex) { err("Bad task parameters: " + ex.getMessage()); }
    }

    // Standard save dialog flow
    private void onApplyDialogSave(ActionEvent e) {
        if (loadedRom == null) { warn("Open a ROM first."); return; }

        CorruptionTask[] toApply = buildTasksForApply();
        if (toApply == null) return;

        JFileChooser saver = new JFileChooser();
        if (lastRomDir != null && lastRomDir.isDirectory()) {
            saver.setCurrentDirectory(lastRomDir);
        }
        saver.setSelectedFile(defaultOutFile());        saver.setSelectedFile(defaultOutFile());
        if (mergeAnnotateChk.isSelected() && !tagField.getText().trim().isEmpty()) {
            File suggested = new File(defaultOutFile().getParentFile(),
                    appendTagToName(defaultOutFile().getName(), tagField.getText().trim()));
            saver.setSelectedFile(suggested);
        }
        if (saver.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File outFile = saver.getSelectedFile();
        corruptSaveAndMaybeAnnotate(toApply, outFile, mergeAnnotateChk.isSelected(), tagField.getText().trim());
        lastSavedRom = outFile;
    }

    // NEW: one-click corrupt + auto-save + auto-launch
    private void onCorruptAndLaunch(ActionEvent e) {
        if (loadedRom == null) { warn("Open a ROM first."); return; }
        if (emulatorField.getText().trim().isEmpty()) { warn("Choose an emulator first."); return; }

        CorruptionTask[] toApply = buildTasksForApply();
        if (toApply == null) return;

        // Ensure auto dir exists
        if (!AUTO_DIR.exists() && !AUTO_DIR.mkdirs()) {
            err("Could not create folder: " + AUTO_DIR.getAbsolutePath());
            return;
        }

        // Always save to fixed path
        if (corruptSaveAndMaybeAnnotate(toApply, AUTO_OUT, true, tagField.getText().trim())) {
            lastSavedRom = AUTO_OUT;
            // Launch emulator
            try {
                logln("Launching emulator with " + AUTO_OUT.getAbsolutePath());
                new ProcessBuilder(emulatorField.getText().trim(), AUTO_OUT.getAbsolutePath())
                        .inheritIO().start();
            } catch (Exception ex) { err("Launch failed: " + ex.getMessage()); }
        }
    }

    private CorruptionTask[] buildTasksForApply() {
        CorruptionTask[] toApply;
        if (useQueueChk.isSelected()) {
            if (tasksModel.isEmpty()) { warn("Queue is empty."); return null; }
            toApply = new CorruptionTask[tasksModel.size()];
            for (int i = 0; i < tasksModel.size(); i++) toApply[i] = tasksModel.get(i);
        } else {
            try {
                toApply = new CorruptionTask[]{ buildTaskFromUI() };
            } catch (Exception ex) { err("Bad task parameters: " + ex.getMessage()); return null; }
        }
        return toApply;
    }

    private boolean corruptSaveAndMaybeAnnotate(CorruptionTask[] toApply, File outFile, boolean annotate, String tag) {
        try {
            byte[] data = readAll(loadedRom);
            for (int i = 0; i < toApply.length; i++) {
                toApply[i].apply(data);
                logln("Applied [" + (i+1) + "/" + toApply.length + "]: " + toApply[i].summary());
            }
            writeAll(outFile, data);
            logln("Saved: " + outFile.getAbsolutePath());
            if (annotate) writeAnnotation(outFile, tag, toApply);
            info("Done.\nSaved to:\n" + outFile.getAbsolutePath());
            return true;
        } catch (Exception ex) {
            err("Corrupt/Save failed: " + ex.getMessage());
            return false;
        }
    }

    private void onSavePreset(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Preset");
        fc.setSelectedFile(new File("jetex_preset.properties"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File preset = fc.getSelectedFile();
        try (FileOutputStream fos = new FileOutputStream(preset)) {
            Properties p = new Properties();
            p.setProperty("app", APP_TITLE);
            p.setProperty("emulator", emulatorField.getText().trim());
            p.setProperty("useQueue", Boolean.toString(useQueueChk.isSelected()));
            p.setProperty("mergeAnnotate", Boolean.toString(mergeAnnotateChk.isSelected()));
            p.setProperty("tag", tagField.getText().trim());
            p.setProperty("queue.size", Integer.toString(tasksModel.size()));
            for (int i = 0; i < tasksModel.size(); i++) {
                CorruptionTask t = tasksModel.get(i);
                String base = "queue." + i + ".";
                p.setProperty(base + "mode", t.mode.name());
                p.setProperty(base + "start", "0x" + Long.toHexString(t.start).toUpperCase());
                p.setProperty(base + "end",   "0x" + Long.toHexString(t.end).toUpperCase());
                p.setProperty(base + "every", Long.toString(t.every));
                if (t.a != null) p.setProperty(base + "a", Integer.toString(t.a));
                if (t.b != null) p.setProperty(base + "b", Integer.toString(t.b));
            }
            // store builder
            p.setProperty("builder.mode", ((Mode)modeBox.getSelectedItem()).name());
            p.setProperty("builder.start", startField.getText().trim());
            p.setProperty("builder.end", endField.getText().trim());
            p.setProperty("builder.autoEnd", Boolean.toString(autoEndChk.isSelected()));
            p.setProperty("builder.every", everyField.getText().trim());
            p.setProperty("builder.a", valueAField.getText().trim());
            p.setProperty("builder.b", valueBField.getText().trim());
            p.setProperty("rom.dir", (lastRomDir != null) ? lastRomDir.getAbsolutePath() : "");
            p.setProperty("rom.path", (loadedRom != null) ? loadedRom.getAbsolutePath() : "");
            
            p.store(fos, APP_TITLE + " preset");
            logln("Preset saved: " + preset.getAbsolutePath());
        } catch (IOException ex) { err("Save preset failed: " + ex.getMessage()); }
    }

    private void onLoadPreset(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Preset");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File preset = fc.getSelectedFile();
        try (FileInputStream fis = new FileInputStream(preset)) {
            Properties p = new Properties();
            p.load(fis);
            String dirStr = p.getProperty("rom.dir", "");
            if (!dirStr.isEmpty()) {
                File d = new File(dirStr);
                if (d.isDirectory()) lastRomDir = d;
            }

            String romStr = p.getProperty("rom.path", "");
            if (!romStr.isEmpty()) {
                File r = new File(romStr);
                if (r.isFile()) {
                    loadedRom = r;
                    romPathField.setText(r.getAbsolutePath());
                    logln("Restored ROM from preset: " + r.getAbsolutePath());
                    setAutoEndIfPossible();
                } else {
                    logln("Preset ROM path not found: " + romStr);
                }
            }
            emulatorField.setText(p.getProperty("emulator", ""));
            useQueueChk.setSelected(Boolean.parseBoolean(p.getProperty("useQueue", "true")));
            mergeAnnotateChk.setSelected(Boolean.parseBoolean(p.getProperty("mergeAnnotate", "true")));
            tagField.setText(p.getProperty("tag", ""));

            tasksModel.clear();
            int n = Integer.parseInt(p.getProperty("queue.size", "0"));
            for (int i = 0; i < n; i++) {
                String base = "queue." + i + ".";
                Mode m = Mode.valueOf(p.getProperty(base + "mode"));
                long start = parseNumber(p.getProperty(base + "start"));
                long end   = parseNumber(p.getProperty(base + "end"));
                long every = Long.parseLong(p.getProperty(base + "every"));
                String aStr = p.getProperty(base + "a", null);
                String bStr = p.getProperty(base + "b", null);
                Integer a = (aStr == null || aStr.isEmpty()) ? null : Integer.parseInt(aStr);
                Integer b = (bStr == null || bStr.isEmpty()) ? null : Integer.parseInt(bStr);
                tasksModel.addElement(new CorruptionTask(m, start, end, every, a, b));
            }

            // restore builder fields
            try { modeBox.setSelectedItem(Mode.valueOf(p.getProperty("builder.mode", Mode.RANDOM.name()))); }
            catch (Exception ignore) {}
            startField.setText(p.getProperty("builder.start", "0x000000"));
            endField.setText(p.getProperty("builder.end", "0x000000"));
            autoEndChk.setSelected(Boolean.parseBoolean(p.getProperty("builder.autoEnd", "false")));
            everyField.setText(p.getProperty("builder.every", "1"));
            valueAField.setText(p.getProperty("builder.a", ""));
            valueBField.setText(p.getProperty("builder.b", ""));

            setAutoEndIfPossible();
            logln("Preset loaded: " + preset.getAbsolutePath() + "  (" + n + " tasks)");
        } catch (Exception ex) { err("Load preset failed: " + ex.getMessage()); }
    }

    // ===== Helpers =====

    private CorruptionTask buildTaskFromUI() {
        Mode mode = (Mode) modeBox.getSelectedItem();
        long start = parseNumber(startField.getText().trim());
        long end   = parseNumber(endField.getText().trim());
        if (loadedRom != null) {
            long len = loadedRom.length();
            if (start < 0) start = 0;
            if (end >= len) end = len - 1;
        }
        if (start > end) { long t = start; start = end; end = t; }

        long every = parseNumber(everyField.getText().trim());
        if (every <= 0) every = 1;

        Integer a = null, b = null;
        switch (mode) {
            case ADD:
            case XOR:
            case SHIFT:
                a = (int) parseSigned(valueAField.getText().trim());
                break;
            case REPLACE:
                a = (int) parseNumber(valueAField.getText().trim()); // from
                b = (int) parseNumber(valueBField.getText().trim()); // to
                break;
            case RANDOM:
            case FLIP:
                break;
        }
        return new CorruptionTask(mode, start, end, every, a, b);
    }

    private void moveSelected(int dir) {
        int i = tasksList.getSelectedIndex();
        if (i < 0) return;
        int j = i + dir;
        if (j < 0 || j >= tasksModel.size()) return;
        CorruptionTask t = tasksModel.remove(i);
        tasksModel.add(j, t);
        tasksList.setSelectedIndex(j);
    }

    private void updateParamFields() {
        Mode m = (Mode) modeBox.getSelectedItem();
        boolean aOn = false, bOn = false;
        String aText = "Value A:", bText = "Value B:";
        switch (m) {
            case ADD:   aOn = true; aText = "Amount (±):"; break;
            case XOR:   aOn = true; aText = "Mask:"; break;
            case SHIFT: aOn = true; aText = "Bits (±):"; break;
            case REPLACE: aOn = true; bOn = true; aText = "From:"; bText = "To:"; break;
            case RANDOM:
            case FLIP:
                break;
        }
        valueALabel.setText(aText);
        valueBLabel.setText(bText);
        valueALabel.setEnabled(aOn);
        valueBLabel.setEnabled(bOn);
        valueAField.setEnabled(aOn);
        valueBField.setEnabled(bOn);
        if (!aOn) valueAField.setText("");
        if (!bOn) valueBField.setText("");
    }

    private void setAutoEndIfPossible() {
        if (autoEndChk.isSelected() && loadedRom != null) {
            long end = loadedRom.length() - 1;
            endField.setText(String.format("0x%06X", end));
        }
    }

    private File defaultOutFile() {
        File baseDir = (loadedRom != null) ? loadedRom.getParentFile() : new File(".");
        String baseName = (loadedRom != null) ? loadedRom.getName() : "rom";
        String outName = baseName.replaceAll("(\\.[^.]+)?$", "_corrupt.z64");
        return new File(baseDir, outName);
    }

    private static String appendTagToName(String fileName, String tag) {
        String cleanTag = tag.replaceAll("[^a-zA-Z0-9._-]+", "_");
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) return fileName + "_" + cleanTag;
        return fileName.substring(0, dot) + "_" + cleanTag + fileName.substring(dot);
    }

    private void writeAnnotation(File outFile, String tag, CorruptionTask[] tasks) {
        try {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            File sidecar = new File(outFile.getParentFile(),
                    outFile.getName().replaceAll("(\\.[^.]+)?$", "") + ".corruption.txt");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(sidecar), "UTF-8"))) {
                pw.println(APP_TITLE + " — Merge Annotation");
                pw.println("Timestamp : " + ts);
                pw.println("ROM       : " + (loadedRom == null ? "(unknown)" : loadedRom.getAbsolutePath()));
                pw.println("Output    : " + outFile.getAbsolutePath());
                pw.println("Tag       : " + (tag == null ? "" : tag));
                pw.println("Tasks     : " + tasks.length);
                pw.println("----------------------------------------");
                for (int i = 0; i < tasks.length; i++) {
                    pw.println(String.format("[%02d] %s", i+1, tasks[i].summary()));
                }
            }
            logln("Wrote annotation: " + sidecar.getAbsolutePath());
        } catch (Exception ex) {
            err("Failed to write annotation: " + ex.getMessage());
        }
    }

    private static byte[] readAll(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(1024, (int)f.length()));
            byte[] buf = new byte[1<<20];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        }
    }
    private static void writeAll(File f, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(data); }
    }

    // number parsing: accepts 0xHEX, bare HEX, or decimal
    private static long parseNumber(String s) {
        s = s.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty number");
        boolean neg = s.startsWith("-");
        if (neg) s = s.substring(1).trim();
        long val;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            val = Long.parseLong(s.substring(2), 16);
        } else if (s.matches("(?i)[0-9A-F]+")) {
            val = Long.parseLong(s, 16);
        } else {
            val = Long.parseLong(s, 10);
        }
        return neg ? -val : val;
    }
    private static long parseSigned(String s) { return parseNumber(s); }

    private void logln(String msg) { log.append(msg + "\n"); log.setCaretPosition(log.getDocument().getLength()); }
    private void warn(String msg)  { JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private void err(String msg)   { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); logln("[ERROR] " + msg); }

    public static void main(String[] args) {
        JOptionPane.showMessageDialog(
                null,
                "NOTE: This works in N64 ROMs only.\n\n" +
                "This is an N64 corruptor.\n" +
                "NES, SNES corruptor options coming soon!",
                "Jetex ROM Corruptor 1.0.1 - Notice",
                JOptionPane.WARNING_MESSAGE
            );
        SwingUtilities.invokeLater(JetexRomCorruptor::new);
    }

    // ===== Modes & Task =====

    enum Mode { ADD, REPLACE, SHIFT, XOR, RANDOM, FLIP }

    static final class CorruptionTask {
        final Mode mode;
        final long start, end, every;
        final Integer a; // amount/mask/bits/from
        final Integer b; // to
        final Random rng = new Random();

        CorruptionTask(Mode mode, long start, long end, long every, Integer a, Integer b) {
            this.mode = mode;
            this.start = Math.max(0, start);
            this.end   = Math.max(0, end);
            this.every = Math.max(1, every);
            this.a = a; this.b = b;
        }

        void apply(byte[] rom) {
            final int len = rom.length;
            int s = (int)Math.min(Math.max(0, start), Math.max(0, len - 1));
            int e = (int)Math.min(Math.max(0, end),   Math.max(0, len - 1));
            if (e < s) { int t = s; s = e; e = t; }

            switch (mode) {
                case ADD: {
                    int amt = (a == null ? 0 : a);
                    for (int i = s; i <= e; i += every) {
                        int v = rom[i] & 0xFF;
                        v = (v + amt) & 0xFF;
                        rom[i] = (byte)v;
                    }
                    break;
                }
                case XOR: {
                    int mask = (a == null ? 0xFF : a) & 0xFF;
                    for (int i = s; i <= e; i += every) {
                        rom[i] = (byte)((rom[i] & 0xFF) ^ mask);
                    }
                    break;
                }
                case SHIFT: {
                    int bits = (a == null ? 1 : a);
                    for (int i = s; i <= e; i += every) {
                        int v = rom[i] & 0xFF;
                        if (bits >= 0) v = (v << bits) & 0xFF;
                        else           v = (v & 0xFF) >>> (-bits);
                        rom[i] = (byte)v;
                    }
                    break;
                }
                case RANDOM: {
                    for (int i = s; i <= e; i += every) rom[i] = (byte) rng.nextInt(256);
                    break;
                }
                case FLIP: {
                    for (int i = s; i <= e; i += every) rom[i] = (byte)(~rom[i]);
                    break;
                }
                case REPLACE: {
                    int from = (a == null ? 0 : a) & 0xFF;
                    int to   = (b == null ? 0 : b) & 0xFF;
                    for (int i = s; i <= e; i += every) {
                        if ((rom[i] & 0xFF) == from) rom[i] = (byte)to;
                    }
                    break;
                }
            }
        }

        String summary() {
            switch (mode) {
                case ADD:     return "ADD +" + a + "  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
                case XOR:     return "XOR ^" + b8(a) + "  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
                case SHIFT:   return "SHIFT " + a + " bits  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
                case RANDOM:  return "RANDOM  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
                case FLIP:    return "FLIP ~  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
                case REPLACE: return "REPLACE " + b8(a) + "→" + b8(b) + "  [" + hx(start) + "…"+ hx(end) + "]  every " + every;
            }
            return toString();
        }

        @Override public String toString() { return summary(); }

        private static String hx(long x){ return String.format("0x%06X", x); }
        private static String b8(Integer x){ return x==null? "-" : String.format("0x%02X",(x&0xFF)); }
    }
}
