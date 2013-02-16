using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Util
{
    public class Periodogram
    {
        private double frequency;
        private double value;

        public Periodogram(double frequency, double value)
        {
            this.frequency = frequency;
            this.value = value;
        }

        public double getFrequency()
        {
            return frequency;
        }

        public void setFrequency(double frequency)
        {
            this.frequency = frequency;
        }

        public double getValue()
        {
            return value;
        }

        public void setValue(double value)
        {
            this.value = value;
        }

        public String toString()
        {
            return frequency + " " + value;
        }
    }
}
