package org.macondo.polar.evaluation;

/**
 * <p></p>
 *
 * Date: 08.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Harmonics {
    private double frequency;
    private Complex value;

    public Harmonics(double frequency, Complex value) {
        this.frequency = frequency;
        this.value = value;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public Complex getValue() {
        return value;
    }

    public void setValue(Complex value) {
        this.value = value;
    }

    public static Harmonics valueOf(int k, int N, Complex c) {
        return new Harmonics(k/(double)N, c);
    }

    public String toString() {
        return frequency + " " + value;
    }
}
