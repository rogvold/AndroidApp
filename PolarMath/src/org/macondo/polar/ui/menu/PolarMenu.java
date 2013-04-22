package org.macondo.polar.ui.menu;

import static org.macondo.polar.ui.menu.PolarMenu.menuResources;

import javax.swing.*;
import java.util.ResourceBundle;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class PolarMenu extends JMenuBar {
    static final ResourceBundle menuResources = ResourceBundle.getBundle("org.macondo.polar.ui.menu.menu");

    public PolarMenu() {
        super();
        add(new FileMenu());
    }
}

class FileMenu extends JMenu {
    public FileMenu() {
        super(menuResources.getString("menu.file"));
        add(new OpenFileAction(menuResources.getString("menu.file.open"), this));
        add(new DisplayFourierTransoformAction(menuResources.getString("menu.file.fourier")));
        add(new AbstractAction(menuResources.getString("menu.file.exit")){
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }
}
