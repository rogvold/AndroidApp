using System;
using System.Collections.Generic;
using System.Linq;
using System.Web.UI.DataVisualization.Charting;

namespace BLELib
{
    public class BLEFilter
    {
        private static bool TwoSideTest(List<ushort> source, double alpha)
        {
            if (source == null) throw new ArgumentNullException("source");
            var ch = new Chart();
            double sum = source.Aggregate<ushort, double>(0, (current, element) => current + element);
            double avg = sum/source.Count;
            sum = source.Sum(element => Math.Pow(element - avg, 2));
            double std = Math.Pow(sum/(source.Count - 1), 0.5);
            double max = source.Select(element => Math.Abs(element - avg)).Concat(new double[] {0}).Max();
            double tValue = ((source.Count - 1)/Math.Pow(source.Count, 0.5))*
                            Math.Pow(
                                Math.Pow(
                                    ch.DataManipulator.Statistics.InverseTDistribution(alpha/(2*source.Count),
                                                                                       source.Count - 2), 2)/
                                (source.Count - 2 +
                                 Math.Pow(
                                     ch.DataManipulator.Statistics.InverseTDistribution(alpha/(2*source.Count),
                                                                                        source.Count - 2), 2)), 0.5);
            return (max/std) < tValue;
        }

        private static bool MaxTest(List<ushort> source, double alpha)
        {
            var ch = new Chart();
            double sum = source.Aggregate<ushort, double>(0, (current, element) => current + element);
            double avg = sum/source.Count;
            sum = source.Sum(element => Math.Pow(element - avg, 2));
            double std = Math.Pow(sum/(source.Count - 1), 0.5);
            double max = 0;
            foreach (ushort element in source)
            {
                if (element > max)
                {
                    max = element;
                }
            }
            double tValue = ((source.Count - 1)/Math.Pow(source.Count, 0.5))*
                            Math.Pow(
                                Math.Pow(
                                    ch.DataManipulator.Statistics.InverseTDistribution(alpha/(source.Count),
                                                                                       source.Count - 2), 2)/
                                (source.Count - 2 +
                                 Math.Pow(
                                     ch.DataManipulator.Statistics.InverseTDistribution(alpha/(source.Count),
                                                                                        source.Count - 2), 2)), 0.5);
            return ((max - avg)/std) < tValue;
        }

        private static bool MinTest(List<ushort> source, double alpha)
        {
            var ch = new Chart();
            double sum = source.Aggregate<ushort, double>(0, (current, element) => current + element);
            double avg = sum/source.Count;
            sum = source.Sum(element => Math.Pow(element - avg, 2));
            double std = Math.Pow(sum/(source.Count - 1), 0.5);
            ushort min = source[0];
            foreach (ushort element in source)
            {
                if (element < min)
                {
                    min = element;
                }
            }
            double tValue = ((source.Count - 1)/Math.Pow(source.Count, 0.5))*
                            Math.Pow(
                                Math.Pow(
                                    ch.DataManipulator.Statistics.InverseTDistribution(alpha/(source.Count),
                                                                                       source.Count - 2), 2)/
                                (source.Count - 2 +
                                 Math.Pow(
                                     ch.DataManipulator.Statistics.InverseTDistribution(alpha/(source.Count),
                                                                                        source.Count - 2), 2)), 0.5);
            return ((avg - min)/std) < tValue;
        }


        public static List<ushort> IntervalFilter(List<ushort> source)
        {
            if (source.Count < 10)
            {
                return source;
            }
            const double alpha = 0.05;
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