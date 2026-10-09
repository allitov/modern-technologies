package io.allitov.todo.view;

import io.allitov.todo.model.Project;
import io.allitov.todo.model.Task;
import io.allitov.todo.model.TaskPriority;
import io.allitov.todo.model.TaskStatus;
import io.allitov.todo.service.TaskManager;
import io.allitov.todo.service.TaskManagerImpl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * Главное окно менеджера задач.
 */
public class MainWindowImpl implements MainWindow {

    private static final String ALL_STATUSES = "All statuses";

    private static final String ALL_PRIORITIES = "All priorities";

    private final TaskManager manager = new TaskManagerImpl();

    private final JFrame window = new JFrame("Task Manager");

    private final JPanel contentPane = new JPanel(null);

    private final JLabel projectLabel = new JLabel("Projects");

    private final JLabel tasksLabel = new JLabel("Tasks");

    private final JLabel titleLabel = new JLabel("Title");

    private final JLabel descriptionLabel = new JLabel("Description");

    private final JLabel searchLabel = new JLabel("Search");

    private final JLabel statusChoiceLabel = new JLabel("Status");

    private final JLabel priorityChoiceLabel = new JLabel("Priority");

    private final JLabel statusFilterLabel = new JLabel("Status filter");

    private final JLabel priorityFilterLabel = new JLabel("Priority filter");

    private final DefaultListModel<String> projectListModel = new DefaultListModel<>();

    private final DefaultListModel<String> taskListModel = new DefaultListModel<>();

    private final JList<String> projectList = new JList<>(projectListModel);

    private final JList<String> taskList = new JList<>(taskListModel);

    private final JTextField titleInput = new JTextField();

    private final JTextArea descriptionInput = new JTextArea();

    private final JTextField searchInput = new JTextField();

    private final JComboBox<TaskStatus> statusChoice = new JComboBox<>(TaskStatus.values());

    private final JComboBox<TaskPriority> priorityChoice = new JComboBox<>(TaskPriority.values());

    private final JComboBox<String> statusFilter = new JComboBox<>(new DefaultComboBoxModel<>(
            new String[] {ALL_STATUSES,
                    TaskStatus.TODO.getDisplayName(),
                    TaskStatus.IN_PROGRESS.getDisplayName(),
                    TaskStatus.DONE.getDisplayName()}));

    private final JComboBox<String> priorityFilter = new JComboBox<>(new DefaultComboBoxModel<>(
            new String[] {ALL_PRIORITIES,
                    TaskPriority.LOW.getDisplayName(),
                    TaskPriority.MEDIUM.getDisplayName(),
                    TaskPriority.HIGH.getDisplayName()}));

    private final JButton addTaskButton = new JButton("Add");

    private final JButton updateTaskButton = new JButton("Update");

    private final JButton deleteTaskButton = new JButton("Delete");

    private final JButton clearButton = new JButton("Clear");

    private final JButton addProjectButton = new JButton("Add project");

    private final JButton deleteProjectButton = new JButton("Delete project");

    private final JLabel statistics = new JLabel("", SwingConstants.CENTER);

    private boolean updatingProjects;

    private boolean updatingTasks;

    /**
     * Создает и настраивает элементы главного окна.
     */
    public MainWindowImpl() {
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setSize(1050, 700);
        window.setContentPane(contentPane);
        window.setResizable(true);

        projectLabel.setBounds(20, 30, 210, 25);
        tasksLabel.setBounds(250, 30, 780, 25);
        titleLabel.setBounds(250, 375, 280, 25);
        descriptionLabel.setBounds(545, 375, 485, 25);
        searchLabel.setBounds(250, 445, 250, 25);
        statusChoiceLabel.setBounds(515, 445, 145, 25);
        priorityChoiceLabel.setBounds(675, 445, 145, 25);
        statusFilterLabel.setBounds(250, 515, 180, 25);
        priorityFilterLabel.setBounds(445, 515, 180, 25);

        statusChoice.setSelectedIndex(0);
        priorityChoice.setSelectedIndex(1);
        statusFilter.setSelectedIndex(0);
        priorityFilter.setSelectedIndex(0);

        statistics.setFont(statistics.getFont().deriveFont(Font.BOLD));

        placeComponents();
        configureLists();
        configureTaskFields();
        configureFilters();
        configureButtons();

        refreshProjects();
        refreshTasks();
    }

    public void show() {
        window.setVisible(true);
    }

