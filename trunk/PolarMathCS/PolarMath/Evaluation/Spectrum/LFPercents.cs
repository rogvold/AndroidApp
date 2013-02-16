using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    public class LFPercents : Evaluation<double>
    {
        public double evaluate(Training training)
        {
            double lf = training.evaluate( new LF() );
            double tp = training.evaluate( new TP() );
            return (lf / tp) * 100;
        }
    }
}
