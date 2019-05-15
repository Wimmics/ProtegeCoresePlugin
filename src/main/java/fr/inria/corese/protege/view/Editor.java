package fr.inria.corese.protege.view;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.load.LoadException;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.exceptions.EngineException;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import javax.swing.*;

import static java.awt.BorderLayout.*;


public class Editor extends JPanel {
    private JTextArea requestArea;

    private JTextArea constraintsArea;

    private JButton evaluateRequest = new JButton("Evaluate Request");

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

        modelManager.addListener(modelListener);
        evaluateRequest.addActionListener(refreshAction);

        requestArea = new JTextArea(20,80);
        requestArea.setText("# shape for shape\n" +
                "select *\n" +
                "where {\n" +
                "   #bind (xt:transformer(st:ds, true) as ?d)\n" +
                "   bind (xt:shapeGraph() as ?g)\n" +
                "   bind (xt:turtle(?g) as ?t)\n" +
                "}" );
        requestArea.setFont(new Font("Serif", Font.ITALIC, 16));
        requestArea.setLineWrap(true);
        requestArea.setWrapStyleWord(true);

//        constraintsArea = new JTextArea("@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
//                "@prefix us: <http://www.corese.inria.fr/user#> .\n" +
//                "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
//                "\n" +
//                "us:test a sh:NodeShape ;\n" +
//                "sh:targetClass foaf:Person ;\n" +
//                "sh:property [\n" +
//                "    sh:path foaf:knows;\n" +
//                "    sh:minCount 1;\n" +
//                "    sh:class foaf:Person\n" +
//                "] .");
        constraintsArea = new JTextArea(20,80);
        constraintsArea.setText("@prefix pizza: <http://www.co-ode.org/ontologies/pizza/pizza.owl#> .\n"+
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

        add(new JScrollPane(requestArea), NORTH);
        add(new JScrollPane(constraintsArea), CENTER);
        add(new JScrollPane(evaluateRequest), SOUTH);
        add(new JScrollPane(resultComponent), SOUTH);
    }

    public void dispose() {
        modelManager.removeListener(modelListener);
        evaluateRequest.removeActionListener(refreshAction);
    }

    private void recalculate() {
        String fileName = "/Users/edemairy/tmp/ontology.ttl";
        String constraintsFileName = "/Users/edemairy/tmp/constraints_shacl.ttl";

        // Save the Protégé ontology in fileName so that it can be read by Corese.
        OWLOntology ontology = this.modelManager.getActiveOntology();
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
        String query = requestArea.getText();
        Mappings map = null;
        try {
            map = exec.query(query);
        } catch (EngineException e) {
            e.printStackTrace();
        }
        ResultFormat f1 = ResultFormat.create(map);
        System.err.println("Result = "+f1);
        resultComponent.setText("Result = \n" + f1);
    }
}
