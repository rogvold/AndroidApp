using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Util
{
    public class Periodogram
    {
        public double Frequency;
        public double Value;

        public Periodogram(double frequency, double value)
        {
            Frequency = frequency;
            Value = value;
        }
    }
}
