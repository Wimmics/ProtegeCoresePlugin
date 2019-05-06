package fr.inria.corese.protege.view;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;


import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.Load;
import fr.inria.corese.core.print.ResultFormat;
import fr.inria.corese.core.query.QueryProcess;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.exceptions.EngineException;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import javax.swing.*;


public class Editor extends JPanel {
    private JTextArea textArea;

    private JButton evaluateRequest = new JButton("Evaluate Request");

    private JLabel textComponent = new JLabel();

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

        textArea = new JTextArea( "select * where {?s ?p ?o}" );
        textArea.setFont(new Font("Serif", Font.ITALIC, 16));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        add(textArea);
        add(evaluateRequest);
        add(textComponent);
    }
    
    public void dispose() {
        modelManager.removeListener(modelListener);
        evaluateRequest.removeActionListener(refreshAction);
    }
    
    private void recalculate() {
        String fileName = "/tmp/ontology.rdf";
        OWLOntology ontology = this.modelManager.getActiveOntology();

        try(FileOutputStream fr = new FileOutputStream( fileName )) {
            ontology.saveOntology( new RDFXMLDocumentFormat(), fr);
        } catch (IOException | OWLOntologyStorageException e) {
            e.printStackTrace();
        }

        String sparqlRequest = textArea.getText();
        Graph graph = Graph.create();
        Load ld = Load.create(graph);
        ld.load(fileName);
        QueryProcess exec = QueryProcess.create(graph);
        String query = "select * where {?x ?p ?y}";
        Mappings map = null;
        try {
            map = exec.query(query);
        } catch (EngineException e) {
            e.printStackTrace();
        }
        ResultFormat f1 = ResultFormat.create(map);
        textComponent.setText("Result = "+f1);
    }
}
