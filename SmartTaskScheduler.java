import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.PriorityBlockingQueue;

class Task implements Comparable<Task>, Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private int priority; // 1-highest, 5-lowest for example
    private Date deadline;

    public Task(String title, int priority, Date deadline) {
        this.title = title;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String getTitle() {
        return title;
    }

    public int getPriority() {
        return priority;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    @Override
    public int compareTo(Task other) {
        // Priority -> deadline order
        if (this.priority != other.priority) {
            return Integer.compare(this.priority, other.priority);
        }
        return this.deadline.compareTo(other.deadline);
    }

    @Override
    public String toString() {
        return "Task[title=" + title + ", priority=" + priority + ", deadline=" + deadline + "]";
    }
}

public class SmartTaskScheduler extends JFrame {
    private PriorityQueue<Task> taskQueue;
    private DefaultTableModel tableModel;
    private JTable taskTable;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Timer reminderTimer;
    private List<Task> taskList; // for filtering & saving

    public SmartTaskScheduler() {
        setTitle("Smart Task Scheduler");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        taskQueue = new PriorityQueue<>();
        taskList = new ArrayList<>();

        // Table Setup
        String[] colNames = {"Title", "Priority", "Deadline"};
        tableModel = new DefaultTableModel(colNames, 0) {
            public boolean isCellEditable(int row, int col) {
                return false; 
            }
        };
        taskTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(taskTable);

        // Buttons
        JButton addBtn = new JButton("Add Task");
        JButton editBtn = new JButton("Edit Task");
        JButton deleteBtn = new JButton("Delete Task");
        JButton saveBtn = new JButton("Save Tasks");
        JButton loadBtn = new JButton("Load Tasks");
        JButton filterTodayBtn = new JButton("Filter Today");
        JButton filterHighPriorityBtn = new JButton("Filter High Priority");
        JButton showAllBtn = new JButton("Show All");

        JPanel panelButtons = new JPanel();
        panelButtons.add(addBtn);
        panelButtons.add(editBtn);
        panelButtons.add(deleteBtn);
        panelButtons.add(saveBtn);
        panelButtons.add(loadBtn);
        panelButtons.add(filterTodayBtn);
        panelButtons.add(filterHighPriorityBtn);
        panelButtons.add(showAllBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(panelButtons, BorderLayout.SOUTH);

        // Button Actions
        addBtn.addActionListener(e -> addTaskDialog());
        editBtn.addActionListener(e -> editTaskDialog());
        deleteBtn.addActionListener(e -> deleteTask());
        saveBtn.addActionListener(e -> saveTasksToFile());
        loadBtn.addActionListener(e -> loadTasksFromFile());
        filterTodayBtn.addActionListener(e -> filterTasksToday());
        filterHighPriorityBtn.addActionListener(e -> filterTasksHighPriority());
        showAllBtn.addActionListener(e -> showAllTasks());

        initializeReminderTimer();
        refreshTaskTable();
    }

    private void addTaskDialog() {
        TaskDialog dialog = new TaskDialog(this, "Add Task", null);
        dialog.setVisible(true);
        Task newTask = dialog.getTask();
        if (newTask != null) {
            taskList.add(newTask);
            updateQueueFromList();
            refreshTaskTable();
        }
    }

    private void editTaskDialog() {
        int selected = taskTable.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Select a task to edit.");
            return;
        }
        Task task = getTaskAtTableRow(selected);
        TaskDialog dialog = new TaskDialog(this, "Edit Task", task);
        dialog.setVisible(true);
        Task editedTask = dialog.getTask();
        if (editedTask != null) {
            taskList.set(taskList.indexOf(task), editedTask);
            updateQueueFromList();
            refreshTaskTable();
        }
    }

    private void deleteTask() {
        int selected = taskTable.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Select a task to delete.");
            return;
        }
        Task task = getTaskAtTableRow(selected);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            taskList.remove(task);
            updateQueueFromList();
            refreshTaskTable();
        }
    }

    private void saveTasksToFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(taskList);
                JOptionPane.showMessageDialog(this, "Tasks saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving tasks: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTasksFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof List<?>) {
                    taskList = (List<Task>) obj;
                    updateQueueFromList();
                    refreshTaskTable();
                    JOptionPane.showMessageDialog(this, "Tasks loaded successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "File does not contain valid task data.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
            }
        }
    }

    private void filterTasksToday() {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date startOfDay = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endOfDay = cal.getTime();

        List<Task> filtered = new ArrayList<>();
        for (Task t : taskList) {
            if (!t.getDeadline().before(startOfDay) && t.getDeadline().before(endOfDay)) {
                filtered.add(t);
            }
        }
        refreshTableWithList(filtered);
    }

    private void filterTasksHighPriority() {
        List<Task> filtered = new ArrayList<>();
        for (Task t : taskList) {
            if (t.getPriority() == 1) {
                filtered.add(t);
            }
        }
        refreshTableWithList(filtered);
    }

    private void showAllTasks() {
        refreshTaskTable();
    }

    private void refreshTaskTable() {
        refreshTableWithList(taskList);
    }

    private void refreshTableWithList(List<Task> list) {
        tableModel.setRowCount(0);
        Collections.sort(list);
        for (Task t : list) {
            tableModel.addRow(new Object[] { t.getTitle(), t.getPriority(), dateFormat.format(t.getDeadline()) });
        }
    }

    private Task getTaskAtTableRow(int row) {
        String title = (String) tableModel.getValueAt(row, 0);
        String priorityStr = tableModel.getValueAt(row, 1).toString();
        String deadlineStr = (String) tableModel.getValueAt(row, 2);
        int priority = Integer.parseInt(priorityStr);
        try {
            Date deadline = dateFormat.parse(deadlineStr);
            for (Task t : taskList) {
                if (t.getTitle().equals(title) && t.getPriority() == priority && t.getDeadline().equals(deadline)) {
                    return t;
                }
            }
        } catch (ParseException ignored) {}
        return null;
    }

    private void updateQueueFromList() {
        taskQueue.clear();
        taskQueue.addAll(taskList);
    }

    private void initializeReminderTimer() {
        reminderTimer = new Timer(true);
        reminderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkReminders();
            }
        }, 0, 60000); // check every 60 sec
    }

    private void checkReminders() {
        Date now = new Date();
        for (Task t : taskList) {
            long diff = t.getDeadline().getTime() - now.getTime();
            // Remind if deadline is within next 5 minutes
            if (diff > 0 && diff <= 5 * 60 * 1000) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Reminder: Task \"" + t.getTitle() + "\" is due soon!",
                        "Task Reminder", JOptionPane.WARNING_MESSAGE));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SmartTaskScheduler().setVisible(true);
        });
    }
}

