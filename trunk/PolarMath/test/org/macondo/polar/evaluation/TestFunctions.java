package org.macondo.polar.evaluation;

import org.testng.annotations.Test;
import org.testng.Assert;
import static org.macondo.polar.evaluation.Functions.*;

/**
 * <p></p>
 *
 * Date: 23.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class TestFunctions {
    @Test(groups = "fft")
    public void testReverseBits_0_0() {
        Assert.assertEquals(reverseBits(0, 5), 0);
    }

    @Test(groups = "fft")
    public void testReverseBits_1_2_2() {
        Assert.assertEquals(reverseBits(1, 2), 2);
    }

    @Test(groups = "fft")
    public void testReverseBits_6_3_3() {
        Assert.assertEquals(reverseBits(6, 3), 3);
    }

    @Test(groups = "fft")
    public void testReverseBits_5_3_5() {
        Assert.assertEquals(reverseBits(5, 3), 5);
    }

    @Test(groups = "fft")
    public void testReverseBitsTwice_1_3_1() {
        Assert.assertEquals(reverseBits(reverseBits(1, 3), 3), 1);
    }

    @Test(groups = "fft")
    public void testFindLength_3_2() {
        Assert.assertEquals(findLength(3), 2);
    }

    @Test(groups = "fft")
    public void testFindLength_5_3() {
        Assert.assertEquals(findLength(5), 3);
    }

    @Test(groups = "fft")
    public void testFindLength_8_4() {
        Assert.assertEquals(findLength(8), 4);
    }

    @Test(groups = "fft")
    public void testReverse_4_success() {
        Integer[] reverseable = new Integer[]{0,1,2,3};
        reverse(reverseable);
        Assert.assertEquals(reverseable[1], Integer.valueOf(2));
        Assert.assertEquals(reverseable[2], Integer.valueOf(1));
    }

    @Test(groups = "fft")
    public void testReverse_8_success() {
        Integer[] reversable = new Integer[] {0,1,2,3,4,5,6,7};
        reverse(reversable);
        Assert.assertEquals(reversable[0], Integer.valueOf(0));
        Assert.assertEquals(reversable[1], Integer.valueOf(4));
        Assert.assertEquals(reversable[2], Integer.valueOf(2));
        Assert.assertEquals(reversable[3], Integer.valueOf(6));
        Assert.assertEquals(reversable[4], Integer.valueOf(1));
        Assert.assertEquals(reversable[5], Integer.valueOf(5));
        Assert.assertEquals(reversable[6], Integer.valueOf(3));
        Assert.assertEquals(reversable[7], Integer.valueOf(7));
    }

    @Test(groups = "fft")
    public void testReverse_16_success() {
        Integer[] reversable = new Integer[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        reverse(reversable);
        Assert.assertEquals(reversable[2], Integer.valueOf(4));

    }
}
