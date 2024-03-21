package Interfaces;

import com.Trazability.ImageGenerate.BpmnColor;
import com.Trazability.DataBase.Connections;
import com.Trazability.ImageGenerate.ImageCapture;
import com.Trazability.Main;
import java.awt.Desktop;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

public class Traceability extends javax.swing.JFrame {

    private final Connections con = new Connections();
    private int projectId, selectedVariableId;

    public Traceability() {
        initComponents();
        initializeFrame();
    }

    private void initializeFrame() {
        this.setResizable(false);
        this.setTitle("TRACEABILITY");

        BIMAGE.setVisible(false);
        BDIAGRAM.setVisible(false);

        loadVariableNames(); // Cargar nombres de variables al iniciar
        addVariableSelectionListener(); // Agrega proyectos según la variable
        getProjectSelectionListener(); // Obtener el id según el proyecto
        getMethodSelectionListener(); // Obtener el id según la clase
        loadData(); //Hacer actualizacion en la base de datos
        openImage();
        openDiagram();
    }

    private void loadVariableNames() {
        try {
            List<String> variableNames = con.getAllVariableNames();
            variableNames.add(0, "Choose a variable");

            if (!variableNames.isEmpty()) {
                updateComboBox(VARIABLES, variableNames);
            } else {
                throw new RuntimeException("No se encontraron nombres de variables.");
            }
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    private void updateComboBox(JComboBox<String> comboBox, List<String> items) {
        comboBox.removeAllItems();
        items.forEach(comboBox::addItem);
    }

    private void addVariableSelectionListener() {
        VARIABLES.addActionListener(e -> {
            try {
                
                handleVariableSelection();
            } catch (IOException | InterruptedException ex) {
                handleException(ex);
            }
        });
    }

    private void handleVariableSelection() throws IOException, InterruptedException {
        String selectedVariable = getSelectedVariable();

        if (!"Choose a variable".equals(selectedVariable)) {
            selectedVariableId = con.searchVariableByName(selectedVariable);

            List<String> projectNames = con.searchProjectByVariableId(selectedVariableId);
            String processName = con.searchProcessByVariableId(selectedVariableId);
            String participant = new BpmnColor().findParticipantName();

            updateProjectsList(projectNames);
            updateProcessName(processName + ".bpmn");
            updateParticipant(participant);

            if (projectNames == null || projectNames.isEmpty()) {
                CountProjects.setText("0");
                CountClasses.setText("0");
                CountMethods.setText("0");
            }

            getUsedElement(selectedVariableId);
            BIMAGE.setVisible(true);
            BDIAGRAM.setVisible(true);
        } else {
            updateProjectsList(null);
            updateProcessName("Select a variable");
            updateParticipant("Select a variable");
            JMODEL.setIcon(null);
            BIMAGE.setVisible(false);
            BDIAGRAM.setVisible(false);
        }

    }

    private String getSelectedVariable() {
        return (String) VARIABLES.getSelectedItem();
    }

    private void updateProjectsList(List<String> projectNames) {
        if (projectNames != null && !projectNames.isEmpty()) {
            DefaultListModel<String> model = new DefaultListModel<>();
            projectNames.forEach(model::addElement);
            LPROJECTS.setModel(model);
            CountProjects.setText(Integer.toString(projectNames.size()));
        } else {
            DefaultListModel<String> defaultModel = new DefaultListModel<>();
            defaultModel.addElement("Select a variable.");
            LPROJECTS.setModel(defaultModel);
            CountProjects.setText("0");
        }
    }

    private void updateProcessName(String processName) {
        PROCESS.setText(processName != null && !processName.isEmpty() ? processName : "Process name not found or error in the search.");
    }

    private void updateParticipant(String participant) {
        PARTICIPANT.setText(participant != null && !participant.isEmpty() ? participant : "Participant name not found or error in the search.");
    }

    private void handleException(Exception e) {
        Logger.getLogger(Traceability.class.getName()).log(Level.SEVERE, null, e);
    }

    private void getProjectSelectionListener() {
        LPROJECTS.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleProjectSelection();
            }
        });
    }

    private void handleProjectSelection() {
        String selectedProject = LPROJECTS.getSelectedValue();
        projectId = con.searchProject(selectedProject);

        String containerName = con.searchContainerName(projectId, selectedVariableId);
        CONTAINER.setText(containerName != null && !containerName.isEmpty() ? containerName : "variable not selected");

        List<String> classNames = con.searchClassById(projectId, selectedVariableId);
        updateList(LCLASSES, classNames, CountClasses, "Project not selected");

        if (classNames == null || classNames.isEmpty()) {
            CountClasses.setText("0");
            CountMethods.setText("0");
        }
    }

    private void getMethodSelectionListener() {
        LCLASSES.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleClassSelection();
            }
        });
    }

    private void handleClassSelection() {
        String selectedClass = LCLASSES.getSelectedValue();
        int classId = con.searchClass(selectedClass);

        List<String> methodNames = con.searchMethodById(classId, selectedVariableId);
        updateList(LMETHODS, methodNames, CountMethods, "Class not selected");

        if (methodNames == null || methodNames.isEmpty()) {
            CountMethods.setText("0");
        }
    }

    private void updateList(JList<String> list, List<String> items, JLabel countLabel, String emptyMessage) {
        if (items != null && !items.isEmpty()) {
            DefaultListModel<String> model = new DefaultListModel<>();
            items.forEach(model::addElement);
            list.setModel(model);
            countLabel.setText(Integer.toString(items.size()));
        } else {
            DefaultListModel<String> defaultModel = new DefaultListModel<>();
            defaultModel.addElement(emptyMessage);
            list.setModel(defaultModel);
        }
    }

    private void getUsedElement(int selectedVariableId) throws IOException, InterruptedException {
        if (selectedVariableId <= 0) {
//            System.err.println("Error: ID de variable no válido.");
            return;
        }

        List<String> usedElementNames = con.searchElementsUsed(selectedVariableId);

        if (!usedElementNames.isEmpty() && !usedElementNames.get(0).equals("Elemento no encontrado")) {
            BpmnColor modifier = new BpmnColor();
            modifier.modifyActivityColors(usedElementNames);

            ImageCapture capture = new ImageCapture();
            capture.imageCapture();

            loadImage();
        } else {
            System.out.println("No se encontraron elementos usados para la variable con ID " + selectedVariableId);
        }
    }

    private void loadData() {
        // LOAD.addActionListener(e -> {
        //     try {
        //         boolean mainSuccess = Main.main(new String[]{});
        //         if (mainSuccess) {
        //             loadVariableNames();
        //         }
        //     } catch (IOException ex) {
        //         ex.printStackTrace();
        //     }
        // });
    }

    private void loadImage() {
        String rutaImagen = Paths.get(System.getProperty("user.dir"), "output", "MSGF-Test-Color.png").toString();
        ImageIcon icono = new ImageIcon(rutaImagen);

        // Obtiene las dimensiones del JLabel
        int anchoJMODEL = JMODEL.getWidth();
        int altoJMODEL = JMODEL.getHeight();

        // Obtiene las dimensiones originales de la imagen
        int anchoOriginal = icono.getIconWidth();
        int altoOriginal = icono.getIconHeight();

        // Calcula las nuevas dimensiones manteniendo la proporción
        int nuevoAncho = anchoOriginal;
        int nuevoAlto = altoOriginal;

        double proporcionAncho = (double) anchoJMODEL / anchoOriginal;
        double proporcionAlto = (double) altoJMODEL / altoOriginal;

        double proporcion = Math.min(proporcionAncho, proporcionAlto);

        nuevoAncho = (int) (anchoOriginal * proporcion);
        nuevoAlto = (int) (altoOriginal * proporcion);

        // Escala la imagen manteniendo su relación de aspecto
        Image imagen = icono.getImage();
        Image imagenEscalada = imagen.getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_SMOOTH);

        // Crea un nuevo ImageIcon con la imagen escalada
        ImageIcon iconoEscalado = new ImageIcon(imagenEscalada);

        // Asigna el ImageIcon al JLabel
        JMODEL.setIcon(iconoEscalado);
    }

    private void openImage() {
        BIMAGE.addActionListener(e -> {
            String rutaImagen = Paths.get(System.getProperty("user.dir"), "output", "MSGF-Test-Color.png").toString();

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                // Obtener el objeto File para la ruta del archivo
                File archivo = new File(rutaImagen);

                // Verificar si el archivo existe
                if (archivo.exists()) {
                    try {
                        // Abrir el archivo
                        desktop.open(archivo);
                    } catch (IOException ex) {
                        Logger.getLogger(Traceability.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("El archivo no existe.");
                }

            }
        });

    }

    private void openDiagram() {
        BDIAGRAM.addActionListener(e -> {
            String rutaDiagram = Paths.get(System.getProperty("user.dir"), "output", "ColorModel.bpmn").toString();

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                // Obtener el objeto File para la ruta del archivo
                File archivo = new File(rutaDiagram);

                // Verificar si el archivo existe
                if (archivo.exists()) {
                    try {
                        // Abrir el archivo
                        desktop.open(archivo);
                    } catch (IOException ex) {
                        Logger.getLogger(Traceability.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("El archivo no existe.");
                }

            }
        });

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        PARTICIPANT = new javax.swing.JLabel();
        PROCESS = new javax.swing.JLabel();
        PPROJECTS = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        LPROJECTS = new javax.swing.JList<>();
        CountProjects = new javax.swing.JLabel();
        PCLASSES = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        LCLASSES = new javax.swing.JList<>();
        CountClasses = new javax.swing.JLabel();
        PMETHODS = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        LMETHODS = new javax.swing.JList<>();
        CountMethods = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        CONTAINER = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        VARIABLES = new javax.swing.JComboBox<>();
        LOAD = new javax.swing.JButton();
        JMODEL = new javax.swing.JLabel();
        BIMAGE = new javax.swing.JButton();
        BDIAGRAM = new javax.swing.JButton();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        PARTICIPANT.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        PARTICIPANT.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PARTICIPANT.setText("Select a variable");

        PROCESS.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        PROCESS.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PROCESS.setText("Select a variable");

        PPROJECTS.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel6.setText("PROJECTS");

        LPROJECTS.setBackground(new java.awt.Color(242, 242, 242));
        LPROJECTS.setBorder(javax.swing.BorderFactory.createCompoundBorder());
        LPROJECTS.setFont(new java.awt.Font("Segoe UI", 2, 14)); // NOI18N
        LPROJECTS.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Select a variable" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        LPROJECTS.setVisibleRowCount(100);
        jScrollPane1.setViewportView(LPROJECTS);

        CountProjects.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CountProjects.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CountProjects.setText("0");
        CountProjects.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        CountProjects.setEnabled(false);
        CountProjects.setFocusable(false);

        javax.swing.GroupLayout PPROJECTSLayout = new javax.swing.GroupLayout(PPROJECTS);
        PPROJECTS.setLayout(PPROJECTSLayout);
        PPROJECTSLayout.setHorizontalGroup(
            PPROJECTSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PPROJECTSLayout.createSequentialGroup()
                .addGroup(PPROJECTSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(PPROJECTSLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(PPROJECTSLayout.createSequentialGroup()
                        .addGap(93, 93, 93)
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                        .addGap(82, 82, 82)
                        .addComponent(CountProjects, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        PPROJECTSLayout.setVerticalGroup(
            PPROJECTSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PPROJECTSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PPROJECTSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CountProjects)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        PCLASSES.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setText("CLASSES");

        LCLASSES.setBackground(new java.awt.Color(242, 242, 242));
        LCLASSES.setBorder(null);
        LCLASSES.setFont(new java.awt.Font("Segoe UI", 2, 14)); // NOI18N
        LCLASSES.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Select a project" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(LCLASSES);

        CountClasses.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CountClasses.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CountClasses.setText("0");
        CountClasses.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        CountClasses.setEnabled(false);
        CountClasses.setFocusable(false);

        javax.swing.GroupLayout PCLASSESLayout = new javax.swing.GroupLayout(PCLASSES);
        PCLASSES.setLayout(PCLASSESLayout);
        PCLASSESLayout.setHorizontalGroup(
            PCLASSESLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PCLASSESLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PCLASSESLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2)
                    .addGroup(PCLASSESLayout.createSequentialGroup()
                        .addGap(0, 169, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addGap(117, 117, 117)
                        .addComponent(CountClasses, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        PCLASSESLayout.setVerticalGroup(
            PCLASSESLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PCLASSESLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PCLASSESLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(CountClasses))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        PMETHODS.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setText("METHODS");

        LMETHODS.setBackground(new java.awt.Color(242, 242, 242));
        LMETHODS.setBorder(null);
        LMETHODS.setFont(new java.awt.Font("Segoe UI", 2, 14)); // NOI18N
        LMETHODS.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Select a class" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        LMETHODS.setVisibleRowCount(100);
        jScrollPane3.setViewportView(LMETHODS);

        CountMethods.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CountMethods.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        CountMethods.setText("0");
        CountMethods.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        CountMethods.setEnabled(false);
        CountMethods.setFocusable(false);

        javax.swing.GroupLayout PMETHODSLayout = new javax.swing.GroupLayout(PMETHODS);
        PMETHODS.setLayout(PMETHODSLayout);
        PMETHODSLayout.setHorizontalGroup(
            PMETHODSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PMETHODSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PMETHODSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PMETHODSLayout.createSequentialGroup()
                        .addGap(100, 100, 100)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 78, Short.MAX_VALUE)
                        .addComponent(CountMethods, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3))
                .addContainerGap())
        );
        PMETHODSLayout.setVerticalGroup(
            PMETHODSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PMETHODSLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PMETHODSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(CountMethods))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("CONTAINER");
        jLabel4.setFocusable(false);

        CONTAINER.setFont(new java.awt.Font("Segoe UI", 2, 14)); // NOI18N
        CONTAINER.setText("Select a project");

        VARIABLES.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        VARIABLES.setMaximumRowCount(100);
        VARIABLES.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select a variable" }));
        VARIABLES.setName("Select a variable"); // NOI18N

        LOAD.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        LOAD.setText("Generate New Trace");
        LOAD.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        LOAD.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        LOAD.setPreferredSize(new java.awt.Dimension(80, 26));

        JMODEL.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JMODEL.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images.png"))); // NOI18N
        JMODEL.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        BIMAGE.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        BIMAGE.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image.png"))); // NOI18N
        BIMAGE.setText("Open Image");
        BIMAGE.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        BIMAGE.setPreferredSize(new java.awt.Dimension(150, 35));

        BDIAGRAM.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        BDIAGRAM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/diagram.png"))); // NOI18N
        BDIAGRAM.setText("Open Diagram");
        BDIAGRAM.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        BDIAGRAM.setPreferredSize(new java.awt.Dimension(150, 35));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(147, 147, 147)
                .addComponent(JMODEL, javax.swing.GroupLayout.PREFERRED_SIZE, 765, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(BIMAGE, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(BDIAGRAM, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(VARIABLES, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(131, 131, 131)
                        .addComponent(PARTICIPANT, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(PROCESS, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(35, 35, 35)
                        .addComponent(LOAD, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(24, 24, 24))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(PPROJECTS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(35, 35, 35)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(CONTAINER, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(PCLASSES, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(36, 36, 36)
                        .addComponent(PMETHODS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(PROCESS, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(PARTICIPANT, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(LOAD, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(VARIABLES))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(99, 99, 99)
                        .addComponent(BIMAGE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(65, 65, 65)
                        .addComponent(BDIAGRAM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JMODEL, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(CONTAINER, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(PCLASSES, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(PPROJECTS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PMETHODS, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Traceability.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Traceability.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Traceability.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Traceability.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Traceability().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton BDIAGRAM;
    public javax.swing.JButton BIMAGE;
    public javax.swing.JLabel CONTAINER;
    public javax.swing.JLabel CountClasses;
    public javax.swing.JLabel CountMethods;
    public javax.swing.JLabel CountProjects;
    public javax.swing.JLabel JMODEL;
    public javax.swing.JList<String> LCLASSES;
    public javax.swing.JList<String> LMETHODS;
    public javax.swing.JButton LOAD;
    public javax.swing.JList<String> LPROJECTS;
    public javax.swing.JLabel PARTICIPANT;
    public javax.swing.JPanel PCLASSES;
    public javax.swing.JPanel PMETHODS;
    private javax.swing.JPanel PPROJECTS;
    private javax.swing.JLabel PROCESS;
    public javax.swing.JComboBox<String> VARIABLES;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel4;
    public javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables
}
