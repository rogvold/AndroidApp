package org.macondo.polar.evaluation;

import org.macondo.polar.data.Training;
import org.macondo.polar.data.TestTraining;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.InputStream;
import java.io.IOException;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class TestMax {
    private Training t;

    @BeforeClass
    public void setUp() throws IOException {
        InputStream is = TestTraining.class.getResourceAsStream("testData.txt");
        t = Training.readTraining(is);
    }

    @Test(groups = "basic")
    public void testMax() {
        Max m = new Max();
        Assert.assertEquals(m.evaluate(t), Integer.valueOf(500));
    }

}
