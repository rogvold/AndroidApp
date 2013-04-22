using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ClientServerInteraction.WinRT
{
    public sealed class Session
    {
        // Constants section

        private const int _sleep = 1;
        private const int _rest = 2;
        private const int _work = 3;
        private const int _training = 4;

        public static int Sleep
        {
            get { return _sleep; }
        }

        public static int Rest
        {
            get { return _rest; }
        }

        public static int Work
        {
            get { return _work; }
        }

        public static int Training
        {
            get { return _training; }
        }

        // main section

        public string IdString { get; set; }

        public string UserId { get; set; }

        public long StartTimestamp { get; set; }

        public string Info { get; set; }

        public string /* int */ DeviceId { get; set; }

        public string DeviceName { get; set; }

        public IList<int> Intervals { get; set; }

        public IList<int> Rates { get; set; }

        public int Activity { get; set; }

        public int HealthState { get; set; }

        // Evaluations section

        public int AMoPercents { get; set; }

        public double Mo { get; set; }

        public int[] RSAI { get; set; }

        public double HF { get; set; }

        public double HFPercents { get; set; }

        public double IC { get; set; }

        public double LF { get; set; }

        public double LFPercents { get; set; }

        public double TP { get; set; }

        public double ULF { get; set; }

        public double ULFPercents { get; set; }

        public double VLF { get; set; }

        public double VLFPercents { get; set; }

        public int Average { get; set; }

        public int RMSSD { get; set; }

        public int SDNN { get; set; }

        public int PNN50 { get; set; }

        
    }
}
