package org.macondo.polar.ui.hr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import org.macondo.polar.data.Training;
import org.macondo.polar.util.TimedValue;

/**
 * <p>Panel for displaying graphs</p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class GraphDisplayPanel extends JPanel {
    private static final Comparator<TimedValue<Integer>> COMPARATOR = new TimedValueTimeComparator();

    private List<TimedValue<Integer>> values;
    private List<TimedValue<Integer>> displayValues;
    private TimedValue<Integer> min = new TimedValue<Integer>(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private TimedValue<Integer> max = new TimedValue<Integer>(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private TimedValue<Integer> origin = new TimedValue<Integer>(0, 0);

    private int availableWidth;
    private int availableHeight;
    private double xFactor;
    private double yFactor;
    private int bottomPadding;
    private int leftPadding;
    private Integer selectionStarts;
    private Integer selectionEnds;
    private boolean inSelectionMode;
    private final int[] intervalCandidates = new int[2];
    private boolean ctrlDown;

    public GraphDisplayPanel() {
        setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                inSelectionMode = true;
                intervalCandidates[0] = (int) e.getPoint().getX();
            }

            public void mouseReleased(MouseEvent e) {
                inSelectionMode = false;
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && (e.getX() - selectionStarts) * (e.getX() - selectionEnds) < 0) {
                    int index1 = getIndexForTime(selectionStarts);
                    int index2 = getIndexForTime(selectionEnds);
                    setDisplayValues(displayValues.subList(
                            Math.min(index1, index2),
                            Math.max(index1, index2)
                    ));
                } else if (e.getClickCount() == 1 && ctrlDown) {
                    setDisplayValues(values);
                } else if (e.getClickCount() == 1 && selectionStarts != null && selectionEnds != null) {
                    int clickPointX = e.getPoint().x;
                    if ((clickPointX - selectionStarts) * (clickPointX - selectionEnds) > 0) {
                        selectionStarts = selectionEnds = null;
                    }
                    repaint();
                }
            }
        });                                                    

        addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                selectionStarts = intervalCandidates[0];
                if (inSelectionMode) {
                    selectionEnds = (int) e.getPoint().getX();
                    repaint();
                }
            }
        });

        setFocusable(true);
        setFocusTraversalKeysEnabled(true);
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    ctrlDown = true;
                }
            }

            public void keyReleased(KeyEvent e) {
                ctrlDown = false;
            }
        });
    }

    public GraphDisplayPanel(Training training) {
        setValues(TimedValue.convertList(training.getIntervals()));
    }

    private void analyseValues() {
        Collections.sort(displayValues, COMPARATOR);
        min.setTime(displayValues.get(0).getTime());
        max.setTime(displayValues.get(displayValues.size() - 1).getTime());

        for (TimedValue<Integer> value : displayValues) {
            if (min.getValue() > value.getValue()) {
                min.setValue(value.getValue());
            } else if (max.getValue() < value.getValue()) {
                max.setValue(value.getValue());
            }
        }

        availableWidth = getWidth() - 2 * leftPadding;
        availableHeight = getHeight() - 40;

        xFactor = ((double) availableWidth) / (max.getTime() - min.getTime());
        yFactor = ((double) availableHeight) / (max.getValue() - min.getValue());

        if (min.getValue() < 0 && max.getValue() > 0) {
            bottomPadding = 20;
            origin.setValue(20 + (int) (yFactor * max.getValue()));
        } else {
            bottomPadding = 30;
            origin.setValue(getHeight() - 20);
        }

        leftPadding = 5;
        if (min.getTime() < 0 && max.getTime() > 0) {
            origin.setTime(leftPadding - (int) (xFactor * min.getValue()));
        } else {
            origin.setTime(20);
        }

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        if (displayValues != null) {
            display(g);
        }
    }

    private void drawBackground(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getSize().width, getSize().height);
        if (selectionStarts != null && selectionEnds != null) {
            g.setColor(Color.lightGray);
            g.fillRect(Math.min(selectionStarts, selectionEnds), 0, Math.abs(selectionEnds - selectionStarts), getSize().height);
        }
    }

    private void display(Graphics g) {
        drawAxis(g);
        g.setColor(Color.BLUE);
        Point pp = getPoint(displayValues.get(0));
        for (int i = 1; i < displayValues.size(); i++) {
            Point p = getPoint(displayValues.get(i));
            g.drawLine(pp.x, pp.y, p.x, p.y);
            pp = p;
        }
    }

    private void drawAxis(Graphics g) {
        g.setColor(Color.RED);
        g.drawLine(0, origin.getValue(), getWidth(), origin.getValue());
        g.drawLine(origin.getTime(), 0, origin.getTime(), getHeight());
    }

    private Point getPoint(TimedValue<Integer> value) {
        return new Point(
                leftPadding + (int) (xFactor * (value.getTime() - min.getTime())),
                getHeight() - bottomPadding - (int) (yFactor * (value.getValue() - min.getValue()))
        );
    }

    private int getIndexForTime(int timeX) {
        int time = getGlobalTFromClick(timeX);
        ListIterator<TimedValue<Integer>> li = displayValues.listIterator();
        int returnIndex = 0;
        while (time > li.next().getTime()) {
            returnIndex = li.nextIndex() - 1;
        }
        return returnIndex;
    }

    private int getGlobalTFromClick(int x) {
        return (int) ((x - leftPadding) / xFactor + min.getTime());
    }

    public void setValues(List<TimedValue<Integer>> values) {
        this.values = values;
        setDisplayValues(this.values);
    }

    public void setDisplayValues(List<TimedValue<Integer>> displayValues) {
        this.displayValues = displayValues;
        selectionStarts = null;
        selectionEnds = null;
        analyseValues();
        repaint();
    }

    private static final class TimedValueTimeComparator implements Comparator<TimedValue<Integer>> {
        public int compare(TimedValue o1, TimedValue o2) {
            return Integer.valueOf(o1.getTime()).compareTo(o2.getTime());
        }
    }
}
