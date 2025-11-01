package view;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProcessSimulatorGUI extends JFrame implements ActionListener {
    private ProcessManager processManager;

    // Campos de Procesos
    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JTextField txtProcessSize;
    private JComboBox<String> cmbStatus;

    // Campos de Particiones
    private JTextField txtPartitionName;
    private JTextField txtPartitionSize;

    // Tablas
    private DefaultTableModel processTableModel;
    private JTable processTable;
    private DefaultTableModel partitionTableModel;
    private JTable partitionTable;

    // Panel de resultados
    private JPanel resultsPanel;
    private CardLayout cardLayout;

    private DefaultTableModel[] resultTableModels;
    
    // ← MODIFICADO: Agregar "Compactaciones" al final
    private String[] tableNames = {
            "Inicial", "Listo", "Despachar", "En Ejecución",
            "Expiración de Tiempo", "Espera de E/S", "Bloqueado", 
            "<html>Terminacion de operacion<br>E/S o evento-De Bloqueo a Listo</html>", "Salidas",
            "Particiones", "Finalización de Particiones", "No Ejecutados", 
            "Condensaciones", "Compactaciones"  // ← NUEVO (índice 13)
    };

    // ← MODIFICADO: Agregar Filter.COMPACTACIONES
    private Filter[] filters = {
            Filter.INICIAL, Filter.LISTO, Filter.DESPACHAR, Filter.EN_EJECUCION,
            Filter.TIEMPO_EXPIRADO, Filter.TRANSICION_BLOQUEO, Filter.BLOQUEADO, 
            Filter.DESPERTAR, Filter.FINALIZADO,
            Filter.PARTICIONES, Filter.FINALIZACION_PARTICIONES, Filter.NO_EJECUTADO, 
            Filter.CONDENSACIONES, Filter.COMPACTACIONES  // ← NUEVO (índice 13)
    };

    private String currentAction;
    private NumberFormat numberFormatter;
    
    // ComboBox para filtro de particiones en Listo
    private JComboBox<String> cmbPartitionFilter;

    public ProcessSimulatorGUI() {
        processManager = new ProcessManager();
        numberFormatter = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        setUndecorated(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void initializeComponents() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Campos de Procesos
        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        txtProcessSize = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[] { "No bloqueado", "Bloqueado" });

        setupTimeField(txtProcessTime);
        setupTimeField(txtProcessSize);

        // Campos de Particiones
        txtPartitionName = new JTextField(15);
        txtPartitionSize = new JTextField(15);
        setupTimeField(txtPartitionSize);

        // Tabla de Procesos
        processTableModel = new DefaultTableModel(
                new String[] { "Nombre", "Tiempo", "Estado", "Tamaño", "Partición" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tabla de Particiones
        partitionTableModel = new DefaultTableModel(
                new String[] { "Nombre", "Tamaño" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        partitionTable = new JTable(partitionTableModel);
        partitionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Panel de resultados
        cardLayout = new CardLayout();
        resultsPanel = new JPanel(cardLayout);

        resultTableModels = new DefaultTableModel[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            // Tabla especial para Particiones (índice 9)
            if (i == 9) {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Partición", "Tamaño", "Límite Inicial", "Límite Final", "Procesos Asignados" },  // ← MODIFICADO
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            // Tabla especial para Finalización de Particiones (índice 10)
            else if (i == 10) {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Partición", "Tamaño", "Límite Inicial", "Límite Final", "Procesos Asignados", "Tiempo Total" },  // ← MODIFICADO
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            
            else if (i == 11) {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Proceso", "Tamaño Proceso", "Partición", "Tamaño Partición", "Problema " },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            else if (i == 12) {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Condensación", "Tamaño Total", "Particiones Fusionadas" },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            // ← NUEVO: Tabla para Compactaciones (índice 13)
            else if (i == 13) {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Compactación", "Tamaño", "Proceso", "Partición Creada", "Límite Inicial", "Límite Final", "Razón" },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            else {
                resultTableModels[i] = new DefaultTableModel(
                        new String[] { "Proceso", "Tiempo Restante", "Estado", "Tamaño", "Partición", "Ciclos" },
                        0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }

            JTable table = new JTable(resultTableModels[i]);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(table);
            resultsPanel.add(scrollPane, tableNames[i]);
        }
        
        // ComboBox para filtro de particiones
        cmbPartitionFilter = new JComboBox<>();
        cmbPartitionFilter.addItem("Todas las particiones");
    }

    private void setupTimeField(JTextField textField) {
        textField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                if (Character.isDigit(c)) {
                    SwingUtilities.invokeLater(() -> {
                        formatTimeFieldInRealTime(textField);
                    });
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                formatTimeFieldInRealTime(textField);
            }
        });
    }

    private void formatTimeFieldInRealTime(JTextField textField) {
        String text = textField.getText().replaceAll("[^0-9]", "");
        if (!text.isEmpty()) {
            try {
                String displayText = text;

                if (text.length() > 18) {
                    StringBuilder formatted = new StringBuilder();
                    int count = 0;
                    for (int i = displayText.length() - 1; i >= 0; i--) {
                        if (count > 0 && count % 3 == 0) {
                            formatted.insert(0, ".");
                        }
                        formatted.insert(0, displayText.charAt(i));
                        count++;
                    }
                    displayText = formatted.toString();
                } else {
                    long number = Long.parseLong(text);
                    displayText = numberFormatter.format(number);
                }

                if (!textField.getText().equals(displayText)) {
                    int caretPos = textField.getCaretPosition();
                    textField.setText(displayText);
                    try {
                        int newCaretPos = Math.min(caretPos + (displayText.length() - text.length()),
                                displayText.length());
                        textField.setCaretPosition(newCaretPos);
                    } catch (IllegalArgumentException ex) {
                        textField.setCaretPosition(displayText.length());
                    }
                }
            } catch (NumberFormatException ex) {
                if (text.length() > 0) {
                    StringBuilder formatted = new StringBuilder();
                    int count = 0;
                    for (int i = text.length() - 1; i >= 0; i--) {
                        if (count > 0 && count % 3 == 0) {
                            formatted.insert(0, ".");
                        }
                        formatted.insert(0, text.charAt(i));
                        count++;
                    }

                    if (!textField.getText().equals(formatted.toString())) {
                        int caretPos = textField.getCaretPosition();
                        textField.setText(formatted.toString());
                        try {
                            textField.setCaretPosition(Math.min(caretPos, formatted.length()));
                        } catch (IllegalArgumentException ex2) {
                            textField.setCaretPosition(formatted.length());
                        }
                    }
                }
            }
        }
    }

    private long parseTimeWithTrick(String timeText) throws NumberFormatException {
        String numbersOnly = timeText.replaceAll("[^0-9]", "");
        if (numbersOnly.isEmpty()) {
            throw new NumberFormatException("Campo vacío");
        }

        if (numbersOnly.length() > 18) {
            numbersOnly = numbersOnly.substring(0, 18);
        }

        while (numbersOnly.length() > 1) {
            try {
                long result = Long.parseLong(numbersOnly);
                return result;
            } catch (NumberFormatException ex) {
                numbersOnly = numbersOnly.substring(1);
            }
        }

        return Long.parseLong(numbersOnly);
    }

    private long parseTimeField(JTextField field) throws NumberFormatException {
        return parseTimeWithTrick(field.getText());
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS CON PARTICIONES");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Panel izquierdo con pestañas
        JTabbedPane leftTabbedPane = new JTabbedPane();
        leftTabbedPane.setPreferredSize(new Dimension(500, 0));

        Font tabFont = new Font("Arial", Font.BOLD, 16); 
        leftTabbedPane.setFont(tabFont);
        
        // Pestaña de Procesos
        JPanel processesTab = createProcessesTab();
        leftTabbedPane.addTab("Procesos", processesTab);

        // Panel derecho (resultados)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        JPanel buttonPanel = createResultButtonPanel();
        rightPanel.add(buttonPanel, BorderLayout.NORTH);

        rightPanel.add(resultsPanel, BorderLayout.CENTER);

        // Panel de acciones globales
        JPanel globalActionsPanel = createGlobalActionsPanel();

        add(titlePanel, BorderLayout.NORTH);
        add(leftTabbedPane, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(globalActionsPanel, BorderLayout.SOUTH);
    }

    private JPanel createProcessesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = createProcessFormPanel();
        panel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(processTable);
        tableScrollPane.setPreferredSize(new Dimension(480, 200));
        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createProcessActionPanel();
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPartitionsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = createPartitionFormPanel();
        panel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(partitionTable);
        tableScrollPane.setPreferredSize(new Dimension(480, 200));
        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createPartitionActionPanel();
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProcessFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Nombre
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessName, gbc);
        row++;
        
        // Tiempo
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessTime, gbc);
        row++;
        
        // Tamaño
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Tamaño:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessSize, gbc);
        row++;
        
        // Estado
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        
        addEnterKeyListenerToProcessForm();
        
        return panel;
    }
    
    private void addEnterKeyListenerToProcessForm() {
        KeyListener enterListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addProcess();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
        };
        
        txtProcessName.addKeyListener(enterListener);
        txtProcessTime.addKeyListener(enterListener);
        txtProcessSize.addKeyListener(enterListener);
        cmbStatus.addKeyListener(enterListener);
    }

    private JPanel createPartitionFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nueva Partición"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPartitionName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tamaño:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPartitionSize, gbc);

        return panel;
    }

    private JPanel createProcessActionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Modificar");
        JButton btnDelete = new JButton("Eliminar");

        Dimension buttonSize = new Dimension(140, 35);
        btnAdd.setPreferredSize(buttonSize);
        btnEdit.setPreferredSize(buttonSize);
        btnDelete.setPreferredSize(buttonSize);

        btnAdd.addActionListener(e -> addProcess());
        btnEdit.addActionListener(e -> editProcess());
        btnDelete.addActionListener(e -> deleteProcess());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnAdd, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(btnEdit, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(btnDelete, gbc);

        return panel;
    }

    private JPanel createPartitionActionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Modificar");
        JButton btnDelete = new JButton("Eliminar");
        
        Dimension buttonSize = new Dimension(140, 35);
        btnAdd.setPreferredSize(buttonSize);
        btnEdit.setPreferredSize(buttonSize);
        btnDelete.setPreferredSize(buttonSize);
        
        btnAdd.addActionListener(e -> addPartition());
        btnEdit.addActionListener(e -> editPartition());
        btnDelete.addActionListener(e -> deletePartition());
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(btnAdd, gbc);
        
        gbc.gridx = 1;
        panel.add(btnEdit, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(btnDelete, gbc);
        
        return panel;
    }

    private JPanel createGlobalActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton btnSimulate = new JButton("Ejecutar Simulación");
        JButton btnReset = new JButton("Limpiar Todo");
        JButton btnManual = new JButton("Manual de usuario");
        JButton btnExit = new JButton("Salir");

        Dimension buttonSize = new Dimension(180, 40);
        btnSimulate.setPreferredSize(buttonSize);
        btnReset.setPreferredSize(buttonSize);
        btnManual.setPreferredSize(buttonSize);
        btnExit.setPreferredSize(buttonSize);

        btnSimulate.setBackground(new Color(46, 125, 50));
        btnSimulate.setForeground(Color.WHITE);
        btnSimulate.setOpaque(true);
        btnSimulate.setBorderPainted(false);
        btnSimulate.setFocusPainted(false);

        btnExit.setBackground(new Color(198, 40, 40));
        btnExit.setForeground(Color.WHITE);
        btnExit.setOpaque(true);
        btnExit.setBorderPainted(false);
        btnExit.setFocusPainted(false);

        btnSimulate.addActionListener(e -> runSimulation());
        btnReset.addActionListener(e -> clearAll());
        btnManual.addActionListener(e -> openUserManual());
        btnExit.addActionListener(e -> System.exit(0));

        panel.add(btnSimulate);
        panel.add(btnReset);
        panel.add(btnManual);
        panel.add(btnExit);

        return panel;
    }

    private JPanel createResultButtonPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new GridLayout(4, 4, 5, 5));  // ← MODIFICADO: 4 filas para 14 botones
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < tableNames.length; i++) {
            JButton btn = new JButton(tableNames[i]);
            btn.setPreferredSize(new Dimension(120, 30));
            final int index = i;
            btn.addActionListener(e -> {
                cardLayout.show(resultsPanel, tableNames[index]);
                updateResultTable(index);
            });
            buttonPanel.add(btn);
        }
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Panel de filtro para Listo
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filtrar por Partición:"));
        filterPanel.add(cmbPartitionFilter);
        
        JButton btnApplyFilter = new JButton("Aplicar Filtro");
        btnApplyFilter.addActionListener(e -> applyPartitionFilter());
        filterPanel.add(btnApplyFilter);
        
        mainPanel.add(filterPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void setupEventHandlers() {
        // Eventos ya manejados en los métodos create...
    }

    private void addPartition() {
        String name = txtPartitionName.getText().trim();
        String sizeText = txtPartitionSize.getText().trim();

        if (name.isEmpty()) {
            showError("El nombre de la partición no puede estar vacío");
            return;
        }

        if (processManager.partitionExists(name)) {
            showError("Ya existe una partición con ese nombre");
            return;
        }

        try {
            long size = parseTimeField(txtPartitionSize);
            if (size <= 0) {
                showError("El tamaño debe ser mayor a 0");
                return;
            }

            processManager.addPartition(name, size);
            
            try {
                updatePartitionTable();
            } catch (Exception ex) {
                System.err.println("Error en updatePartitionTable: " + ex.getMessage());
                ex.printStackTrace();
            }
            
            try {
                updatePartitionFilterComboBox();
            } catch (Exception ex) {
                System.err.println("Error en updatePartitionFilterComboBox: " + ex.getMessage());
                ex.printStackTrace();
            }
            
            txtPartitionName.setText("");
            txtPartitionSize.setText("");
            
            showInfo("Partición agregada exitosamente");

        } catch (NumberFormatException ex) {
            showError("Ingrese un tamaño válido");
        } catch (Exception ex) {
            System.err.println("Error general en addPartition: " + ex.getMessage());
            ex.printStackTrace();
            showError("Error al agregar partición: " + ex.getMessage());
        }
    }

    private void deletePartition() {
        int selectedRow = partitionTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione una partición para eliminar");
            return;
        }

        String partitionName = (String) partitionTableModel.getValueAt(selectedRow, 0);
        
        if (processManager.hasPartitionAssignedProcesses(partitionName)) {
            showError("No se puede eliminar la partición '" + partitionName + 
                     "' porque tiene procesos asignados");
            return;
        }

        currentAction = "DELETE_PARTITION:" + partitionName;
        new CustomDialog(this, "¿Está seguro de que desea eliminar la partición '" + partitionName + "'?",
                CustomDialog.CONFIRM_TYPE);
    }

    private void updatePartitionTable() {
        partitionTableModel.setRowCount(0);

        for (Partition p : processManager.getPartitions()) {
            String formattedSize = numberFormatter.format(p.getSize());
            partitionTableModel.addRow(new Object[] {
                    p.getName(),
                    formattedSize,
                    p.getProcessCount()
            });
        }
    }

    private void updatePartitionFilterComboBox() {
        cmbPartitionFilter.removeAllItems();
        cmbPartitionFilter.addItem("Todas las particiones");
        for (Partition p : processManager.getPartitions()) {
            cmbPartitionFilter.addItem(p.getName());
        }
    }

    private void clearPartitionForm() {
        txtPartitionName.setText("");
        txtPartitionSize.setText("");
    }

    private void addProcess() {
        String name = txtProcessName.getText().trim();
        
        if (name.isEmpty()) {
            showError("El nombre del proceso no puede estar vacío");
            clearProcessForm(); 
            return;
        }
        
        if (processManager.processExists(name)) {
            showError("Ya existe un proceso con ese nombre");
            clearProcessForm(); 
            return;
        }
        
        try {
            long time = parseTimeField(txtProcessTime);
            if (time <= 0) {
                showError("El tiempo debe ser mayor a 0");
                clearProcessForm(); 
                return;
            }
            
            long size = parseTimeField(txtProcessSize);
            if (size <= 0) {
                showError("El tamaño debe ser mayor a 0");
                clearProcessForm(); 
                return;
            }
            
            Status status = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            
            processManager.addProcess(name, time, status, size);
            
            updateProcessTable();
            clearProcessForm();
            showInfo("Proceso agregado exitosamente");
            
        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
            clearProcessForm();
        }
    }

    private void editProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para modificar");
            return;
        }

        String oldName = (String) processTableModel.getValueAt(selectedRow, 0);
        model.Process selectedProcess = null;

        for (model.Process p : processManager.getInitialProcesses()) {
            if (p.getName().equals(oldName)) {
                selectedProcess = p;
                break;
            }
        }

        if (selectedProcess == null)
            return;

        JDialog editDialog = createEditDialog(selectedProcess, selectedRow);
        editDialog.setVisible(true);
    }

    private void editPartition() {
        int selectedRow = partitionTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione una partición para modificar");
            return;
        }
        
        String partitionName = (String) partitionTableModel.getValueAt(selectedRow, 0);
        Partition partition = processManager.findPartitionByName(partitionName);
        
        if (partition == null) return;
        
        JDialog editDialog = createEditPartitionDialog(partition);
        editDialog.setVisible(true);
    }

    private JDialog createEditPartitionDialog(Partition partition) {
        JDialog dialog = new JDialog(this, "Modificar Partición", true);
        dialog.setUndecorated(true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(44, 62, 80));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        
        JLabel titleLabel = new JLabel("Modificar Partición");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        JTextField txtEditName = new JTextField(partition.getName(), 20);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);
        addDialogComponentStyled(mainPanel, gbc, "Nombre:", txtEditName, 1);
        
        JTextField txtEditSize = new JTextField(numberFormatter.format(partition.getSize()), 20);
        setupTimeField(txtEditSize);
        addDialogComponentStyled(mainPanel, gbc, "Tamaño:", txtEditSize, 2);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(44, 62, 80));
        
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        
        btnSave.addActionListener(e -> {
            try {
                long newSize = parseTimeField(txtEditSize);
                if (newSize <= 0) {
                    showError("El tamaño debe ser mayor a 0");
                    return;
                }
                
                processManager.editPartition(partition.getName(), newSize);
                updatePartitionTable();
                dialog.dispose();
                clearPartitionForm();
                
                showInfo("Partición modificada exitosamente");
                
            } catch (NumberFormatException ex) {
                showError("Ingrese un tamaño válido");
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);
        
        dialog.add(mainPanel);
        return dialog;
    }

    private JDialog createEditDialog(model.Process process, int selectedRow) {
        JDialog dialog = new JDialog(this, "Modificar Proceso", true);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);

        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(44, 62, 80));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);

        JLabel titleLabel = new JLabel("Modificar Proceso");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        mainPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 10, 8, 10);

        JTextField txtEditName = new JTextField(process.getName(), 20);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);
        txtEditName.setFont(new Font("Arial", Font.PLAIN, 14));

        JTextField txtEditTime = new JTextField(numberFormatter.format(process.getOriginalTime()), 20);
        txtEditTime.setFont(new Font("Arial", Font.PLAIN, 14));
        setupTimeField(txtEditTime);

        JTextField txtEditSize = new JTextField(numberFormatter.format(process.getSize()), 20);
        txtEditSize.setFont(new Font("Arial", Font.PLAIN, 14));
        setupTimeField(txtEditSize);

        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[] { "No bloqueado", "Bloqueado" });
        cmbEditStatus.setSelectedIndex(process.isBlocked() ? 1 : 0);
        cmbEditStatus.setFont(new Font("Arial", Font.PLAIN, 14));

        int row = 1;
        addDialogComponentStyled(mainPanel, gbc, "Nombre:", txtEditName, row++);
        addDialogComponentStyled(mainPanel, gbc, "Tiempo:", txtEditTime, row++);
        addDialogComponentStyled(mainPanel, gbc, "Tamaño:", txtEditSize, row++);
        addDialogComponentStyled(mainPanel, gbc, "Estado:", cmbEditStatus, row++);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(44, 62, 80));

        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");

        Dimension buttonSize = new Dimension(100, 35);
        btnSave.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);

        btnSave.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSave.setBackground(Color.WHITE);
        btnSave.setForeground(new Color(44, 62, 80));
        btnSave.setFocusPainted(false);

        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(new Color(44, 62, 80));
        btnCancel.setFocusPainted(false);

        btnSave.addActionListener(e -> {
            if (saveEditedProcess(dialog, process, selectedRow, txtEditTime, txtEditSize,
                    cmbEditStatus)) {
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        mainPanel.add(buttonPanel, gbc);

        dialog.add(mainPanel);

        dialog.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSave.doClick();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        dialog.setFocusable(true);
        dialog.requestFocus();

        return dialog;
    }

    private void addDialogComponentStyled(JPanel panel, GridBagConstraints gbc, String labelText, JComponent component,
            int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(140, 25));
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    private boolean saveEditedProcess(JDialog dialog, model.Process originalProcess, int selectedRow,
            JTextField txtTime, JTextField txtSize, JComboBox<String> cmbStatus) {
        try {
            long newTime = parseTimeField(txtTime);
            if (newTime <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return false;
            }
            
            long newSize = parseTimeField(txtSize);
            if (newSize <= 0) {
                showError("El tamaño debe ser mayor a 0");
                return false;
            }
            
            Status newStatus = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            
            processManager.editProcess(selectedRow, originalProcess.getName(), newTime, newStatus, newSize);
            
            updateProcessTable();
            showInfo("Proceso editado exitosamente");
            return true;
            
        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
            return false;
        }
    }

    private void deleteProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para eliminar");
            return;
        }

        String processName = (String) processTableModel.getValueAt(selectedRow, 0);
        currentAction = "DELETE_PROCESS:" + processName;
        new CustomDialog(this, "¿Está seguro de que desea eliminar el proceso '" + processName + "'?",
                CustomDialog.CONFIRM_TYPE);
    }

    private void updateProcessTable() {
        processTableModel.setRowCount(0);
        
        for (model.Process p : processManager.getInitialProcesses()) {
            String formattedTime = numberFormatter.format(p.getOriginalTime());
            String formattedSize = numberFormatter.format(p.getSize());
            
            processTableModel.addRow(new Object[] {
                    p.getName(),
                    formattedTime,
                    p.getStatusString(),
                    formattedSize,
                    "Asignación dinámica" 
            });
        }
    }

    private void clearProcessForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        txtProcessSize.setText("");
        cmbStatus.setSelectedIndex(0);
    }

    // ========== SIMULACIÓN ==========

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        } 
        processManager.runSimulation();

        updatePartitionFilterComboBox();
        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }

        cardLayout.show(resultsPanel, tableNames[0]);
        showInfo("Simulación ejecutada exitosamente");
    }

    // ← MODIFICADO: Actualizar tabla de resultados con límites y compactaciones
    private void updateResultTable(int tableIndex) {
        // Tabla especial para Particiones (índice 9)
        if (tableIndex == 9) {
            resultTableModels[9].setRowCount(0);
            for (Partition p : processManager.getPartitions()) {
                String formattedSize = numberFormatter.format(p.getSize());
                String formattedInitialLimit = numberFormatter.format(p.getInitialLimit());
                String formattedFinalLimit = numberFormatter.format(p.getFinalLimit());
                String processNames = p.getProcessHistoryString();
                
                resultTableModels[9].addRow(new Object[] {
                        p.getName(),
                        formattedSize,
                        formattedInitialLimit,  // ← NUEVO
                        formattedFinalLimit,    // ← NUEVO
                        processNames
                });
            }
            return;
        }

        // Tabla especial para Finalización de Particiones (índice 10)
        if (tableIndex == 10) {
            resultTableModels[10].setRowCount(0);
            List<ProcessManager.PartitionFinalizationInfo> report = processManager.getPartitionFinalizationReport();
            
            for (ProcessManager.PartitionFinalizationInfo info : report) {
                Partition partition = processManager.findPartitionByName(info.getName());
                
                String formattedSize = numberFormatter.format(info.getSize());
                String formattedInitialLimit = numberFormatter.format(partition != null ? partition.getInitialLimit() : 0);
                String formattedFinalLimit = numberFormatter.format(partition != null ? partition.getFinalLimit() : 0);
                String formattedTime = numberFormatter.format(info.getTotalTime());
                
                resultTableModels[10].addRow(new Object[] {
                        info.getName(),
                        formattedSize,
                        formattedInitialLimit,  // ← NUEVO
                        formattedFinalLimit,    // ← NUEVO
                        info.getProcessNames(),
                        formattedTime
                });
            }
            return;
        }

        if (tableIndex == 11) {
            resultTableModels[11].setRowCount(0);
            List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
            
            for (Log log : logs) {
                String formattedProcessSize = numberFormatter.format(log.getSize());
                
                String formattedPartitionSize = "N/A";
                String exceedByMessage = "Proceso no cabe en ninguna partición";
                
                if (log.getPartition() != null) {
                    formattedPartitionSize = numberFormatter.format(log.getPartition().getSize());
                    long exceedBy = log.getSize() - log.getPartition().getSize();
                    String formattedExceedBy = numberFormatter.format(exceedBy);
                    exceedByMessage = "El proceso excede el tamaño de la partición en: " + formattedExceedBy;
                }
                
                resultTableModels[11].addRow(new Object[] {
                        log.getProcessName(),
                        formattedProcessSize,
                        log.getPartitionName(),
                        formattedPartitionSize,
                        exceedByMessage
                });
            }

            JTable table = (JTable) ((JScrollPane) resultsPanel.getComponent(11)).getViewport().getView();
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(1).setPreferredWidth(120);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getColumnModel().getColumn(3).setPreferredWidth(150);
            table.getColumnModel().getColumn(4).setPreferredWidth(200);

            return;
        }
        
        if (tableIndex == 12) {
            resultTableModels[12].setRowCount(0);
            ArrayList<Condensation> condensations = processManager.getCondensations();
            
            for (Condensation cond : condensations) {
                String formattedSize = numberFormatter.format(cond.getSize());
                
                StringBuilder partitionNames = new StringBuilder();
                ArrayList<Partition> partitions = cond.getPartitions();
                for (int i = 0; i < partitions.size(); i++) {
                    partitionNames.append(partitions.get(i).getName());
                    if (i < partitions.size() - 1) {
                        partitionNames.append(", ");
                    }
                }
                
                resultTableModels[12].addRow(new Object[] {
                        cond.getName(),
                        formattedSize,
                        partitionNames.toString()
                });
            }
            return;
        }
        
        // ← NUEVO: Tabla de Compactaciones (índice 13)
        if (tableIndex == 13) {
            resultTableModels[13].setRowCount(0);
            ArrayList<Compactation> compactations = processManager.getCompactations();
            
            for (Compactation comp : compactations) {
                String formattedSize = numberFormatter.format(comp.getSize());
                String formattedInitialLimit = numberFormatter.format(
                    comp.getPartitionCreated() != null ? comp.getPartitionCreated().getInitialLimit() : 0
                );
                String formattedFinalLimit = numberFormatter.format(
                    comp.getPartitionCreated() != null ? comp.getPartitionCreated().getFinalLimit() : 0
                );
                String reason = comp.isForExpired() ? "Expiración de tiempo" : "Finalización de proceso";
                
                resultTableModels[13].addRow(new Object[] {
                        comp.getName(),
                        formattedSize,
                        comp.getProcess() != null ? comp.getProcess().getName() : "N/A",
                        comp.getPartitionCreated() != null ? comp.getPartitionCreated().getName() : "N/A",
                        formattedInitialLimit,
                        formattedFinalLimit,
                        reason
                });
            }
            return;
        }
        
        // Tabla especial para Inicial (índice 0)
        if (tableIndex == 0) {
            resultTableModels[0].setRowCount(0);
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());
                String formattedSize = numberFormatter.format(p.getSize());

                resultTableModels[0].addRow(new Object[] {
                        p.getName(),
                        formattedTime,
                        p.getStatusString(),
                        formattedSize,
                        "Asignación dinámica",
                        0
                });
            }
            return;
        }

        // Resto de tablas (logs)
        List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
        resultTableModels[tableIndex].setRowCount(0);

        for (Log log : logs) {
            String formattedTime = numberFormatter.format(log.getRemainingTime());
            String formattedSize = numberFormatter.format(log.getSize());
            
            String partitionInfo;
            
            if (filters[tableIndex] == Filter.FINALIZADO) {
                partitionInfo = log.getPartitionHistoryString();
            } else {
                partitionInfo = log.getPartitionName();
            }

            resultTableModels[tableIndex].addRow(new Object[] {
                    log.getProcessName(),
                    formattedTime,
                    log.getStatusString(),
                    formattedSize,
                    partitionInfo,
                    log.getCycleCount()
            });
        }
    }

    private void applyPartitionFilter() {
        String selectedPartition = (String) cmbPartitionFilter.getSelectedItem();
        
        if (selectedPartition == null || selectedPartition.equals("Todas las particiones")) {
            updateResultTable(1);
        } else {
            resultTableModels[1].setRowCount(0);
            List<Log> logs = processManager.getLogsByFilterAndPartition(Filter.LISTO, selectedPartition);
            
            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());
                String formattedSize = numberFormatter.format(log.getSize());

                resultTableModels[1].addRow(new Object[] {
                        log.getProcessName(),
                        formattedTime,
                        log.getStatusString(),
                        formattedSize,
                        log.getPartitionName(),
                        log.getCycleCount()
                });
            }
        }
        
        cardLayout.show(resultsPanel, tableNames[1]);
    }

    // ========== UTILIDADES ==========

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los datos?", CustomDialog.CONFIRM_TYPE);
    }

    private void openUserManual() {
        try {
            File manualFile = new File("Manual_Usuario.pdf");

            if (!manualFile.exists()) {
                showError("No se encontró el archivo del manual de usuario.<br>" +
                        "Asegúrese de que el archivo 'Manual_Usuario.pdf'<br>" +
                        "esté en la misma carpeta que el programa.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(manualFile);
                } else {
                    showError("Su sistema no permite abrir archivos PDF automáticamente.<br>" +
                            "Por favor, abra manualmente el archivo:<br>" +
                            "Manual_Usuario.pdf");
                }
            } else {
                showError("Su sistema no permite abrir archivos automáticamente.<br>" +
                        "Por favor, abra manualmente el archivo:<br>" +
                        manualFile.getAbsolutePath());
            }

        } catch (IOException ex) {
            showError("Error al abrir el manual de usuario:<br>" + ex.getMessage());
        } catch (Exception ex) {
            showError("Error inesperado al abrir el manual:<br>" + ex.getMessage());
        }
    }

    private void showError(String message) {
        new CustomDialog(this, message, CustomDialog.WARNING_TYPE);
    }

    private void showInfo(String message) {
        new CustomDialog(this, message, CustomDialog.INFO_TYPE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case view.Constants.CLOSE_WARNING:
            case view.Constants.CLOSE_INFO:
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case view.Constants.CONFIRM_YES:
                handleConfirmYes();
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;

            case view.Constants.CONFIRM_NO:
                ((JDialog) ((JButton) e.getSource()).getTopLevelAncestor()).dispose();
                break;
        }
    }

    private void handleConfirmYes() {
        if (currentAction != null) {
            if (currentAction.startsWith("DELETE_PROCESS:")) {
                String processName = currentAction.substring("DELETE_PROCESS:".length());
                processManager.removeProcess(processName);
                updateProcessTable();
                updatePartitionTable();
                showInfo("Proceso eliminado");
            } else if (currentAction.startsWith("DELETE_PARTITION:")) {
                String partitionName = currentAction.substring("DELETE_PARTITION:".length());
                processManager.removePartition(partitionName);
                updatePartitionTable();
                updatePartitionFilterComboBox();
                showInfo("Partición eliminada");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();
                updatePartitionTable();
                updatePartitionFilterComboBox();

                for (DefaultTableModel model : resultTableModels) {
                    model.setRowCount(0);
                }

                clearProcessForm();
                clearPartitionForm();
                showInfo("Todos los datos han sido eliminados");
            }
            currentAction = null;
        }
    }
}