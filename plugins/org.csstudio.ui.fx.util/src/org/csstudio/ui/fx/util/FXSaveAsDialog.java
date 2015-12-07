package org.csstudio.ui.fx.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SaveAsDialog;

import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 *
 * <code>FXSaveAsDialog</code> is an extension of the {@link SaveAsDialog}, which provides JavaFX buttons.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class FXSaveAsDialog extends SaveAsDialog {

    private Button okButton;

    /**
     * Constructs a new dialog.
     *
     * @param parentShell the parent shell
     */
    public FXSaveAsDialog(Shell parentShell) {
        super(parentShell);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SaveAsDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridLayout) parent.getLayout()).numColumns++;
        new FXCanvasMaker() {
            @Override
            protected Scene createFxScene() {
                okButton = new Button(IDialogConstants.OK_LABEL);
                okButton.setOnAction(e -> buttonPressed(IDialogConstants.OK_ID));
                Button cancelButton = new Button(IDialogConstants.CANCEL_LABEL);
                cancelButton.setOnAction(e -> buttonPressed(IDialogConstants.CANCEL_ID));
                okButton.setPrefWidth(60);
                cancelButton.setPrefWidth(60);

                GridPane pane = new GridPane();
                pane.setHgap(10);
                GridPane.setHgrow(okButton, Priority.ALWAYS);
                GridPane.setHalignment(okButton, HPos.RIGHT);
                pane.add(okButton, 0, 0);
                pane.add(cancelButton, 1, 0);
                return new Scene(pane);
            }
        }.createPartControl(parent);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.dialogs.SaveAsDialog#setDialogComplete(boolean)
     */
    @Override
    protected void setDialogComplete(boolean value) {
        okButton.setDisable(!value);
    }
}