    /**
     * Размещает компоненты окна по координатам исходного интерфейса.
     */
    private void placeComponents() {
        addLabel(projectLabel);
        addLabel(tasksLabel);
        addLabel(titleLabel);
        addLabel(descriptionLabel);
        addLabel(searchLabel);
        addLabel(statusChoiceLabel);
        addLabel(priorityChoiceLabel);
        addLabel(statusFilterLabel);
        addLabel(priorityFilterLabel);

        JScrollPane projectScrollPane = new JScrollPane(projectList);
        projectScrollPane.setBounds(20, 60, 210, 390);
        contentPane.add(projectScrollPane);

        JScrollPane taskScrollPane = new JScrollPane(taskList);
        taskScrollPane.setBounds(250, 60, 780, 300);
        contentPane.add(taskScrollPane);

        titleInput.setBounds(250, 400, 280, 32);
        contentPane.add(titleInput);

        JScrollPane descriptionScrollPane = new JScrollPane(descriptionInput);
        descriptionScrollPane.setBounds(545, 400, 485, 32);
        contentPane.add(descriptionScrollPane);

        searchInput.setBounds(250, 470, 250, 32);
        contentPane.add(searchInput);

        statusChoice.setBounds(515, 470, 145, 32);
        priorityChoice.setBounds(675, 470, 145, 32);
        statusFilter.setBounds(250, 540, 180, 32);
        priorityFilter.setBounds(445, 540, 180, 32);
        contentPane.add(statusChoice);
        contentPane.add(priorityChoice);
        contentPane.add(statusFilter);
        contentPane.add(priorityFilter);

        addTaskButton.setBounds(840, 465, 140, 32);
        updateTaskButton.setBounds(840, 505, 140, 32);
        deleteTaskButton.setBounds(840, 545, 140, 32);
        clearButton.setBounds(650, 540, 160, 32);
        addProjectButton.setBounds(20, 470, 210, 32);
        deleteProjectButton.setBounds(20, 510, 210, 32);
        contentPane.add(addTaskButton);
        contentPane.add(updateTaskButton);
        contentPane.add(deleteTaskButton);
        contentPane.add(clearButton);
        contentPane.add(addProjectButton);
        contentPane.add(deleteProjectButton);

        statistics.setBounds(20, 600, 1010, 40);
        statistics.setBorder(BorderFactory.createEmptyBorder());
        contentPane.add(statistics);
    }

