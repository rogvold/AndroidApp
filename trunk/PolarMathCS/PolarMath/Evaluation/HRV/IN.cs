using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.HRV
{
    public class IN : IEvaluation<int>
    {
        public int Evaluate(Training training)
        {
            var bp = training.Evaluate( new BP() );
            var amo = training.Evaluate( new AMoPercents() );
            var mo = training.Evaluate( new Mo() );

            return (int) (amo / (2 * bp * mo));
        }
    }
}
