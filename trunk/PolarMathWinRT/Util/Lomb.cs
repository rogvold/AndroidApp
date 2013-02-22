using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HrmMath.Evaluation;
using HrmMath.Evaluation.Statistics;

namespace HrmMath.Util
{
    internal sealed class Lomb : IEvaluation
    {
        private int _size;

        public Index Name
        {
            get { return Index.Lomb; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var intervals = training.Intervals;

            _size = intervals.Count;

            var freqValues = new List<Double>();
            var intervs = new List<Double>();
            var timeValues = new List<Double>();

            double time = 0;
            //int endIndex = size;
            for (var i = 0; i < _size; i++)
            {
                /*if (time > 300) {
                    endIndex = i;
                    break;
                }*/
                time += intervals.ElementAt( i ) / (double) 1000;
                timeValues.Add( time );
                intervs.Add( intervals.ElementAt( i ) / (double) 1000 );
            }

            var maxFreq = 0.5 / (intervals.Min() / (double) 1000);

            //size = endIndex;

            var str = new List<String>();

            var intervalValues = new double[_size];
            for (var i = 0; i < _size; i++)
            {
                intervalValues[i] = intervs.ElementAt( i );
                str.Add( timeValues.ElementAt( i ) + " " + intervalValues[i] );
            }

            var freqSize = 0;
            double freqTotal = 0;

            while (freqTotal <= maxFreq)
            {
                freqValues.Add( freqSize / (double) 1024 );
                freqSize++;
                freqTotal += 1 / (double) 1024;
            }

            var sdnn = (double)training.Evaluate(new SDNN());
            intervalValues = SubtractMean( intervalValues , (int)training.Evaluate(new Average()));

            var periodogram = new List<Periodogram>();

            for (var i = 0; i < freqSize; i++)
            {
                var w = 2 * Math.PI * freqValues.ElementAt( i );

                if (w <= 0) 
                    continue;
                double sinSum = 0;
                double cosSum = 0;

                for (var j = 0; j < _size; j++)
                {
                    sinSum += Math.Sin( 2 * w * timeValues.ElementAt( j ) );
                    cosSum += Math.Cos( 2 * w * timeValues.ElementAt( j ) );
                }

                var tau = Math.Atan( sinSum / cosSum ) / 2 / w;

                double sinHigh = 0;
                double sinLow = 0;
                double cosHigh = 0;
                double cosLow = 0;

                for (var j = 0; j < _size; j++)
                {
                    sinHigh += intervalValues[j] * Math.Sin( w * (timeValues.ElementAt( j ) - tau) );
                    cosHigh += intervalValues[j] * Math.Cos( w * (timeValues.ElementAt( j ) - tau) );
                    cosLow += Math.Pow( Math.Cos( w * (timeValues.ElementAt( j ) - tau) ), 2 );
                    sinLow += Math.Pow( Math.Sin( w * (timeValues.ElementAt( j ) - tau) ), 2 );
                }
                periodogram.Add( new Periodogram( freqValues.ElementAt( i ), (1 /(double) (2 * sdnn * sdnn) *
                                                                              (sinHigh * sinHigh / sinLow + cosHigh * cosHigh / cosLow)) ) );
            }

            double tp = 0;

            for (var i = 0; i < freqSize - 1; i++)
            {
                tp += (1 / (double) 1024) * periodogram.ElementAt( i ).Value;
            }

            return periodogram;
        }

        public double[] SubtractMean(double[] values, int m/*expectedValue*/)
        {
            var n = values.Count();
            var valuesMinusExpectedValue = new double[n];
            for (var i = 0; i < n; i++)
                valuesMinusExpectedValue[i] = values[i] - m;
            return valuesMinusExpectedValue;
        }
    }
}
