package fr.inria.corese.protege.mappingsviewer;

import fr.inria.corese.gui.query.MyJPanelQuery;
import fr.inria.corese.kgram.api.core.Node;
import fr.inria.corese.kgram.core.Mapping;
import fr.inria.corese.kgram.core.Mappings;
import fr.inria.corese.kgram.core.Query;
import fr.inria.corese.sparql.api.IDatatype;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TableViewer extends JTable {
    private static final Logger logger = LogManager.getLogger(TableViewer.class.getName());
    private final int MAXRES = 10000;
    private Mappings mappings;

    public TableViewer() {
        super();
        setModel(new DefaultTableModel());
        this.setPreferredScrollableViewportSize(this.getPreferredSize());
        this.setFillsViewportHeight(true);
    }

    public void setMappings(Mappings mappings) {
        this.mappings = mappings;
    }

    public void updateModel() {
        Query q = mappings.getQuery();
        List<Node> vars = q.getSelect();
        if (q.isUpdate() && mappings.size() > 0){
            vars = mappings.get(0).getQueryNodeList();
        }
        DefaultTableModel model = new DefaultTableModel();

        int size = Math.min(MAXRES, mappings.size());

        String[] col = new String[size];
        for (int i = 0; i<size; i++){
            col[i] = Integer.toString(i+1);
        }
        model.addColumn("num", col);

        for (Node var : vars) {
            String columnName = var.getLabel();
            //System.out.println(sv);
            String[] colmunData = new String[size];

            for (int j = 0; j < mappings.size(); j++) {
                if (j >= MAXRES){
                    logger.warn("Stop display after " + MAXRES + " results out of " + mappings.size());
                    break;
                }
                Mapping m = mappings.get(j);
                Node value = m.getNode(columnName);

                if (value != null) {
                    IDatatype dt = (IDatatype) value.getValue();
                    colmunData[j] = pretty(dt);
                }
            }
            model.addColumn(columnName, colmunData);
        }

        setModel(model);
    }
    String pretty(IDatatype dt) {
        if (dt.isList()) {
            return dt.getValues().toString();
        } else if (dt.isPointer()) {
            return dt.getPointerObject().toString();
        } else if (dt.isLiteral()) {
            if (dt.getCode() == IDatatype.STRING || (dt.getCode() == IDatatype.LITERAL && ! dt.hasLang())){
                return dt.stringValue();
            }
            return dt.toString();
        }
        else if (dt.isURI()){
            return dt.toString();
        }
        else {
            return dt.getLabel();
        }
    }
}
