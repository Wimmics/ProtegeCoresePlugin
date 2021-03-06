package fr.inria.corese.protege.view;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.gui.query.SparqlQueryEditor;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.protege.mappingsviewer.GraphViewer;
import fr.inria.corese.protege.mappingsviewer.MappingsViewerInterface;
import fr.inria.corese.protege.mappingsviewer.TableViewer;
import fr.inria.corese.protege.mappingsviewer.TreeViewer;
import fr.inria.corese.sparql.exceptions.EngineException;
import org.protege.editor.core.Disposable;
import org.protege.editor.core.ModelManager;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Editor extends JPanel {

    private Logger logger = LoggerFactory.getLogger(Editor.class);
    private JTabbedPane tabbedPaneResults;
    private final HashMap<String, MappingsViewerInterface> mappingsViewers = new HashMap<>();
    private SparqlQueryEditor requestArea;

    private JTextArea constraintsArea;

    private JButton evaluateRequestButton;

    private JTextArea resultComponent;

    private OWLModelManager modelManager;
    private JSplitPane fullPanel;

    private ActionListener refreshAction = e -> recalculate();

    private OWLModelManagerListener modelListener;
    private JRadioButton activeOntology;
    private JRadioButton ontologies;
    private ArrayList<JCheckBox> ontologiesChoice;

    public Editor(OWLModelManager modelManager) {
        this.modelManager = modelManager;
        setLayout(new GridLayout(0, 1));
        JComponent editorPanel = createRequestEditorPanel();
        JComponent constraintsPanel = createConstraintsPanel();

        JSplitPane upperPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, editorPanel, constraintsPanel);

        JComponent buttonsPanel = createButtonsPanel();
        modelListener = event -> {
            if (event.getType() == EventType.ACTIVE_ONTOLOGY_CHANGED) {
                recalculate();
            }
        };
        modelManager.addListener(modelListener);
        evaluateRequestButton.addActionListener(refreshAction);

        JComponent resultsPanel = createResultsPanel();
        JSplitPane lowerPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, buttonsPanel, resultsPanel);

        fullPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, upperPanel, lowerPanel);
        add(fullPanel);
    }

    private JComponent createResultsPanel() {

        tabbedPaneResults = new JTabbedPane();
        resultComponent = new JTextArea();
        tabbedPaneResults.add("Raw Results", new JScrollPane(resultComponent));

        TableViewer tableResults = new TableViewer();
        mappingsViewers.put("Table", tableResults);

        TreeViewer treeResults = new TreeViewer();
        mappingsViewers.put("Tree", treeResults);

        GraphViewer graphResults = new GraphViewer();
        mappingsViewers.put("Graph", graphResults);

        for (String viewerId : mappingsViewers.keySet()) {
            tabbedPaneResults.add(viewerId, mappingsViewers.get(viewerId).getComponent());
        }

        return tabbedPaneResults;
    }

    private JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel();

        // Begin of upper part
        evaluateRequestButton = new JButton("Evaluate Request");
        JPanel upperPart = new JPanel();
        upperPart.add(evaluateRequestButton);
        //End of upper part

        // Begin to build the ontology panel.
        activeOntology = new JRadioButton("Active Ontology");
        activeOntology.setSelected(true);
        activeOntology.setToolTipText(modelManager.getActiveOntology().getOntologyID().toString());

        ontologies = new JRadioButton("Select Among Active Ontologies");
        ButtonGroup activeOntologiesButtonGroup = new ButtonGroup();
        activeOntologiesButtonGroup.add(ontologies);
        activeOntologiesButtonGroup.add(activeOntology);

        JPanel ontologiesPanel = new JPanel(new GridLayout(0, 1));
        ontologiesPanel.add(ontologies);
        ontologiesPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        JPanel ontologiesChoicePanel = new JPanel();
        ontologiesChoice = new ArrayList<>();
        for (OWLOntology ontology : modelManager.getOntologies()) {
            String ontologyShortName = ontology.getOntologyID().toString();
            Pattern splitter = Pattern.compile(".*OntologyIRI\\(([^)]*)\\).*");
            Matcher m = splitter.matcher(ontologyShortName);
            if (m.find()) {
                ontologyShortName = m.group(1);
            } else {
                ontologyShortName = ontologyShortName.substring(Math.max(0,ontologyShortName.length() - 25), ontologyShortName.length() - 1);
            }
            JCheckBox checkbox = new JCheckBox(ontologyShortName);
            checkbox.setToolTipText(ontology.getOntologyID().toString());
            checkbox.setActionCommand(ontology.getOntologyID().toString());
            checkbox.setEnabled(false);
            ontologiesChoice.add(checkbox);
            ontologiesChoicePanel.add(checkbox);
        }

        ontologiesPanel.add(ontologiesChoicePanel);
        ontologies.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                for (JCheckBox checkbox : ontologiesChoice) {
                    checkbox.setEnabled(true);
                }
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                for (JCheckBox checkbox : ontologiesChoice) {
                    checkbox.setEnabled(true);
                }
            }
        });

        buttonPanel.add(upperPart);
        buttonPanel.add(ontologiesPanel);
        buttonPanel.add(activeOntology);
        // End of building the ontology panel.

        return buttonPanel;
    }

    private JComponent createRequestEditorPanel() {
        requestArea = new SparqlQueryEditor();
        requestArea.setQueryText("construct {\n"
                + "?s ?p ?o \n"
                + "} where {\n"
                + "  ?s ?p ?o\n"
                + "}\n");
//        requestArea.setQueryText("# shape for shape\n" +
//                "select *\n" +
//                "where {\n" +
//                "   #bind (xt:transformer(st:ds, true) as ?d)\n" +
//                "   bind (xt:shapeGraph() as ?g)\n" +
//                "   bind (xt:turtle(?g) as ?t)\n" +
//                "}" );
        requestArea.setFont(new Font("Serif", Font.ITALIC, 16));
        JLabel requestLabel = new JLabel("SPARQL Request");
        requestLabel.setLabelFor(requestArea);
        JScrollPane requestScrollPane = new JScrollPane();
        requestScrollPane.setViewportView(requestArea);
        return requestScrollPane;
    }

    private JComponent createConstraintsPanel() {
        constraintsArea = new JTextArea();
        constraintsArea.setText("@prefix pizza: <http://www.co-ode.org/ontologies/pizza/pizza.owl#> .\n"
                + "@prefix sh: <http://www.w3.org/ns/shacl#> .\n"
                + "@prefix us: <http://www.corese.inria.fr/user#> .\n"
                + "\n"
                + "us:test a sh:NodeShape; \n"
                + "sh:targetClass pizza:Pizza;\n"
                + "sh:property [\n"
                + "    sh:path pizza:hasTopping;\n"
                + "    sh:minCount 1;\n"
                + "    sh:class pizza:OliveTopping\n"
                + "] .");

        constraintsArea.setFont(new Font("Serif", Font.ITALIC, 16));
        constraintsArea.setLineWrap(true);
        constraintsArea.setWrapStyleWord(true);
        JLabel constraintsLabel = new JLabel("SHACL Constraints");
        constraintsLabel.setLabelFor(constraintsArea);
        JScrollPane constraintsScrollPane = new JScrollPane();
        constraintsScrollPane.setViewportView(constraintsArea);
        return constraintsScrollPane;
    }

    public void dispose() {
        modelManager.removeListener(modelListener);
        evaluateRequestButton.removeActionListener(refreshAction);
    }

    void recalculate() {
        Graph graph = Graph.create();
        Load ld = Load.create(graph);

        readOntologiesInCorese(ld);
        readShaclConstraintsInCorese(ld);

        QueryProcess exec = QueryProcess.create(graph);
        String query = requestArea.getTextPaneQuery().getText();
        Mappings map = null;
        try {
            map = exec.query(query);
        } catch (EngineException e) {
            e.printStackTrace();
        }
        ResultFormat f1 = ResultFormat.create(map);
        for (MappingsViewerInterface viewer : mappingsViewers.values()) {
            viewer.setMappings(map);
            viewer.updateModel();
        }
        resultComponent.setText("Result = \n" + f1);
    }

    private void readShaclConstraintsInCorese(Load ld) {
        try {
            File constraintsTempFile = File.createTempFile("constraints_shacl", ".ttl");
            String constraintsFileName = constraintsTempFile.getAbsolutePath();
            // Begin of reading constraints
            try ( FileOutputStream fr = new FileOutputStream(constraintsFileName)) {
                fr.write(constraintsArea.getText().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ld.parse(constraintsFileName);
            } catch (LoadException e) {
                e.printStackTrace();
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void readOntologiesInCorese(Load ld) {
        ArrayList<OWLOntology> ontologiesToRead = new ArrayList<>();
        if (activeOntology.isSelected()) {
            ontologiesToRead.add(modelManager.getActiveOntology());
        } else if (ontologies.isSelected()) {
            for (JCheckBox choice : ontologiesChoice) {
                if (choice.isEnabled()) {
                    Set<OWLOntology> ontologies = modelManager.getOntologies();
                    String ontologyId = choice.getActionCommand();
                    for (OWLOntology ontology : ontologies) {
                        if (ontology.getOntologyID().toString().equals(ontologyId)) {
                            ontologiesToRead.add(ontology);
                        }
                    }
                }
            }
        }
        // Save the Protégé ontology in fileName so that it can be read by Corese.
        if (ontologiesToRead.isEmpty()) {
            logger.warn("No ontology to read!");
        }
        File rdfDataFile;
        try {
            rdfDataFile = File.createTempFile("ontology", ".ttl");

            String fileName = rdfDataFile.getAbsolutePath();
            for (OWLOntology ontology : ontologiesToRead) {
//            RDFTranslator translator = new RDFTranslator(modelManager.getOWLOntologyManager(), ontology, true, OWLIndividual::isAnonymous);
//            for (RDFTriple triple: translator.getGraph().getAllTriples()) {
//                logger.info("triple = {}", triple.toString());
//            }
                logger.info("Reading ontology {}", ontology.getOntologyID());
                try ( FileOutputStream fr = new FileOutputStream(fileName)) {
                    ontology.saveOntology(new TurtleDocumentFormat(), fr);
                } catch (IOException | OWLOntologyStorageException e) {
                    e.printStackTrace();
                }
                try {
                    ld.parse(fileName);
                } catch (LoadException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Editor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
//        SparqlQueryEditor requestArea = new SparqlQueryEditor();
//        requestArea.setQueryText("construct { ?s ?p ?o } where {" +
//                "  ?s ?p ?o" +
//                "}");
////        requestArea.setQueryText("# shape for shape\n" +
////                "select *\n" +
////                "where {\n" +
////                "   #bind (xt:transformer(st:ds, true) as ?d)\n" +
////                "   bind (xt:shapeGraph() as ?g)\n" +
////                "   bind (xt:turtle(?g) as ?t)\n" +
////                "}" );
//        requestArea.setFont(new Font("Serif", Font.ITALIC, 16));
//        JLabel requestLabel = new JLabel("SPARQL Request");
//        requestLabel.setLabelFor(requestArea);
//        JPanel panel = new JPanel(new BorderLayout());
//
//        panel.add(requestArea);

        Editor e = new Editor(null);
        JFrame frame = new JFrame("Panel");
        frame.setLocation(200, 200);
        frame.getContentPane().add(e);
        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
