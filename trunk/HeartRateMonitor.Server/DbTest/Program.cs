using System;
using HeartRateMonitor.BusinessLayer.Helpers;

namespace DbTest
{
    class Program
    {

        static void Main(string[] args)
        {

            DBHelper.DropCollection("Users");
            DBHelper.DropCollection("Sessions");

            Console.WriteLine("Import Success");

            Console.ReadKey();

        }

        
    }
}
