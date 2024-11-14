import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public class TechSupportAISGUI extends JFrame {
    private DatabaseManager dbManager;
    private JTextArea outputArea;
    private JTextField clientNameField;
    private JTextField titleField;
    private JTextField descriptionField;
    private JTextField searchField;
    private JComboBox<String> statusCombo;
    private JComboBox<String> priorityCombo;
    private JTable requestsTable;
    private DefaultTableModel tableModel;

    public TechSupportAISGUI() {
        this.initializeUI();
        this.dbManager = new DatabaseManager(this.outputArea);
        this.loadRequests();
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                TechSupportAISGUI.this.dbManager.closeConnection();
                System.exit(0);
            }
        });
    }

    private void initializeUI() {
        this.setTitle("Tech Support AIS");
        this.setSize(1200, 700);
        this.setDefaultCloseOperation(3);
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel addPanel = new JPanel(new FlowLayout(0));
        addPanel.setBorder(BorderFactory.createTitledBorder("Добавить заявку"));
        this.clientNameField = new JTextField(15);
        this.titleField = new JTextField(15);
        this.descriptionField = new JTextField(20);
        this.statusCombo = new JComboBox(new String[]{"Новая", "В работе", "Ожидает ответа", "Закрыта"});
        this.priorityCombo = new JComboBox(new String[]{"Низкий", "Средний", "Высокий", "Критический"});
        JButton addButton = new JButton("Добавить");
        addButton.addActionListener((e) -> {
            this.addRequest();
        });
        addPanel.add(new JLabel("Имя клиента:"));
        addPanel.add(this.clientNameField);
        addPanel.add(new JLabel("Тема:"));
        addPanel.add(this.titleField);
        addPanel.add(new JLabel("Описание:"));
        addPanel.add(this.descriptionField);
        addPanel.add(new JLabel("Статус:"));
        addPanel.add(this.statusCombo);
        addPanel.add(new JLabel("Приоритет:"));
        addPanel.add(this.priorityCombo);
        addPanel.add(addButton);
        mainPanel.add(addPanel, "North");
        JPanel actionPanel = new JPanel(new FlowLayout(0));
        this.searchField = new JTextField(20);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener((e) -> {
            this.searchRequests();
        });
        JButton updateStatusButton = new JButton("Обновить статус");
        updateStatusButton.addActionListener((e) -> {
            this.showUpdateStatusDialog();
        });
        JButton writeToFileButton = new JButton("Записать в файл");
        writeToFileButton.addActionListener((e) -> {
            this.dbManager.writeResultsToFile("output.txt");
        });
        actionPanel.add(new JLabel("Поиск заявок:"));
        actionPanel.add(this.searchField);
        actionPanel.add(searchButton);
        actionPanel.add(updateStatusButton);
        actionPanel.add(writeToFileButton);
        mainPanel.add(actionPanel, "Center");
        String[] columnNames = new String[]{"ID", "Клиент", "Тема", "Описание", "Статус", "Приоритет", "Создано", "Обновлено", "Действия"};
        this.tableModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == 8;
            }
        };
        this.requestsTable = new JTable(this.tableModel);
        this.requestsTable.setAutoResizeMode(0);
        this.requestsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        this.requestsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        this.requestsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        this.requestsTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        this.requestsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        this.requestsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        this.requestsTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        this.requestsTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        this.requestsTable.getColumnModel().getColumn(8).setPreferredWidth(80);
        this.requestsTable.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());
        this.requestsTable.getColumnModel().getColumn(8).setCellEditor(new ButtonEditor(new JCheckBox()));
        JScrollPane scrollPane = new JScrollPane(this.requestsTable);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        mainPanel.add(scrollPane, "South");
        this.outputArea = new JTextArea(5, 30);
        this.outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(this.outputArea);
        mainPanel.add(outputScrollPane, "East");
        this.add(mainPanel);
        this.setLocationRelativeTo((Component)null);
    }

    private void addRequest() {
        String clientName = this.clientNameField.getText().trim();
        String title = this.titleField.getText().trim();
        String description = this.descriptionField.getText().trim();
        String status = (String)this.statusCombo.getSelectedItem();
        String priority = (String)this.priorityCombo.getSelectedItem();
        if (!clientName.isEmpty() && !title.isEmpty() && !description.isEmpty()) {
            int clientId = this.dbManager.addNewClient(clientName);
            if (clientId != -1) {
                this.dbManager.addNewRequest(clientId, title, description, status, priority);
                this.loadRequests();
                this.clearInputFields();
            }

        } else {
            JOptionPane.showMessageDialog(this, "Пожалуйста, заполните все поля", "Ошибка", 0);
        }
    }

    private void searchRequests() {
        String keyword = this.searchField.getText().trim();
        this.loadRequests(this.dbManager.searchRequestsByKeyword(keyword));
    }

    private void loadRequests() {
        this.loadRequests(this.dbManager.getAllRequests());
    }

    private void loadRequests(List<Request> requests) {
        this.tableModel.setRowCount(0);
        Iterator var2 = requests.iterator();

        while(var2.hasNext()) {
            Request request = (Request)var2.next();
            Object[] row = new Object[]{request.getId(), request.getClientName(), request.getTitle(), request.getDescription(), request.getStatus(), request.getPriority(), request.getCreationDate(), request.getUpdateDate(), "Удалить"};
            this.tableModel.addRow(row);
        }

    }

    private void clearInputFields() {
        this.clientNameField.setText("");
        this.titleField.setText("");
        this.descriptionField.setText("");
        this.statusCombo.setSelectedIndex(0);
        this.priorityCombo.setSelectedIndex(0);
    }

    private void showUpdateStatusDialog() {
        JTextField requestIdField = new JTextField(10);
        JComboBox<String> newStatusCombo = new JComboBox(new String[]{"Новая", "В работе", "Ожидает ответа", "Закрыта"});
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("ID заявки:"));
        panel.add(requestIdField);
        panel.add(new JLabel("Новый статус:"));
        panel.add(newStatusCombo);
        int result = JOptionPane.showConfirmDialog((Component)null, panel, "Обновить статус заявки", 2, -1);
        if (result == 0) {
            try {
                int requestId = Integer.parseInt(requestIdField.getText().trim());
                String newStatus = (String)newStatusCombo.getSelectedItem();
                this.dbManager.updateRequestStatus(requestId, newStatus);
                this.loadRequests();
            } catch (NumberFormatException var7) {
                JOptionPane.showMessageDialog(this, "ID заявки должен быть числом", "Ошибка", 0);
            }
        }

    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            TechSupportAISGUI app = new TechSupportAISGUI();
            app.setVisible(true);
        });
    }

    static class DatabaseManager {
        private Connection conn;
        private static final String DB_URL = "jdbc:sqlite:techsupport.db";
        private final JTextArea outputArea;

        public DatabaseManager(JTextArea outputArea) {
            this.outputArea = outputArea;

            try {
                this.initializeDatabase();
            } catch (SQLException var5) {
                outputArea.append("Ошибка при инициализации БД: " + var5.getMessage() + "\n");

                try {
                    this.resetDatabase();
                } catch (SQLException var4) {
                    outputArea.append("Ошибка при сбросе базы данных: " + var4.getMessage() + "\n");
                }
            }

        }

        private void resetDatabase() throws SQLException {
            this.closeConnection();
            File dbFile = new File("techsupport.db");
            if (dbFile.exists() && !dbFile.delete()) {
                throw new SQLException("Не удалось удалить файл базы данных");
            } else {
                this.initializeDatabase();
            }
        }

        private void initializeDatabase() throws SQLException {
            this.conn = DriverManager.getConnection("jdbc:sqlite:techsupport.db");
            this.createTables();
            Statement stmt = this.conn.createStatement();

            try {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM requests");

                try {
                    if (rs.next() && rs.getInt(1) == 0) {
                        this.insertInitialData();
                    }
                } catch (Throwable var7) {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (rs != null) {
                    rs.close();
                }
            } catch (Throwable var8) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Throwable var5) {
                        var8.addSuppressed(var5);
                    }
                }

                throw var8;
            }

            if (stmt != null) {
                stmt.close();
            }

        }

        private void createTables() throws SQLException {
            Statement stmt = this.conn.createStatement();

            try {
                stmt.execute("    CREATE TABLE IF NOT EXISTS clients (\n        id INTEGER PRIMARY KEY AUTOINCREMENT,\n        name TEXT NOT NULL UNIQUE\n    )\n");
                stmt.execute("    CREATE TABLE IF NOT EXISTS requests (\n        id INTEGER PRIMARY KEY AUTOINCREMENT,\n        client_id INTEGER NOT NULL,\n        title TEXT NOT NULL,\n        description TEXT NOT NULL,\n        status TEXT NOT NULL,\n        priority TEXT NOT NULL,\n        creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n        update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n        FOREIGN KEY (client_id) REFERENCES clients(id),\n        UNIQUE(client_id, title, description)\n    )\n");
            } catch (Throwable var5) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (stmt != null) {
                stmt.close();
            }

        }

        private void insertInitialData() throws SQLException {
            Statement stmt = this.conn.createStatement();

            try {
                stmt.execute("    INSERT OR IGNORE INTO clients (id, name) VALUES\n    (1, 'Иванов Иван'),\n    (2, 'Петров Петр'),\n    (3, 'Сидорова Анна'),\n    (4, 'Козлова Елена'),\n    (5, 'Смирнов Алексей')\n");
                stmt.execute("    INSERT OR IGNORE INTO requests (client_id, title, description, status, priority) VALUES\n    (1, 'Настройка принтера', 'Не печатает цветные документы', 'Новая', 'Средний'),\n    (2, 'Замена монитора', 'Требуется замена старого монитора', 'В работе', 'Низкий'),\n    (3, 'Обновление Windows', 'Установка последних обновлений', 'Закрыта', 'Высокий'),\n    (4, 'Настройка почты', 'Проблемы с отправкой писем', 'В работе', 'Средний'),\n    (5, 'Замена клавиатуры', 'Не работают некоторые клавиши', 'Новая', 'Низкий'),\n    (1, 'Проблемы с интернетом', 'Медленное соединение', 'В работе', 'Высокий'),\n    (2, 'Установка 1С', 'Требуется установка новой версии', 'Новая', 'Критический'),\n    (3, 'Настройка VPN', 'Подключение к корпоративной сети', 'Закрыта', 'Средний'),\n    (4, 'Вирусная угроза', 'Обнаружен вирус на компьютере', 'В работе', 'Критический'),\n    (5, 'Backup данных', 'Создание резервной копии', 'Ожидает ответа', 'Высокий'),\n    (1, 'Установка Office', 'Требуется Microsoft Office 2021', 'Новая', 'Средний'),\n    (2, 'Проблема со сканером', 'Не сканирует документы', 'В работе', 'Низкий'),\n    (3, 'Замена батареи UPS', 'Требуется замена аккумулятора', 'Закрыта', 'Средний'),\n    (4, 'Настройка телефонии', 'Настройка IP-телефона', 'В работе', 'Высокий'),\n    (5, 'Проблема с принтером', 'Замятие бумаги', 'Новая', 'Низкий'),\n    (1, 'Сброс пароля', 'Забыт пароль от учетной записи', 'Закрыта', 'Средний'),\n    (2, 'Медленная работа ПК', 'Компьютер тормозит', 'В работе', 'Высокий'),\n    (3, 'Установка антивируса', 'Требуется установка ESET NOD32', 'Новая', 'Средний'),\n    (4, 'Подключение принтера', 'Настройка сетевого принтера', 'Ожидает ответа', 'Низкий'),\n    (5, 'Обновление драйверов', 'Требуется обновление драйверов', 'В работе', 'Средний')\n");
            } catch (Throwable var5) {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (stmt != null) {
                stmt.close();
            }

        }

        public List<Request> getAllRequests() {
            List<Request> requests = new ArrayList();

            try {
                Statement stmt = this.conn.createStatement();

                try {
                    ResultSet rs = stmt.executeQuery("    SELECT r.id, c.name as client_name, r.title, r.description,\n           r.status, r.priority, r.creation_date, r.update_date\n    FROM requests r\n    JOIN clients c ON r.client_id = c.id\n    ORDER BY r.id ASC\n");

                    try {
                        while(rs.next()) {
                            requests.add(new Request(rs.getInt("id"), rs.getString("client_name"), rs.getString("title"), rs.getString("description"), rs.getString("status"), rs.getString("priority"), rs.getString("creation_date"), rs.getString("update_date")));
                        }
                    } catch (Throwable var8) {
                        if (rs != null) {
                            try {
                                rs.close();
                            } catch (Throwable var7) {
                                var8.addSuppressed(var7);
                            }
                        }

                        throw var8;
                    }

                    if (rs != null) {
                        rs.close();
                    }
                } catch (Throwable var9) {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable var6) {
                            var9.addSuppressed(var6);
                        }
                    }

                    throw var9;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var10) {
                this.outputArea.append("Ошибка при получении заявок: " + var10.getMessage() + "\n");
            }

            return requests;
        }

        public void deleteRequest(int id) {
            try {
                this.conn.setAutoCommit(false);
                PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM requests WHERE id = ?");

                try {
                    pstmt.setInt(1, id);
                    int deleted = pstmt.executeUpdate();
                    if (deleted > 0) {
                        this.conn.commit();
                        this.outputArea.append("Заявка успешно удалена.\n");
                    } else {
                        this.outputArea.append("Заявка с указанным ID не найдена.\n");
                    }
                } catch (Throwable var18) {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        } catch (Throwable var17) {
                            var18.addSuppressed(var17);
                        }
                    }

                    throw var18;
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException var19) {
                try {
                    this.conn.rollback();
                } catch (SQLException var16) {
                    this.outputArea.append("Ошибка при откате транзакции: " + var16.getMessage() + "\n");
                }

                this.outputArea.append("Ошибка при удалении заявки: " + var19.getMessage() + "\n");
            } finally {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException var15) {
                    this.outputArea.append("Ошибка при восстановлении автоматических транзакций: " + var15.getMessage() + "\n");
                }

            }

        }

        public int addNewClient(String name) {
            try {
                PreparedStatement pstmt = this.conn.prepareStatement("SELECT id FROM clients WHERE name = ?");

                ResultSet rs;
                int var4;
                label78: {
                    try {
                        pstmt.setString(1, name);
                        rs = pstmt.executeQuery();
                        if (!rs.next()) {
                            break label78;
                        }

                        var4 = rs.getInt("id");
                    } catch (Throwable var7) {
                        if (pstmt != null) {
                            try {
                                pstmt.close();
                            } catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }

                        throw var7;
                    }

                    if (pstmt != null) {
                        pstmt.close();
                    }

                    return var4;
                }

                if (pstmt != null) {
                    pstmt.close();
                }

                pstmt = this.conn.prepareStatement("INSERT INTO clients (name) VALUES (?)", 1);

                label84: {
                    try {
                        pstmt.setString(1, name);
                        pstmt.executeUpdate();
                        rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            var4 = rs.getInt(1);
                            break label84;
                        }
                    } catch (Throwable var8) {
                        if (pstmt != null) {
                            try {
                                pstmt.close();
                            } catch (Throwable var5) {
                                var8.addSuppressed(var5);
                            }
                        }

                        throw var8;
                    }

                    if (pstmt != null) {
                        pstmt.close();
                    }

                    return -1;
                }

                if (pstmt != null) {
                    pstmt.close();
                }

                return var4;
            } catch (SQLException var9) {
                this.outputArea.append("Ошибка при добавлении клиента: " + var9.getMessage() + "\n");
                return -1;
            }
        }

        public void addNewRequest(int clientId, String title, String description, String status, String priority) {
            try {
                PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO requests (client_id, title, description, status, priority) VALUES (?, ?, ?, ?, ?)");

                try {
                    pstmt.setInt(1, clientId);
                    pstmt.setString(2, title);
                    pstmt.setString(3, description);
                    pstmt.setString(4, status);
                    pstmt.setString(5, priority);
                    pstmt.executeUpdate();
                    this.outputArea.append("Заявка успешно добавлена.\n");
                } catch (Throwable var10) {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        } catch (Throwable var9) {
                            var10.addSuppressed(var9);
                        }
                    }

                    throw var10;
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLiteException var11) {
                if (var11.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT) {
                    this.outputArea.append("Такая заявка уже существует.\n");
                } else {
                    this.outputArea.append("Ошибка при добавлении заявки: " + var11.getMessage() + "\n");
                }
            } catch (SQLException var12) {
                this.outputArea.append("Ошибка при добавлении заявки: " + var12.getMessage() + "\n");
            }

        }

        public void updateRequestStatus(int requestId, String newStatus) {
            try {
                PreparedStatement pstmt = this.conn.prepareStatement("UPDATE requests SET status = ?, update_date = CURRENT_TIMESTAMP WHERE id = ?");

                try {
                    pstmt.setString(1, newStatus);
                    pstmt.setInt(2, requestId);
                    int updated = pstmt.executeUpdate();
                    if (updated > 0) {
                        this.outputArea.append("Статус заявки успешно обновлен.\n");
                    } else {
                        this.outputArea.append("Заявка с указанным ID не найдена.\n");
                    }
                } catch (Throwable var7) {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException var8) {
                this.outputArea.append("Ошибка при обновлении статуса: " + var8.getMessage() + "\n");
            }

        }

        public List<Request> searchRequestsByKeyword(String keyword) {
            List<Request> requests = new ArrayList();

            try {
                PreparedStatement pstmt = this.conn.prepareStatement("    SELECT r.id, c.name as client_name, r.title, r.description,\n           r.status, r.priority, r.creation_date, r.update_date\n    FROM requests r\n    JOIN clients c ON r.client_id = c.id\n    WHERE r.title LIKE ? OR r.description LIKE ? OR\n          c.name LIKE ? OR CAST(r.id AS TEXT) = ?\n    ORDER BY r.id ASC\n");

                try {
                    String searchPattern = "%" + keyword + "%";
                    pstmt.setString(1, searchPattern);
                    pstmt.setString(2, searchPattern);
                    pstmt.setString(3, searchPattern);
                    pstmt.setString(4, keyword);
                    ResultSet rs = pstmt.executeQuery();

                    while(rs.next()) {
                        requests.add(new Request(rs.getInt("id"), rs.getString("client_name"), rs.getString("title"), rs.getString("description"), rs.getString("status"), rs.getString("priority"), rs.getString("creation_date"), rs.getString("update_date")));
                    }
                } catch (Throwable var7) {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException var8) {
                this.outputArea.append("Ошибка при поиске заявок: " + var8.getMessage() + "\n");
            }

            return requests;
        }

        public void writeResultsToFile(String filename) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

                try {
                    Statement stmt = this.conn.createStatement();

                    try {
                        ResultSet rs = stmt.executeQuery("    SELECT r.id, c.name as client_name, r.title, r.description,\n           r.status, r.priority, r.creation_date, r.update_date\n    FROM requests r\n    JOIN clients c ON r.client_id = c.id\n    ORDER BY r.id ASC\n");

                        try {
                            writer.write("Отчет по заявкам\n\n");
                            int count = 0;

                            while(true) {
                                if (!rs.next()) {
                                    writer.write(String.format("\nВсего заявок: %d", count));
                                    this.outputArea.append("Результаты успешно записаны в файл " + filename + "\n");
                                    break;
                                }

                                ++count;
                                writer.write(String.format("ID: %d\nКлиент: %s\nТема: %s\nОписание: %s\nСтатус: %s\nПриоритет: %s\nСоздано: %s\nОбновлено: %s\n\n", rs.getInt("id"), rs.getString("client_name"), rs.getString("title"), rs.getString("description"), rs.getString("status"), rs.getString("priority"), rs.getString("creation_date"), rs.getString("update_date")));
                            }
                        } catch (Throwable var10) {
                            if (rs != null) {
                                try {
                                    rs.close();
                                } catch (Throwable var9) {
                                    var10.addSuppressed(var9);
                                }
                            }

                            throw var10;
                        }

                        if (rs != null) {
                            rs.close();
                        }
                    } catch (Throwable var11) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var8) {
                                var11.addSuppressed(var8);
                            }
                        }

                        throw var11;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (Throwable var12) {
                    try {
                        writer.close();
                    } catch (Throwable var7) {
                        var12.addSuppressed(var7);
                    }

                    throw var12;
                }

                writer.close();
            } catch (IOException | SQLException var13) {
                this.outputArea.append("Ошибка при записи в файл: " + var13.getMessage() + "\n");
            }

        }

        public void closeConnection() {
            if (this.conn != null) {
                try {
                    this.conn.close();
                    this.outputArea.append("Соединение с базой данных закрыто.\n");
                } catch (SQLException var2) {
                    this.outputArea.append("Ошибка при закрытии соединения с БД: " + var2.getMessage() + "\n");
                }
            }

        }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            this.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.setText(value == null ? "" : value.toString());
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button = new JButton();
        private String label;
        private boolean isPushed;
        private int targetRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            this.button.setOpaque(true);
            this.button.addActionListener((e) -> {
                this.fireEditingStopped();
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.label = value == null ? "" : value.toString();
            this.button.setText(this.label);
            this.targetRow = row;
            this.isPushed = true;
            return this.button;
        }

        public Object getCellEditorValue() {
            if (this.isPushed && "Удалить".equals(this.label)) {
                int id = (Integer)TechSupportAISGUI.this.tableModel.getValueAt(this.targetRow, 0);
                int confirm = JOptionPane.showConfirmDialog((Component)null, "Вы уверены, что хотите удалить эту заявку?", "Подтверждение удаления", 0);
                if (confirm == 0) {
                    TechSupportAISGUI.this.dbManager.deleteRequest(id);
                    TechSupportAISGUI.this.loadRequests();
                }
            }

            this.isPushed = false;
            return this.label;
        }

        public boolean stopCellEditing() {
            this.isPushed = false;
            return super.stopCellEditing();
        }
    }

    public static class Request {
        private final int id;
        private final String clientName;
        private final String title;
        private final String description;
        private final String status;
        private final String priority;
        private final String creationDate;
        private final String updateDate;

        public Request(int id, String clientName, String title, String description, String status, String priority, String creationDate, String updateDate) {
            this.id = id;
            this.clientName = clientName;
            this.title = title;
            this.description = description;
            this.status = status;
            this.priority = priority;
            this.creationDate = creationDate;
            this.updateDate = updateDate;
        }

        public int getId() {
            return this.id;
        }

        public String getClientName() {
            return this.clientName;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }

        public String getStatus() {
            return this.status;
        }

        public String getPriority() {
            return this.priority;
        }

        public String getCreationDate() {
            return this.creationDate;
        }

        public String getUpdateDate() {
            return this.updateDate;
        }
    }
}