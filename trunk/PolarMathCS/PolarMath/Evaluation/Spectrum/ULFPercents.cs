using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    class ULFPercents : Evaluation<double>
    {
        public double evaluate(Training training)
        {
            double ulf = training.evaluate( new ULF() );
            double tp = training.evaluate( new TP() );
            return (ulf / tp) * 100;
        }
    }
}