    /**
     * Настраивает списки проектов и задач и их обработчики выбора.
     */
    private void configureLists() {
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingProjects) {
                onProjectSelected();
            }
        });
        taskList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updatingTasks) {
                loadSelectedTask();
            }
        });
    }

    /**
     * Настраивает обработчики изменения полей формы задачи.
     */
    private void configureTaskFields() {
        searchInput.getDocument().addDocumentListener(new SearchDocumentListener());
    }

    /**
     * Настраивает обработчики изменения фильтров задач.
     */
    private void configureFilters() {
        statusFilter.addActionListener(_ -> updateFilter());
        priorityFilter.addActionListener(_ -> updateFilter());
    }

    /**
     * Настраивает обработчики нажатия кнопок интерфейса.
     */
    private void configureButtons() {
        addTaskButton.addActionListener(_ -> addTaskFromForm());
        updateTaskButton.addActionListener(_ -> updateTaskFromForm());
        deleteTaskButton.addActionListener(_ -> deleteSelectedTask());
        clearButton.addActionListener(_ -> clearTaskFields());
        addProjectButton.addActionListener(_ -> addProjectFromDialog());
        deleteProjectButton.addActionListener(_ -> deleteSelectedProject());
    }

    /**
     * Добавляет текстовую метку на форму с выравниванием по левому краю.
     *
     * @param label добавляемая метка.
     */
    private void addLabel(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.LEFT);
        contentPane.add(label);
    }

    /**
     * Обновляет список проектов и выбирает первый проект, если он есть.
     */
    private void refreshProjects() {
        updatingProjects = true;
        projectListModel.clear();

        List<Project> projects = manager.getProjects();
        for (Project project : projects) {
            projectListModel.addElement(project.getName());
        }

        if (!projects.isEmpty()) {
            projectList.setSelectedIndex(0);
        }
        updatingProjects = false;
    }

    /**
     * Обновляет список задач с учетом текущих фильтров и статистику.
     */
    private void refreshTasks() {
        updatingTasks = true;
        taskListModel.clear();

        List<Task> tasks = manager.getTasks();
        String search = searchInput.getText();
        String selectedStatus = choiceText(statusFilter);
        String selectedPriority = choiceText(priorityFilter);
        int shown = 0;

        for (Task task : tasks) {
            String title = task.getTitle();
            TaskStatus status = task.getStatus();
            TaskPriority priority = task.getPriority();

            boolean matchesSearch = search.isEmpty() || title.contains(search);
            boolean matchesStatus = ALL_STATUSES.equals(selectedStatus)
                    || status.getDisplayName().equals(selectedStatus);
            boolean matchesPriority = ALL_PRIORITIES.equals(selectedPriority)
                    || priority.getDisplayName().equals(selectedPriority);

            if (matchesSearch && matchesStatus && matchesPriority) {
                taskListModel.addElement("%d | %s | %s | %s".formatted(
                        task.getId(), title, priority.getDisplayName(), status.getDisplayName()));
                shown++;
            }
        }

        statistics.setText("Tasks: %d    Shown: %d    Projects: %d".formatted(
                tasks.size(), shown, manager.getProjects().size()));
        updatingTasks = false;
    }

    /**
     * Определяет индекс выбранной задачи в общем хранилище.
     *
     * @return индекс задачи или {@code -1}, если задача не выбрана.
     */
    private int selectedTaskIndex() {
        if (taskList.getSelectedIndex() < 0) {
            return -1;
        }

        String selected = taskList.getSelectedValue();
        List<Task> tasks = manager.getTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (selected.startsWith(tasks.get(i).getId() + " | ")) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Загружает данные выбранной задачи в форму редактирования.
     */
    private void loadSelectedTask() {
        int index = selectedTaskIndex();
        if (index < 0) {
            return;
        }

        Task task = manager.getTasks().get(index);
        titleInput.setText(task.getTitle());
        descriptionInput.setText(task.getDescription());
        statusChoice.setSelectedItem(task.getStatus());
        priorityChoice.setSelectedItem(task.getPriority());
    }

    /**
     * Очищает форму задачи и снимает выбор в списке задач.
     */
    private void clearTaskFields() {
        titleInput.setText("");
        descriptionInput.setText("");
        statusChoice.setSelectedIndex(0);
        priorityChoice.setSelectedIndex(1);
        taskList.clearSelection();
    }

    /**
     * Добавляет задачу по данным формы после проверки названия.
     */
    private void addTaskFromForm() {
        if (titleInput.getText().isEmpty()) {
            JOptionPane.showMessageDialog(window, "Enter a task title.");
            return;
        }

        manager.addTask(titleInput.getText(), descriptionInput.getText(),
                selectedPriority(), selectedStatus(), selectedProjectId());
        refreshTasks();
        clearTaskFields();
    }

    /**
     * Обновляет выбранную задачу по данным формы.
     */
    private void updateTaskFromForm() {
        int index = selectedTaskIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(window, "Select a task first.");
            return;
        }

        manager.updateTask(index, titleInput.getText(), descriptionInput.getText(),
                selectedPriority(), selectedStatus());
        refreshTasks();
    }

    /**
     * Удаляет выбранную задачу и очищает форму.
     */
    private void deleteSelectedTask() {
        int index = selectedTaskIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(window, "Select a task first.");
            return;
        }

        manager.deleteTask(index);
        refreshTasks();
        clearTaskFields();
    }

    /**
     * Запрашивает название проекта и добавляет новый проект.
     */
    private void addProjectFromDialog() {
        String name = JOptionPane.showInputDialog(window, "Project name:", "New project");
        if (name != null && !name.isEmpty()) {
            manager.addProject(name);
            refreshProjects();
            refreshTasks();
        }
    }

    /**
     * Удаляет выбранный проект и обновляет списки.
     */
    private void deleteSelectedProject() {
        int row = projectList.getSelectedIndex();
        if (row < 0) {
            JOptionPane.showMessageDialog(window, "Select a project first.");
            return;
        }

        manager.deleteProject(row);
        refreshProjects();
        refreshTasks();
    }

    /**
     * Обновляет список задач при изменении фильтров.
     */
    private void updateFilter() {
        refreshTasks();
    }

    /**
     * Обновляет сводку при выборе проекта.
     */
    private void onProjectSelected() {
        int row = projectList.getSelectedIndex();
        if (row >= 0) {
            Project project = manager.getProjects().get(row);
            statistics.setText(String.format("Selected project: %s    Tasks: %d    Done: %.0f%%    Projects: %d",
                    project.getName(),
                    manager.countTasksInProject(project.getId()),
                    manager.completionPercent(project.getId()),
                    manager.getProjects().size()));
        }
    }

    /**
     * Получает идентификатор выбранного проекта.
     *
     * @return идентификатор выбранного проекта или {@code 0}, если проект не выбран.
     */
    private int selectedProjectId() {
        int row = projectList.getSelectedIndex();
        return row < 0 ? 0 : manager.getProjects().get(row).getId();
    }

    /**
     * Получает выбранный текст выпадающего списка.
     *
     * @param choice выпадающий список.
     * @return выбранный текст или пустая строка.
     */
    private String choiceText(JComboBox<String> choice) {
        Object value = choice.getSelectedItem();
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Получает выбранный приоритет задачи.
     *
     * @return выбранный приоритет.
     */
    private TaskPriority selectedPriority() {
        return priorityChoice.getItemAt(priorityChoice.getSelectedIndex());
    }

    /**
     * Получает выбранный статус задачи.
     *
     * @return выбранный статус.
     */
    private TaskStatus selectedStatus() {
        return statusChoice.getItemAt(statusChoice.getSelectedIndex());
    }

    /**
     * Слушатель изменений поисковой строки.
     */
    private final class SearchDocumentListener implements DocumentListener {

        /**
         * Обрабатывает вставку или изменение текста поиска.
         *
         * @param event событие документа.
         */
        @Override
        public void insertUpdate(DocumentEvent event) {
            updateFilter();
        }

        /**
         * Обрабатывает удаление текста поиска.
         *
         * @param event событие документа.
         */
        @Override
        public void removeUpdate(DocumentEvent event) {
            updateFilter();
        }

        /**
         * Обрабатывает замену текста поиска.
         *
         * @param event событие документа.
         */
        @Override
        public void changedUpdate(DocumentEvent event) {
            updateFilter();
        }
    }
}
