using PolarMath.Data;
using PolarMath.Util;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    public class LF : Evaluation<double>
    {
        public double evaluate(Training training)
        {
            List<Periodogram> periodogram = training.evaluate( new Lomb() );

            return new Square( periodogram, 0.04, 0.15 ).Calculate();
        }
    }
}
