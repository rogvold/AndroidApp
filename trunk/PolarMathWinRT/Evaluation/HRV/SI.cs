using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.HRV
{
    internal sealed class SI : IEvaluation
    {
        public Index Name
        {
            get { return Index.SI; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var mxdmn = (double)training.Evaluate( new MxDMn() );
            var amo = (double)training.Evaluate( new AMoPercents() );
            var mo = (double)training.Evaluate( new Mo() );

            return (int) (amo / (2 * mxdmn * mo));
        }
    }
}
