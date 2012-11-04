package org.macondo.polar.evaluation;

/**
 * <p>Some basic functions for FFT</p>
 *
 * Date: 23.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
class Functions {
    private Functions() {
    }

    static int reverseBits(int src, int length) {
        int out = 0;
        for (int i = 0; i < length; i++) {
            out <<= 1;
            out += (src>>i) & 1;
        }
        return out;
    }

    static <T> void reverse(T[] data) {
        int maskLength = findLength(data.length - 1);
        for (int i = 0; i < data.length/2; i++) {
            T t = data[i];
            T w = t;
            final int complementaryIndex = reverseBits(i, maskLength);
            if (i > complementaryIndex) {
                continue;
            }
            data[i] = data[complementaryIndex];
            data[complementaryIndex] = w;
        }
    }

    static void reversePaired(double[] data) {
        int maskLength = findLength(data.length - 1) -1;
        for (int i = 0; i < data.length/4; i++) {
            final int idx = i << 1;
            double t1 = data[idx];
            double t2 = data[idx + 1];
            final int complementaryIndex = reverseBits(i, maskLength);
            final int cIdx = complementaryIndex << 1;
            data[idx] = data[cIdx];
            data[idx + 1] = data[cIdx + 1];
            data[cIdx] = t1;
            data[cIdx + 1] = t2;
        }
    }

    static int findLength(int value) {
        int power2 = 1;
        int power = 1;
        while (power2 <= value) {
            power++;
            power2 *= 2;
        }
        return power - 1;
    }
}
