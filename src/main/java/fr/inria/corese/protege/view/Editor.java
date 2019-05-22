package fr.inria.corese.protege.view;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;


import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.gui.query.SparqlQueryEditor;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.protege.mappingsviewer.TableViewer;
import fr.inria.corese.sparql.exceptions.EngineException;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semarglproject.vocab.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import static java.awt.BorderLayout.*;


public class Editor extends JPanel {
    private Logger logger = LoggerFactory.getLogger(Editor.class);
    private JTabbedPane tabbedPaneResults;
    private TableViewer tableResults;
    private SparqlQueryEditor requestArea;

    private JTextArea constraintsArea;

    private JButton evaluateRequestButton;

    private JTextArea resultComponent = new JTextArea(40, 120);

    private OWLModelManager modelManager;


    private ActionListener refreshAction = e -> recalculate();

    private OWLModelManagerListener modelListener = event -> {
        if (event.getType() == EventType.ACTIVE_ONTOLOGY_CHANGED) {
            recalculate();
        }
    };

    public Editor(OWLModelManager modelManager) {
        this.modelManager = modelManager;


        JComponent editorPanel = createRequestEditorPanel();
        JComponent constraintsPanel = createConstraintsPanel();

        JSplitPane upperPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        upperPanel.add(editorPanel);
        upperPanel.add(constraintsPanel);
        add(upperPanel);

        JComponent buttonsPanel = createButtonsPanel();
        modelManager.addListener(modelListener);
        evaluateRequestButton.addActionListener(refreshAction);

        JComponent resultsPanel = createResultsPanel();

        JPanel lowerPanel = new JPanel();
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(buttonsPanel, NORTH);
        lowerPanel.add(resultsPanel, CENTER);

        JSplitPane fullPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        fullPanel.add(upperPanel);
        fullPanel.add(lowerPanel);
        add(fullPanel);
    }

    private JComponent createResultsPanel() {
        tabbedPaneResults = new JTabbedPane();
        tabbedPaneResults.add("Raw Results", new JScrollPane(resultComponent));
        tableResults = new TableViewer();
        JScrollPane tableScroll = new JScrollPane();
        tableScroll.setViewportView(tableResults);
        tabbedPaneResults.add("Table Results", tableScroll);
        return tabbedPaneResults;
    }

    private JComponent createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

        // Begin of upper part
        evaluateRequestButton = new JButton("Evaluate Request");
        JPanel upperPart = new JPanel();
        upperPart.add(evaluateRequestButton);
        //End of upper part

        // Begin to build the ontology panel.
        JRadioButton activeOntology = new JRadioButton("Active Ontology");
        activeOntology.setToolTipText(modelManager.getActiveOntology().getOntologyID().toString());
        JRadioButton activeOntologies = new JRadioButton("Select Among Active Ontologies");

        ButtonGroup activeOntologiesButtonGroup = new ButtonGroup();
        activeOntologiesButtonGroup.add(activeOntologies);
        activeOntologiesButtonGroup.add(activeOntology);

        JPanel activeOntologiesPanel = new JPanel(new GridLayout(0, 1));
        activeOntologiesPanel.add(activeOntologies);
        activeOntologiesPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        JPanel ontologiesPanel = new JPanel(new GridLayout(0, 1));
        ArrayList<JCheckBox> checkboxes = new ArrayList<>();
        for (OWLOntology ontology : modelManager.getActiveOntologies()) {
            JCheckBox checkbox = new JCheckBox(ontology.getOntologyID().toString());
            checkbox.setEnabled(false);
            checkboxes.add(checkbox);
            ontologiesPanel.add(new JPanel());
            ontologiesPanel.add(checkbox);
        }

        activeOntologiesPanel.add(ontologiesPanel);
        activeOntologies.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    for (JCheckBox checkbox : checkboxes) {
                        checkbox.setEnabled(true);
                    }
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    for (JCheckBox checkbox : checkboxes) {
                        checkbox.setEnabled(true);
                    }
                }
            }
        });

        buttonPanel.add(upperPart);
        buttonPanel.add(activeOntologiesPanel);
        buttonPanel.add(activeOntology);
        // End of building the ontology panel.

        return buttonPanel;
    }

    private JComponent createRequestEditorPanel() {
        requestArea = new SparqlQueryEditor();
        requestArea.setQueryText("select * where {" +
                "  ?s ?p ?o" +
                "}");
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
        constraintsArea = new JTextArea(10, 80);
        constraintsArea.setText("@prefix pizza: <http://www.co-ode.org/ontologies/pizza/pizza.owl#> .\n" +
                "@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
                "@prefix us: <http://www.corese.inria.fr/user#> .\n" +
                "\n" +
                "us:test a sh:NodeShape; \n" +
                "sh:targetClass pizza:Pizza;\n" +
                "sh:property [\n" +
                "    sh:path pizza:hasTopping;\n" +
                "    sh:minCount 1;\n" +
                "    sh:class pizza:OliveTopping\n" +
                "] .");

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

    private void recalculate() {
        String fileName = "/Users/edemairy/tmp/ontology.ttl";
        String constraintsFileName = "/Users/edemairy/tmp/constraints_shacl.ttl";

        Set<OWLOntology> ontologies = modelManager.getActiveOntologies();
        for (OWLOntology ontology : ontologies) {
            logger.info("Ontology ID: {}", ontology.getOntologyID());
        }
        // Save the Protégé ontology in fileName so that it can be read by Corese.
        OWLOntology ontology = modelManager.getActiveOntology();
        try (FileOutputStream fr = new FileOutputStream(fileName)) {
            ontology.saveOntology(new TurtleDocumentFormat(), fr);
        } catch (IOException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }

        // Saving constraints to file
        try (FileOutputStream fr = new FileOutputStream(constraintsFileName)) {
            fr.write(constraintsArea.getText().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Graph graph = Graph.create();
        Load ld = Load.create(graph);
        try {
            ld.parse(fileName);
        } catch (LoadException e) {
            e.printStackTrace();
        }
        try {
            ld.parse(constraintsFileName);
        } catch (LoadException e) {
            e.printStackTrace();
        }
        QueryProcess exec = QueryProcess.create(graph);
        String query = requestArea.getTextPaneQuery().getText();
        Mappings map = null;
        try {
            map = exec.query(query);
        } catch (EngineException e) {
            e.printStackTrace();
        }
        ResultFormat f1 = ResultFormat.create(map);
        System.err.println("Result = " + f1);
        tableResults.setMappings(map);
        tableResults.updateModel();
        tableResults.invalidate();
        resultComponent.setText("Result = \n" + f1);
    }
}
