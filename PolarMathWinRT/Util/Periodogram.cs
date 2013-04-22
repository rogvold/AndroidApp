using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Util
{
    public sealed class Periodogram
    {
        internal double Frequency;
        internal double Value;

        public Periodogram(double frequency, double val)
        {
            Frequency = frequency;
            Value = val;
        }
    }
}
