using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.HRV
{
    public class SI : IEvaluation<int>
    {
        public int Evaluate(SessionData training)
        {
            var mxdmn = training.Evaluate( new MxDMn() );
            var amo = training.Evaluate( new AMoPercents() );
            var mo = training.Evaluate( new Mo() );

            return (int) (amo / (2 * mxdmn * mo));
        }
    }
}
