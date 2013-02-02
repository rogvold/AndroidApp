using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Web.UI.DataVisualization.Charting;

namespace BLELib
{
    public class BLEFilter
    {
        private static bool TwoSideTest(List<ushort> source, double alpha)
        {
            Chart ch = new Chart();
            double avg;
            double sum = 0;
            foreach (ushort element in source)
            {
                sum += element;
            }
            avg = sum / (double)source.Count;
            double std;
            sum = 0;
            foreach (ushort element in source)
            {
                sum += Math.Pow(element - avg, 2);
            }
            std = Math.Pow(sum / (double)(source.Count - 1), 0.5);
            double max = 0;
            foreach (ushort element in source)
            {
                if (Math.Abs(element - avg) > max)
                {
                    max = Math.Abs(element - avg);
                }
            }
            double tValue = ((source.Count - 1) / Math.Pow(source.Count, 0.5)) * 
                Math.Pow(Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(2 * source.Count), source.Count - 2), 2) / 
                (source.Count - 2 + Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(2 * source.Count), source.Count - 2), 2)), 0.5);
            return (max / std) < tValue;
        }

        private static bool MaxTest(List<ushort> source, double alpha)
        {
            Chart ch = new Chart();
            double avg;
            double sum = 0;
            foreach (ushort element in source)
            {
                sum += element;
            }
            avg = sum / (double)source.Count;
            double std;
            sum = 0;
            foreach (ushort element in source)
            {
                sum += Math.Pow(element - avg, 2);
            }
            std = Math.Pow(sum / (double)(source.Count - 1), 0.5);
            double max = 0;
            foreach (ushort element in source)
            {
                if (element > max)
                {
                    max = element;
                }
            }
            double tValue = ((source.Count - 1) / Math.Pow(source.Count, 0.5)) *
                Math.Pow(Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(source.Count), source.Count - 2), 2) /
                (source.Count - 2 + Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(source.Count), source.Count - 2), 2)), 0.5);
            return ((max - avg)/ std) < tValue;
        }

        private static bool MinTest(List<ushort> source, double alpha)
        {
            Chart ch = new Chart();
            double avg;
            double sum = 0;
            foreach (ushort element in source)
            {
                sum += element;
            }
            avg = sum / (double)source.Count;
            double std;
            sum = 0;
            foreach (ushort element in source)
            {
                sum += Math.Pow(element - avg, 2);
            }
            std = Math.Pow(sum / (double)(source.Count - 1), 0.5);
            double min = source[0];
            foreach (ushort element in source)
            {
                if (element < min)
                {
                    min = element;
                }
            }
            double tValue = ((source.Count - 1) / Math.Pow(source.Count, 0.5)) *
                Math.Pow(Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(source.Count), source.Count - 2), 2) /
                (source.Count - 2 + Math.Pow(ch.DataManipulator.Statistics.InverseTDistribution(alpha / (double)(source.Count), source.Count - 2), 2)), 0.5);
            return ((avg - min) / std) < tValue;
        }


        public static List<ushort> IntervalFilter(List<ushort> source)
        {
            double alpha = 0.05;
            while (!MaxTest(source, alpha))
            {
                source.Remove(source.Max());
            }
            while (!MinTest(source, alpha))
            {
                source.Remove(source.Min());
            }
            return source;
        }
    }
}
