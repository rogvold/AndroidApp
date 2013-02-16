using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Spectrum
{
    public class IC : Evaluation<double>
    {
        public double evaluate(Training training)
        {
            double hf = training.evaluate( new HF() );
            double lf = training.evaluate( new LF() );
            double vlf = training.evaluate( new VLF() );

            return (lf + vlf) / hf;
        }
    }
}
