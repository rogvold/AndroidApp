using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HrmMath.Data;

namespace HrmMath.Util
{
    public sealed class Filter
    {
        /// <summary>
        /// Used for deleting artifacts from the intervals list.\n
        /// This mysterious algorithm is taken from http://ntpo.com/patents_medicine/medicine_19/medicine_324.shtml
        /// </summary>
        /// <param name="training"></param>
        /// <returns>Filtrated interval list(can contain less numbers)</returns>
        public static IList<int> Filtrate(SessionData training)
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
                    else if (current.Next.Value / (double)current.Previous.Value > 1.2)
                    {
                        countOfPVC++;
                        var nextnextValue = current.Next.Next == null ? current.Previous.Value : current.Next.Next.Value;
                        current.Value = current.Next.Value = (current.Previous.Value + nextnextValue) / 2;
                    }
                }
                else
                {
                    var val1 = current.Value / (double)current.Previous.Value;
                    var val2 = Math.Truncate(val1);
                    if (val1 >= 1.9)
                        if (Math.Abs(val1 - val2) < 0.2)
                        {
                            for (var j = 0; j < val2 - 1; j++)
                                intervals.AddAfter(current, (int)(current.Value / val2));
                            current.Value = (int)(current.Value / val2);
                        }
                        else
                        {
                            current = current.Previous;
                            intervals.Remove(current.Next);
                        }
                }
                current = current.Next;
            }
            return new List<int>(intervals);
        }
    }
}
