using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.HRV
{
    public sealed class SI : IEvaluation
    {
        public Index Name
        {
            get { return Index.SI; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var mxdmn = Convert.ToDouble(training.Evaluate( new MxDMn() ));
            var amo = Convert.ToDouble(training.Evaluate( new AMoPercents() ));
            var mo = Convert.ToDouble(training.Evaluate( new Mo() ));

            return (int) (amo / (2 * mxdmn * mo));
        }
    }
}
