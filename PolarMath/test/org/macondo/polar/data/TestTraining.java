package org.macondo.polar.data;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.macondo.polar.evaluation.Average;
import org.macondo.polar.evaluation.SDNN;

import java.io.InputStream;
import java.io.IOException;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class TestTraining {
    protected Training t;

    @Test(groups = "basic")
    public void testReadTraining() throws IOException {
        InputStream is = TestTraining.class.getResourceAsStream("testData.txt");
        t = new TrainingReader().readTraining(is);
        Assert.assertEquals(6, t.getIntervals().size());
    }

    @Test(groups = "basic", dependsOnMethods = "testReadTraining")
    public void testAverage() {
        Average a = new Average();
        Integer average = t.evaluate(a);
        Assert.assertEquals(average, Integer.valueOf(400));
    }

    @Test(groups = "basic", dependsOnMethods = "testReadTraining")
    public void testEvaluationCaching() {
        Average a = new Average();
        Average b = new Average();
        Integer ia = t.evaluate(a);
        Integer ib = t.evaluate(b);
        Assert.assertTrue(ia == ib);
    }

    @Test(groups = "basic", dependsOnMethods = "testReadTraining")
    public void testMeanRootSquereDeviation() {
        SDNN evaluation = new SDNN();
        Integer result = t.evaluate(evaluation);
        Assert.assertEquals(result, Integer.valueOf(81));
    }
}
