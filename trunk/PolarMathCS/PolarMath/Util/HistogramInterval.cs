using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Util
{
    public class HistogramInterval
    {
        private int start;
        private int end;
        private int number = 0;
        private LinkedList<int> values = new LinkedList<int>();

        public HistogramInterval(int start, int end)
        {
            this.start = start;
            this.end = end;
        }

        public int getStart()
        {
            return start;
        }

        public int getEnd()
        {
            return end;
        }

        public int getNumber()
        {
            return number;
        }

        public void add(int interval)
        {
            this.number++;
            this.values.AddLast( interval );
        }

        public LinkedList<int> getValues()
        {
            return this.values;
        }

        public String toString()
        {
            return new StringBuilder( "(" )
                    .Append( start )
                    .Append( ", " )
                    .Append( end )
                    .Append( ") = " )
                    .Append( number ).ToString();
        }
    }
}
