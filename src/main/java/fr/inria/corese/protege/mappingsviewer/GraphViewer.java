package fr.inria.corese.protege.mappingsviewer;

import fr.inria.corese.core.visitor.ldpath.AST;
import fr.inria.corese.kgram.api.core.ExpType;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.sparql.api.IDatatype;
import fr.inria.corese.sparql.triple.parser.ASTQuery;
import fr.inria.corese.sparql.triple.parser.NSManager;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.springbox.implementations.LinLog;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;

import javax.swing.*;
import java.awt.*;

public class GraphViewer extends JPanel implements MappingsViewerInterface {
    private static final String KGSTYLE = ExpType.KGRAM + "style";
    private Mappings mappings;
    private MultiGraph graph;

    public GraphViewer() {
        super();
        setLayout(new GridLayout(0,1));
    }

    @Override
    public void setMappings(Mappings mappings) {
        this.mappings = mappings;

//

//        JPanel panelStyleGraph = new JPanel();
//        panelStyleGraph.setLayout(new BorderLayout());
//
//        panelStyleGraph.add(textPaneStyleGraph, BorderLayout.CENTER);
//        panelStyleGraph.add(textAreaLinesGraph, BorderLayout.WEST);
//
//        JScrollPane jsStyleGraph = new JScrollPane();
//        jsStyleGraph.setViewportView(panelStyleGraph);
//
//        final JSplitPane jpGraph = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsStyleGraph, sgr);
//        jpGraph.setContinuousLayout(true);
//        scrollPaneTreeResult.setViewportView(jpGraph);

    }

    @Override
    public void updateModel() {
        if (mappings.getQuery().isConstruct()) {
            displayGraph((fr.inria.corese.core.Graph) mappings.getGraph(), ((ASTQuery) mappings.getAST()).getNSM());
        }
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    void displayGraph(fr.inria.corese.core.Graph g, NSManager nsm) {
        graph = create(g, nsm);
//        graph.addAttribute("ui.stylesheet", stylesheet);
//        graph.addAttribute("ui.antialias");
//        textPaneStyleGraph.setText(stylesheet);

        //permet de visualiser correctement le graphe dans l'onglet de Corese
        LinLog lLayout = new LinLog();
        lLayout.setQuality(0.9);
        lLayout.setGravityFactor(0.9);

        Viewer sgv = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_SWING_THREAD);
        sgv.enableAutoLayout(lLayout);
        View sgr = sgv.addDefaultView(false);

        sgr.getCamera().setAutoFitView(true);
        add(sgr);
        invalidate();

    }

    MultiGraph create(fr.inria.corese.core.Graph g, NSManager nsm) {
        //            graph.addNode(temp).addAttribute("ui.style", "fill-color:white;");
        //                gsub.addAttribute("ui.style", "fill-color:lightblue;size-mode:dyn-size;shape:rounded-box;");
        //                    ee.addAttribute("ui.style", "size:0;edge-style:dashes;fill-color:white;");
        int num = 0;
        String sujetUri, predicat, objetUri;

        String sujet;
        String objet;

        MultiGraph graph = new MultiGraph(g.getName(), false, true);

        for (fr.inria.corese.kgram.api.core.Edge ent : g.getEdges()) {
            fr.inria.corese.kgram.api.core.Edge edge = ent.getEdge();
            fr.inria.corese.kgram.api.core.Node n1 = edge.getNode(0);
            fr.inria.corese.kgram.api.core.Node n2 = edge.getNode(1);

            sujetUri = n1.getLabel();
            objetUri = n2.getLabel();

            predicat = getLabel(nsm, edge.getEdgeNode());

            sujet = getLabel(nsm, n1);
            objet = getLabel(nsm, n2);

            Node gsub = graph.getNode(sujetUri);
            if (gsub == null) {
                gsub = graph.addNode(sujetUri);
                gsub.addAttribute("label", sujet);
                style(n1, gsub);
            }
            num++;

            if (isStyle(edge)) {
                // xxx kg:style ex:Wimmics
                // it is a fake edge, do not create it
                gsub.setAttribute("ui.class", objet);
            } else {
                Node gobj = graph.getNode(objetUri);
                if (gobj == null) {
                    gobj = graph.addNode(objetUri);
                    gobj.addAttribute("label", objet);
                    style(n2, gobj);
                }
                num++;

                Edge ee = graph.addEdge("edge" + num, sujetUri, objetUri, true);
                ee.addAttribute("label", predicat);
            }
        }

        return graph;
    }

    private String getLabel(NSManager nsm, fr.inria.corese.kgram.api.core.Node n) {
        IDatatype dt = (IDatatype) n.getValue();
        if (dt.isURI()) {
            return nsm.toPrefix(n.getLabel());
        } else {
            return n.getLabel();
        }
    }

    void style(fr.inria.corese.kgram.api.core.Node n, Node gn) {
        if (n.isBlank()) {
            gn.setAttribute("ui.class", "Blank");
        } else if (n.getDatatypeValue().isLiteral()) {
            gn.setAttribute("ui.class", "Literal");
        }
    }

    private boolean isStyle(fr.inria.corese.kgram.api.core.Edge edge) {
        return edge.getLabel().equals(KGSTYLE);
    }
}

