using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using PolarMath.Data;

namespace PolarMath.Util
{
    public class Filter
    {
        /// <summary>
        /// Used for deleting artifacts from the intervals list.\n
        /// This mysterious algorithm is taken from http://ntpo.com/patents_medicine/medicine_19/medicine_324.shtml
        /// </summary>
        /// <param name="training"></param>
        /// <returns>Filtrated interval list(can contain less numbers)</returns>
        public static List<int> Filtrate(SessionData training)
        {
            var intervals = new LinkedList<int>(training.Intervals);
            var n = intervals.Count;
            var countOfPVC = 0; //Premature Ventricular Contraction
            var current = intervals.First.Next;
            while (current.Next != null)
            {
                if (current.Value / (double)current.Previous.Value < 0.8)
                {
                    if (current.Next.Value / (double)current.Previous.Value > 0.8 &&
                        current.Next.Value / (double)current.Previous.Value < 1.2)
                    {
                        current = current.Next;
                        intervals.Remove(current.Previous);
                    }
                    else
                        if (current.Next.Value / (double)current.Previous.Value > 1.2)
                        {
                            countOfPVC++;
                            var nextnextValue = current.Next.Next == null ? current.Previous.Value : current.Next.Next.Value;
                            current.Value = current.Next.Value = (current.Previous.Value + nextnextValue) / 2;
                        }
                }
                else
                    if (Math.Abs(current.Value / (double) current.Previous.Value - 2) < 0.2)
                    {
                        intervals.AddAfter(current, current.Value / 2);
                        current.Value /= 2;
                    }
            }
            return new List<int>(intervals);
        }
    }
}
