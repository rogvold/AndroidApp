using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using PolarMath.Data;

namespace PolarMath
{
    public interface Evaluation<T>
    {
        T evaluate(Training training);
    }
}
