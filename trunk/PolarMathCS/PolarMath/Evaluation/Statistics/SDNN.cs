using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Statistics
{
    public class SDNN : Evaluation<int>
    {
        public int evaluate(Training training) {
        int average = training.evaluate(new Average());
        long total = 0;
        foreach (int integer in training.getIntervals()) {
            total += (average - integer) * (average - integer);
        }
        return (int) Math.Sqrt(total / training.getIntervals().Count);
    }
    }
}
