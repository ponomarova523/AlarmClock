import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;

public class AlarmClock extends JFrame {
    private JLabel timeLabel;
    private JTable alarmTable;
    private DefaultTableModel tableModel;
    private List<Alarm> alarms;
    private javax.swing.Timer clockTimer;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private Thread soundThread;
    private volatile boolean stopSound = false;

    public AlarmClock() {
        alarms = new ArrayList<>();
        timeFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("uk"));

        setupUI();
        startClock();
    }

    private void setupUI() {
        setTitle("Будильник");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Верхня панель - поточний час
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(28, 46, 74));
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 60));
        timeLabel.setForeground(Color.WHITE);

        JLabel dateLabel = new JLabel(dateFormat.format(new Date()), SwingConstants.CENTER);
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        dateLabel.setForeground(new Color(240, 231, 213));

        topPanel.add(timeLabel, BorderLayout.CENTER);
        topPanel.add(dateLabel, BorderLayout.SOUTH);

        // Центральна панель - таблиця будильників
        String[] columnNames = {"Час", "Назва", "Дні тижня", "Увімкнено"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 3 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        alarmTable = new JTable(tableModel);
        alarmTable.setFont(new Font("Arial", Font.PLAIN, 14));
        alarmTable.setRowHeight(40);
        alarmTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        alarmTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        alarmTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        alarmTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        alarmTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 3) {
                int row = e.getFirstRow();
                boolean enabled = (Boolean) tableModel.getValueAt(row, 3);
                alarms.get(row).setEnabled(enabled);
            }
        });

        JScrollPane scrollPane = new JScrollPane(alarmTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Нижня панель - кнопки
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JButton addButton = createStyledButton("Додати будильник", new Color(167, 243, 193));
        JButton deleteButton = createStyledButton("Видалити", new Color(251, 172, 172));
        JButton searchButton = createStyledButton("Пошук", new Color(246, 230, 175));

        addButton.addActionListener(e -> showAddAlarmDialog());
        deleteButton.addActionListener(e -> deleteSelectedAlarm());
        searchButton.addActionListener(e -> showSearchDialog());

        bottomPanel.add(addButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(searchButton);

        // Додавання панелей
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(200, 45));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void startClock() {
        clockTimer = new javax.swing.Timer(1000, e -> {
            Date now = new Date();
            timeLabel.setText(timeFormat.format(now));
            checkAlarms(now);
        });
        clockTimer.start();
    }

    private void checkAlarms(Date now) {
        String currentTime = new SimpleDateFormat("HH:mm").format(now);
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);

        // ЦИКЛІЧНИЙ АЛГОРИТМ: Перевірка всіх будильників
        // Цикл проходить через всі будильники та перевіряє умови спрацювання
        for (Alarm alarm : alarms) {
            if (alarm.isEnabled() && alarm.getTime().equals(currentTime) &&
                    now.getSeconds() == 0 && !alarm.isRinging() &&
                    alarm.isActiveOnDay(currentDay)) {
                alarm.setRinging(true);
                playAlarmSound();
                showAlarmDialog(alarm);
            }
        }
    }

    private void showAddAlarmDialog() {
        JDialog dialog = new JDialog(this, "Додати будильник", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Час
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timePanel.add(new JLabel("Час (ГГ:ХХ):"));
        JTextField timeField = new JTextField("07:00", 10);
        timePanel.add(timeField);
        mainPanel.add(timePanel);

        // Назва
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.add(new JLabel("Назва:"));
        JTextField labelField = new JTextField("Будильник", 20);
        labelPanel.add(labelField);
        mainPanel.add(labelPanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Дні тижня
        JPanel daysPanel = new JPanel();
        daysPanel.setLayout(new BoxLayout(daysPanel, BoxLayout.Y_AXIS));
        daysPanel.setBorder(BorderFactory.createTitledBorder("Дні тижня"));

        JCheckBox[] dayCheckboxes = new JCheckBox[7];
        String[] dayNames = {"Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця", "Субота", "Неділя"};

        for (int i = 0; i < 7; i++) {
            dayCheckboxes[i] = new JCheckBox(dayNames[i], true);
            daysPanel.add(dayCheckboxes[i]);
        }

        mainPanel.add(daysPanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Повторювати
        JCheckBox repeatCheckbox = new JCheckBox("Повторювати щотижня", true);
        repeatCheckbox.setFont(new Font("Arial", Font.BOLD, 12));
        mainPanel.add(repeatCheckbox);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveButton = new JButton("Зберегти");
        JButton cancelButton = new JButton("Скасувати");

        saveButton.addActionListener(e -> {
            String time = timeField.getText();
            String label = labelField.getText();

            if (time.matches("\\d{2}:\\d{2}")) {
                Set<Integer> activeDays = new HashSet<>();
                for (int i = 0; i < 7; i++) {
                    if (dayCheckboxes[i].isSelected()) {
                        // Конвертуємо в Calendar.DAY_OF_WEEK (Неділя = 1, Понеділок = 2, ...)
                        activeDays.add(i == 6 ? 1 : i + 2);
                    }
                }

                if (activeDays.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Виберіть хоча б один день тижня!",
                            "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                addAlarm(time, label, activeDays, repeatCheckbox.isSelected());
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Невірний формат часу! Використовуйте ГГ:ХХ",
                        "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addAlarm(String time, String label, Set<Integer> activeDays, boolean repeat) {
        Alarm alarm = new Alarm(time, label, activeDays, repeat);
        alarms.add(alarm);

        // АЛГОРИТМ СОРТУВАННЯ: Сортування бульбашкою (Bubble Sort)
        // Сортуємо будильники за часом після додавання нового
        for (int i = 0; i < alarms.size() - 1; i++) {
            for (int j = 0; j < alarms.size() - i - 1; j++) {
                if (compareTime(alarms.get(j).getTime(), alarms.get(j + 1).getTime()) > 0) {
                    // Міняємо місцями
                    Alarm temp = alarms.get(j);
                    alarms.set(j, alarms.get(j + 1));
                    alarms.set(j + 1, temp);
                }
            }
        }

        // Оновлюємо таблицю після сортування
        refreshTable();
    }

    // Допоміжний метод для порівняння часу
    private int compareTime(String time1, String time2) {
        String[] parts1 = time1.split(":");
        String[] parts2 = time2.split(":");
        int hour1 = Integer.parseInt(parts1[0]);
        int min1 = Integer.parseInt(parts1[1]);
        int hour2 = Integer.parseInt(parts2[0]);
        int min2 = Integer.parseInt(parts2[1]);

        if (hour1 != hour2) {
            return hour1 - hour2;
        }
        return min1 - min2;
    }

    // Оновлення таблиці
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Alarm alarm : alarms) {
            String daysStr = formatDaysString(alarm.getActiveDays());
            if (!alarm.isRepeat()) {
                daysStr += " (один раз)";
            }
            tableModel.addRow(new Object[]{alarm.getTime(), alarm.getLabel(), daysStr, alarm.isEnabled()});
        }
    }

    private String formatDaysString(Set<Integer> days) {
        if (days.size() == 7) {
            return "Щодня";
        }

        String[] shortDays = {"Нд", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};
        List<String> daysList = new ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            if (days.contains(i)) {
                daysList.add(shortDays[i - 1]);
            }
        }

        return String.join(", ", daysList);
    }

    private void deleteSelectedAlarm() {
        int selectedRow = alarmTable.getSelectedRow();
        if (selectedRow >= 0) {
            alarms.remove(selectedRow);
            tableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Будь ласка, виберіть будильник для видалення",
                    "Увага", JOptionPane.WARNING_MESSAGE);
        }
    }

    // АЛГОРИТМ ПОШУКУ: Лінійний пошук
    // Пошук будильника за назвою або часом
    private void showSearchDialog() {
        String searchTerm = JOptionPane.showInputDialog(this,
                "Введіть назву або час будильника для пошуку:",
                "Пошук будильника",
                JOptionPane.QUESTION_MESSAGE);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            searchTerm = searchTerm.trim().toLowerCase();

            // Лінійний пошук по всіх будильниках
            List<Integer> foundIndices = new ArrayList<>();
            for (int i = 0; i < alarms.size(); i++) {
                Alarm alarm = alarms.get(i);
                if (alarm.getLabel().toLowerCase().contains(searchTerm) ||
                        alarm.getTime().contains(searchTerm)) {
                    foundIndices.add(i);
                }
            }

            if (foundIndices.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Будильник не знайдено",
                        "Результат пошуку",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Виділяємо перший знайдений результат
                alarmTable.setRowSelectionInterval(foundIndices.get(0), foundIndices.get(0));
                alarmTable.scrollRectToVisible(alarmTable.getCellRect(foundIndices.get(0), 0, true));

                JOptionPane.showMessageDialog(this,
                        "Знайдено " + foundIndices.size() + " будильник(ів)",
                        "Результат пошуку",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void showAlarmDialog(Alarm alarm) {
        JDialog dialog = new JDialog(this, "БУДИЛЬНИК!", false);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setAlwaysOnTop(true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(new Color(2, 2, 2, 100));

        JLabel timeLabel = new JLabel(alarm.getTime());
        timeLabel.setFont(new Font("Arial", Font.BOLD, 48));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setForeground(Color.WHITE);

        JLabel labelLabel = new JLabel(alarm.getLabel());
        labelLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        labelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelLabel.setForeground(Color.WHITE);

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(timeLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(labelLabel);
        contentPanel.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(2, 2, 2, 100));

        JButton stopButton = createStyledButton("Вимкнути", Color.WHITE);
        stopButton.setForeground(new Color(2, 2, 2, 100));

        // Панель для відкладення
        JPanel snoozePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        snoozePanel.setBackground(new Color(2, 2, 2, 100));

        JLabel snoozeLabel = new JLabel("Відкладти на:");
        snoozeLabel.setForeground(Color.WHITE);

        SpinnerNumberModel model = new SpinnerNumberModel(5, 1, 60, 1);
        JSpinner snoozeSpinner = new JSpinner(model);
        snoozeSpinner.setPreferredSize(new Dimension(60, 30));

        JLabel minutesLabel = new JLabel("хв");
        minutesLabel.setForeground(Color.WHITE);

        JButton snoozeButton = new JButton("Відкласти");
        snoozeButton.setBackground(new Color(255, 255, 255, 200));
        snoozeButton.setForeground(new Color(2, 2, 2, 100));
        snoozeButton.setFont(new Font("Arial", Font.BOLD, 14));
        snoozeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        snoozePanel.add(snoozeLabel);
        snoozePanel.add(snoozeSpinner);
        snoozePanel.add(minutesLabel);
        snoozePanel.add(snoozeButton);

        stopButton.addActionListener(e -> {
            stopAlarmSound();
            alarm.setRinging(false);

            // Якщо будильник не повторюється, вимикаємо його
            if (!alarm.isRepeat()) {
                alarm.setEnabled(false);
                updateTableRow(alarm);
            }

            dialog.dispose();
        });

        snoozeButton.addActionListener(e -> {
            stopAlarmSound();
            alarm.setRinging(false);
            int minutes = (Integer) snoozeSpinner.getValue();
            snoozeAlarm(alarm, minutes);
            dialog.dispose();
        });

        buttonPanel.add(stopButton);

        dialog.add(contentPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(2, 2, 2, 100));
        bottomPanel.add(snoozePanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(bottomPanel, BorderLayout.SOUTH);

        // Зупинити звук при закритті вікна
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAlarmSound();
                alarm.setRinging(false);
            }
        });

        dialog.setVisible(true);
    }

    private void snoozeAlarm(Alarm alarm, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, minutes);
        String snoozeTime = new SimpleDateFormat("HH:mm").format(cal.getTime());

        // Створюємо одноразовий будильник на поточний день
        Set<Integer> currentDay = new HashSet<>();
        currentDay.add(cal.get(Calendar.DAY_OF_WEEK));

        addAlarm(snoozeTime, alarm.getLabel() + " (відкладено " + minutes + " хв)", currentDay, false);
    }

    private void updateTableRow(Alarm alarm) {
        int index = alarms.indexOf(alarm);
        if (index >= 0) {
            tableModel.setValueAt(alarm.isEnabled(), index, 3);
        }
    }

    private void playAlarmSound() {
        stopSound = false;
        soundThread = new Thread(() -> {
            SourceDataLine line = null;
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                byte[] buffer = new byte[4410];

                // Грати звук поки не зупинять
                while (!stopSound) {
                    for (int i = 0; i < 10 && !stopSound; i++) {
                        double frequency = (i % 2 == 0) ? 800.0 : 1000.0;
                        for (int j = 0; j < buffer.length; j++) {
                            double angle = j / (44100.0 / frequency) * 2.0 * Math.PI;
                            buffer[j] = (byte) (Math.sin(angle) * 127.0);
                        }
                        if (!stopSound) {
                            line.write(buffer, 0, buffer.length);
                        }
                    }
                }

                line.drain();
                line.stop();
                line.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (line != null && line.isOpen()) {
                    line.close();
                }
            }
        });
        soundThread.start();
    }

    private void stopAlarmSound() {
        stopSound = true;
        if (soundThread != null && soundThread.isAlive()) {
            soundThread.interrupt();
        }
    }

    // Клас для будильника
    class Alarm {
        private String time;
        private String label;
        private Set<Integer> activeDays;
        private boolean enabled;
        private boolean ringing;
        private boolean repeat;

        public Alarm(String time, String label, Set<Integer> activeDays, boolean repeat) {
            this.time = time;
            this.label = label;
            this.activeDays = activeDays;
            this.enabled = true;
            this.ringing = false;
            this.repeat = repeat;
        }

        public String getTime() { return time; }
        public String getLabel() { return label; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isRinging() { return ringing; }
        public void setRinging(boolean ringing) { this.ringing = ringing; }
        public boolean isRepeat() { return repeat; }

        public boolean isActiveOnDay(int dayOfWeek) {
            return activeDays.contains(dayOfWeek);
        }

        public Set<Integer> getActiveDays() {
            return activeDays;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            AlarmClock clock = new AlarmClock();
            clock.setVisible(true);
        });
    }
}
