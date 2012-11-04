package org.macondo.polar.ui.menu;

import org.macondo.polar.ui.Polar;
import org.macondo.polar.data.Training;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;

/**
 * <p></p>
 *
 * Date: 16.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class OpenFileAction extends AbstractAction {
    private Component parent;

    public OpenFileAction(String name, Component parent) {
        super(name);
        this.parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".hrm");
            }

            public String getDescription() {
                return "Polar HRM";
            }
        });
        fc.showOpenDialog(parent);
        if (fc.getSelectedFile() == null) {
            return;
        }
        try {
            Polar.getPolarUI().setTraining(Training.readTraining(new FileInputStream(fc.getSelectedFile())));
            Polar.getPolarUI().getGraphPanel().repaint();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(fc, "Unable to open file");
        }
    }
}
