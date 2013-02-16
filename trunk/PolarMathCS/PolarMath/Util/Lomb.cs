using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Util
{
    public class Lomb : Evaluation<List<Periodogram>>
    {
        private int size;

        public List<Periodogram> evaluate(Training training)
        {
            List<int> intervals = training.getIntervals();

            size = intervals.Count;

            List<Double> freqValues = new List<Double>();
            List<Double> intervs = new List<Double>();
            List<Double> timeValues = new List<Double>();

            double time = 0;
            //int endIndex = size;
            for (int i = 0; i < size; i++)
            {
                /*if (time > 300) {
                    endIndex = i;
                    break;
                }*/
                time += intervals.ElementAt( i ) / (double) 1000;
                timeValues.Add( time );
                intervs.Add( intervals.ElementAt( i ) / (double) 1000 );
            }

            double maxFreq = 0.5 / (intervals.Min() / (double) 1000);

            //size = endIndex;

            List<String> str = new List<String>();

            double[] intervalValues = new double[size];
            for (int i = 0; i < size; i++)
            {
                intervalValues[i] = intervs.ElementAt( i );
                str.Add( timeValues.ElementAt( i ) + " " + intervalValues[i] );
            }

            int freqSize = 0;
            double freqTotal = 0;

            while (freqTotal <= maxFreq)
            {
                freqValues.Add( freqSize / (double) 1024 );
                freqSize++;
                freqTotal += 1 / (double) 1024;
            }

            double var = standardDeviation( intervalValues );
            intervalValues = subtractMean( intervalValues );

            List<Periodogram> periodogram = new List<Periodogram>();

            for (int i = 0; i < freqSize; i++)
            {
                double w = 2 * Math.PI * freqValues.ElementAt( i );

                if (w > 0)
                {
                    double sinSum = 0;
                    double cosSum = 0;

                    for (int j = 0; j < size; j++)
                    {
                        sinSum += Math.Sin( 2 * w * timeValues.ElementAt( j ) );
                        cosSum += Math.Cos( 2 * w * timeValues.ElementAt( j ) );
                    }

                    double tau = Math.Atan( sinSum / cosSum ) / 2 / w;

                    double sinHigh = 0;
                    double sinLow = 0;
                    double cosHigh = 0;
                    double cosLow = 0;

                    for (int j = 0; j < size; j++)
                    {
                        sinHigh += intervalValues[j] * Math.Sin( w * (timeValues.ElementAt( j ) - tau) );
                        cosHigh += intervalValues[j] * Math.Cos( w * (timeValues.ElementAt( j ) - tau) );
                        cosLow += Math.Pow( Math.Cos( w * (timeValues.ElementAt( j ) - tau) ), 2 );
                        sinLow += Math.Pow( Math.Sin( w * (timeValues.ElementAt( j ) - tau) ), 2 );
                    }
                    periodogram.Add( new Periodogram( freqValues.ElementAt( i ), (1 / (2 * var * var) *
                            (sinHigh * sinHigh / sinLow + cosHigh * cosHigh / cosLow)) ) );
                }
            }

            double tp = 0;

            for (int i = 0; i < freqSize - 1; i++)
            {
                tp += (1 / (double) 1024) * periodogram.ElementAt( i ).getValue();
            }

            return periodogram;
        }

        public double[] subtractMean(double[] values)
        {
            int n = values.Count();
            double m = expectedValue(values);
            double[] valuesMinusExpectedValue = new double[n];
            for (int i = 0; i < n; i++)
                valuesMinusExpectedValue[i] = values[i] - m;
            return valuesMinusExpectedValue;
        }

        public double standardDeviation(double[] values)
        {
            double sigma = 0;
            double m = expectedValue( values );
            int n = values.Count();
            for (int i = 0; i < n; i++)
            {
                sigma += Math.Pow(values[i] - m, 2);
            }
            return Math.Sqrt(sigma / (double)(n - 1));
        }

        public double expectedValue(double[] values)
        {
            double m = 0;
            int n = values.Count();
            for (int i = 0; i < n; i++)
            {
                m += values[i];
            }
            return m / (double)n;
        }
    }
}
