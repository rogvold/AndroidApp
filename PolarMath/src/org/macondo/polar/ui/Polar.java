package org.macondo.polar.ui;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JFrame;

import org.macondo.polar.ui.menu.PolarMenu;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Polar {
    public static void main(String[] args) throws IOException {
        JFrame f = new JFrame("Polar - Together to Healthy Future");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setJMenuBar(new PolarMenu());
        f.getContentPane().setLayout(new BorderLayout());

        pui = new PolarUI();

        f.setSize(800, 600);
        f.setVisible(true);
        f.setContentPane(pui.getGraphPanel());
    }

    private static PolarUI pui;

    public static PolarUI getPolarUI() {
        return pui;
    }
}
