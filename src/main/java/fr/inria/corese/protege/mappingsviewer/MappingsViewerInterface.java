package fr.inria.corese.protege.mappingsviewer;

import fr.inria.corese.kgram.core.Mappings;

import javax.swing.*;

public interface MappingsViewerInterface {
    void setMappings(Mappings mappings);

    void updateModel();

    JComponent getComponent();
}
