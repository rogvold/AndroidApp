using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Text;

namespace PolarMath.Util
{
    public class HistogramInterval
    {
        public int Start { get; private set; }
        public int End { get; private set; }
        public LinkedList<int> Values { get; set; }

        public HistogramInterval(int start, int end)
        {
            Start = start;
            End = end;
            Values = new LinkedList<int>();
        }

        public override String ToString()
        {
            return new StringBuilder( "(" )
                    .Append( Start )
                    .Append( ", " )
                    .Append( End )
                    .Append( ") = " )
                    .Append( Values.Count ).ToString();
        }
    }
}
