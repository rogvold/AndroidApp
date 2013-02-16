using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.HRV
{
    public class IN : Evaluation<int>
    {
        public int evaluate(Training training)
        {
            double bp = training.evaluate( new BP() );
            int amo = training.evaluate( new AMoPercents() );
            double mo = training.evaluate( new Mo() );

            return (int) (amo / (2 * bp * mo));
        }
    }
}
