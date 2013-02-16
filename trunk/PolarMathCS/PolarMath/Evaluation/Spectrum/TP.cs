using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Util;

namespace PolarMath.Spectrum
{
    public class TP : Evaluation<double>
    {
        public Double evaluate(Training training)
        {
            //List<Periodogram> periodogram = training.evaluate(new FFT());
            List<Periodogram> periodogram1 = training.evaluate( new Lomb() );
            return new Square( periodogram1, 0, 0.4 ).Calculate();
        }
    }
}
