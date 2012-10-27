using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HeartRateMonitor.Math.Evaluation;
using HeartRateMonitor.Math.Evaluation.Geometry;

namespace HeartRateMonitor.Math
{
    class Program
    {
        static void Main(string[] args)
        {
            var intervals = new List<int>();
            var rand = new Random();
            for (int i = 0; i < 100; i++)
            {
                intervals.Add(rand.Next(700, 1000));
            }

            foreach(int interval in intervals)
            {
                Console.Write(interval + " ");
            }
            Console.WriteLine();

            var session = new SessionData()
                {
                    Intervals = intervals
                };
            Console.WriteLine("Average: " + session.Evaluate(new Average()));
            Console.WriteLine("SDNN: " + session.Evaluate(new SDNN()));
            Console.WriteLine("RMSSD: " + session.Evaluate(new RMSSD()));
            Console.WriteLine("PNN50: " + session.Evaluate(new PNN50()));
            Console.WriteLine("CV: " + session.Evaluate(new CV()));
            Console.WriteLine(session.Evaluate(new EvaluateBasicHistogram()).ToString());
            Console.ReadLine();
        }
    }
}
