package fr.inria.corese.protege.view;

import java.awt.*;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreseView extends AbstractOWLViewComponent {

    private static final Logger log = LoggerFactory.getLogger(CoreseView.class);

    private Editor metricsComponent;

    @Override
    protected void initialiseOWLView() throws Exception {
//        setLayout(new GridLayout(0,1));
        setLayout(new BorderLayout());

        metricsComponent = new Editor(getOWLModelManager());
        add(metricsComponent, BorderLayout.CENTER);
        log.info("Example View Component initialized");
    }

    @Override
    protected void disposeOWLView() {
        metricsComponent.dispose();
    }
}
