package org.macondo.polar.evaluation;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.macondo.polar.data.Training;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import static java.lang.Math.*;

/**
 * <p></p>
 *
 * Date: 23.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class TestFFT {
    private FFT fft;

    @BeforeClass(groups = {"fft", "resample"})
    public void setUp() {
        fft = new FFT();
    }

    @Test(groups = "fft")
    public void testPadZeroes_123_00201000() {
        List<Double> items = Arrays.asList(0., 1., 2.);
        Complex[] d = fft.padZeroes(items);
        Assert.assertEquals(d.length, 4);
        Assert.assertEquals(d[0], Complex.valueOf(0.));
        Assert.assertEquals(d[1], Complex.valueOf(2.));
        Assert.assertEquals(d[2], Complex.valueOf(1.));
    }

    @Test(groups = "fft")
    public void testPadZeroes_01234567_0040206010503070() {
        List<Double> items = Arrays.asList(0., 1., 2., 3., 4., 5., 6., 7.);
        Complex[] d = fft.padZeroes(items);
        Assert.assertEquals(d.length, 8);
        Assert.assertEquals(d[0], Complex.valueOf(0.));
        Assert.assertEquals(d[1], Complex.valueOf(4.));
        Assert.assertEquals(d[2], Complex.valueOf(2.));
        Assert.assertEquals(d[3], Complex.valueOf(6.));
        Assert.assertEquals(d[4], Complex.valueOf(1.));
        Assert.assertEquals(d[5], Complex.valueOf(5.));
        Assert.assertEquals(d[6], Complex.valueOf(3.));
        Assert.assertEquals(d[7], Complex.valueOf(7.));
    }

    @Test(groups = "fft")
    public void testPadZeroes_0123456_0040206010503000() {
        List<Double> items = Arrays.asList(0., 1., 2., 3., 4., 5., 6.);
        Complex[] d = fft.padZeroes(items);
        Assert.assertEquals(d.length, 8);
        Assert.assertEquals(d[0], Complex.valueOf(0.));
        Assert.assertEquals(d[1], Complex.valueOf(4.));
        Assert.assertEquals(d[2], Complex.valueOf(2.));
        Assert.assertEquals(d[3], Complex.valueOf(6.));
        Assert.assertEquals(d[4], Complex.valueOf(1.));
        Assert.assertEquals(d[5], Complex.valueOf(5.));
        Assert.assertEquals(d[6], Complex.valueOf(3.));
        Assert.assertEquals(d[7], Complex.valueOf(0.));
    }

    @Test(groups = "fft")
    public void testFFT() {
        List<Integer> values = new LinkedList<Integer>();

        loadValues(values);

        final Training training = new Training();
        training.setIntervals(values);

        printList(values);
        System.out.println("----------------------------------");

        List<Harmonics> harmo = fft.evaluate(training);
        printList(harmo);
    }

    private void loadValues(List<Integer> values) {
/*
        for (int i = 0; i < 16; i++) {
            if (i % 4 == 0) {
                values.add(1);
            } else {
                values.add(0);
            }
        }
*/
        for (int i = 0; i < 10000; i++) {
            if (i % 200 < 40) {
                values.add(1);
            } else {
                values.add(0);
            }
        }
/*
        for (int i = 0; i < 10000; i++) {
            values.add((int) (1000 * Math.cos(0.01 * i)));
        }
*/
    }

    @Test(groups = {"fft", "resample"})
    public void testResample_success() {
        List<Integer> sample = Arrays.asList(5, 4, 8);

        List<Double> resampled = fft.resample(sample, 0);

        Assert.assertEquals(resampled.size(), 7);
        Assert.assertEquals(resampled.toArray(new Double[resampled.size()]), new Double[] {5., 4.5, 4., 5., 6., 7., 8.});
    }

    @Test(groups = {"fft", "resample"})
    public void testResample2_success() {
        List<Integer> sample = Arrays.asList(4, 5, 5);

        List<Double> resampled = fft.resample(sample, 4);

        Assert.assertEquals(resampled.toArray(new Double[resampled.size()]), new Double[] {0., 0.4, 0.8, 1., 1., 1.});
    }

    private static void printList(List<?> list) {
        for (Object o : list) {
            System.out.println(o);
        }
    }
}
