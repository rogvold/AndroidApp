using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    public class VLFPercents : Evaluation<double>
    {
        public Double evaluate(Training training)
        {
            double vlf = training.evaluate( new VLF() );
            double tp = training.evaluate( new TP() );
            return (vlf / tp) * 100;
        }
    }
}
