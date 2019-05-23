package fr.inria.corese.protege.mappingsviewer;

import fr.inria.corese.kgram.core.Mapping;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.sparql.triple.parser.ASTQuery;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class GraphViewer extends JPanel implements MappingsViewerInterface {
    private Mappings mappings;
    private JTree treeResult;
    private final JScrollPane scrollPaneTreeResult = new JScrollPane();
    private DefaultMutableTreeNode root;

    public GraphViewer() {
        super();
        buildTreeResult();
    }

    private void buildTreeResult() {
        root = new DefaultMutableTreeNode("root");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        treeResult = new JTree(treeModel);
        treeResult.setShowsRootHandles(true);
        TreePath myPath = treeResult.getPathForRow(0);
        treeResult.expandPath(myPath);
        scrollPaneTreeResult.setViewportView(treeResult);
        add(scrollPaneTreeResult);
    }

    @Override
    public void setMappings(Mappings mappings) {
        this.mappings = mappings;
    }
    @Override
    public void updateModel() {
        int i = 1;
        for (Mapping res : mappings) {
            DefaultMutableTreeNode x = new DefaultMutableTreeNode("result " + i);
            // Pour chaque variable du résultat on ajoute une feuille contenant le nom de la variable et sa valeur

            for (fr.inria.corese.kgram.api.core.Node var : mappings.getSelect()) {
                fr.inria.corese.kgram.api.core.Node node = res.getNode(var);
                if (node != null) {
                    x.add(new DefaultMutableTreeNode(var.getLabel()));
                    x.add(new DefaultMutableTreeNode(node.getValue().toString()));
                    root.add(x);
                }
            }
            i++;
        }
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
