package org.csstudio.saverestore.ui.browser;

import org.csstudio.ui.fx.util.FXMessageDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 *
 * <code>ResetRepository</code> resets the repository to its default state (depends on the repository implementation
 * what that means).
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ResetRepository extends AbstractHandler implements IHandler {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart part = HandlerUtil.getActivePart(event);
        if (part instanceof BrowserView) {
            if (FXMessageDialog.openConfirm(part.getSite().getShell(), "Reset Repository",
                    "Are you sure you want to rese the local repository. You will lose all data that has not been "
                            + "pushed to the central repository")) {
                ((BrowserView) part).getActionManager().resetRepository();
            }
        }
        return null;
    }
}