using System;
using System.Collections.Generic;
using System.Linq;
using System.Web.UI.DataVisualization.Charting;

namespace BLELib
{
    public class BLEFilter
    {
        private static bool MaxTest(List<ushort> source, double alpha)
        {
            var ch = new Chart();
            var avg = source.Aggregate<ushort, double>(0, (current, element) => current + element) / 
                source.Count;
            var std = Math.Pow(source.Sum(element => Math.Pow(element - avg, 2)) / 
                (source.Count - 1), 0.5);
            var max = 0;
            foreach (var element in source)
            {
                if (element > max)
                {
                    max = element;
                }
            }
            var tValue = ((source.Count - 1)/Math.Pow(source.Count, 0.5))*
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
            var avg = source.Aggregate<ushort, double>(0, (current, element) => current + element) /
                source.Count;
            var std = Math.Pow(source.Sum(element => Math.Pow(element - avg, 2)) /
                (source.Count - 1), 0.5);
            var min = source[0];
            foreach (var element in source)
            {
                if (element < min)
                {
                    min = element;
                }
            }
            var tValue = ((source.Count - 1)/Math.Pow(source.Count, 0.5))*
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