package org.macondo.polar.evaluation;

/**
 * <p></p>
 *
 * Date: 08.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public final class Complex {
    public static final Complex I = new Complex(0, 1);
    public static final Complex ZERO = new Complex(0, 0);

    private double real;
    private double imaginary;

    private Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    Complex add(Complex a) {
        return new Complex(this.real + a.real, this.imaginary + a.imaginary);
    }

    Complex sub(Complex a) {
        return new Complex(this.real - a.real, this.imaginary - a.imaginary);
    }

    Complex times(Complex that) {
        return new Complex(this.real * that.real - this.imaginary * that.imaginary,
                this.real * that.imaginary + this.imaginary * that.real);
    }

    public double abs() {
        return Math.sqrt(real * real + imaginary * imaginary);
    }

    public static Complex EXP(Complex a) {
        double realExp = Math.exp(a.real);
        return new Complex(realExp * Math.cos(a.imaginary), realExp * Math.sin(a.imaginary));
    }

    public static Complex EXP(double imaginary) {
        return new Complex(Math.cos(imaginary), Math.sin(imaginary));
    }

    public static Complex valueOf(double real, double imag) {
        return new Complex(real, imag);
    }

    public static Complex valueOf(double real) {
        return new Complex(real, 0);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Complex complex = (Complex) o;

        if (Double.compare(complex.imaginary, imaginary) != 0) return false;
        if (Double.compare(complex.real, real) != 0) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = real != +0.0d ? Double.doubleToLongBits(real) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = imaginary != +0.0d ? Double.doubleToLongBits(imaginary) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String toString() {
        return real + " " + imaginary;
    }

    public double getReal() {
        return real;
    }

    public double getImaginary() {
        return imaginary;
    }
}
