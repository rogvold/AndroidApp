using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    public class HFPercents : Evaluation<double>
    {
        public Double evaluate(Training training)
        {
            double hf = training.evaluate( new HF() );
            double tp = training.evaluate( new TP() );
            return (hf / tp) * 100;
        }
    }
}