class TaskDialog extends JDialog {
    private JTextField txtTitle;
    private JComboBox<Integer> cmbPriority;
    private JTextField txtDeadline;
    private JButton btnOk, btnCancel;
    private Task task;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public TaskDialog(Frame owner, String title, Task task) {
        super(owner, title, true);
        this.task = task;
        setSize(350, 200);
        setLocationRelativeTo(owner);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Title:"), gbc);
        gbc.gridy++;
        add(new JLabel("Priority (1-5):"), gbc);
        gbc.gridy++;
        add(new JLabel("Deadline (yyyy-MM-dd HH:mm):"), gbc);

        txtTitle = new JTextField(20);
        cmbPriority = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        txtDeadline = new JTextField(20);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1; gbc.gridy = 0;
        add(txtTitle, gbc);
        gbc.gridy++;
        add(cmbPriority, gbc);
        gbc.gridy++;
        add(txtDeadline, gbc);

        btnOk = new JButton("OK");
        btnCancel = new JButton("Cancel");

        JPanel panelButtons = new JPanel();
        panelButtons.add(btnOk);
        panelButtons.add(btnCancel);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy++;
        add(panelButtons, gbc);

        if (task != null) {
            txtTitle.setText(task.getTitle());
            cmbPriority.setSelectedItem(task.getPriority());
            txtDeadline.setText(dateFormat.format(task.getDeadline()));
        }

        btnOk.addActionListener(e -> onOk());
        btnCancel.addActionListener(e -> onCancel());
    }

    private void onOk() {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.");
            return;
        }
        int priority = (Integer) cmbPriority.getSelectedItem();

        Date deadline;
        try {
            deadline = dateFormat.parse(txtDeadline.getText().trim());
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Deadline format invalid. Correct format: yyyy-MM-dd HH:mm");
            return;
        }

        task = new Task(title, priority, deadline);
        dispose();
    }

    private void onCancel() {
        task = null;
        dispose();
    }

    public Task getTask() {
        return task;
    }
}

