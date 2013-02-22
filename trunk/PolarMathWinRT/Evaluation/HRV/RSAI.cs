using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HrmMath.Data;
using HrmMath.Evaluation.Statistics;
using HrmMath.Evaluation.Spectrum;

namespace HrmMath.Evaluation.HRV
{
    /// <summary>
    /// This class is for computation of RSAI(regulatory systems adequacy/activity index).\n
    /// It takes values from 0 to 10. The algorithm is taken from http://ntpo.com/patents_medicine/medicine_19/medicine_324.shtml \n
    /// Before evaluation RR-intervals must be filtered by Util.Filter.Filtrate();
    /// </summary>
    internal sealed class RSAI : IEvaluation
    {
        public Index Name
        {
            get { return Index.RSAI; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var intervals = (List<int>)training.Intervals;
            var average = (double)training.Evaluate( new Average() );
            var sdnn = (double)training.Evaluate( new SDNN() );
            var cv = (double)training.Evaluate( new CV() );
            var mo = (double)training.Evaluate( new Mo() );
            var amo = (double)training.Evaluate( new AMoPercents() );
            var mxdmn = (double)training.Evaluate( new MxDMn() );
            var si = (double)training.Evaluate( new SI() );
            var lfPercents = (double)training.Evaluate( new LFPercents() );
            var hfPercents = (double)training.Evaluate( new HFPercents() );
            var vlfPercents = (double)training.Evaluate( new VLFPercents() ) + (double)training.Evaluate( new ULFPercents() );
            var totalFPercents = lfPercents + vlfPercents + hfPercents;

            short[] h = new short[5] { 0, 0, 0, 0, 0 };
            //now we need to calculate 5 indexes
            //RSAI consists of these 5 indexes

            //h[0]
            //Cumulative effect of regulation
            if (average <= 0.66)
                h[0] = 2;
            else
                if (average <= 0.80)
                    h[0] = 1;
                else
                    if (average >= 0.80 && average <= 1.00)
                        h[0] = 0;
                    else
                        if (average >= 1.00)
                            h[0] = -1;
                        else
                            if (average >= 1.20)
                                h[0] = -2;

            //h[1]
            //function of automatism
            if (sdnn <= 0.02 && mxdmn <= 0.1 && cv <= 2.0)
                h[1] = 2;
            else
                if (sdnn >= 0.10 && mxdmn >= 0.3 && cv >= 8.0)
                    h[1] = 1;
                else
                    if (mxdmn >= 0.45)
                        h[1] = -1;
                    else
                        if (sdnn <= 0.10 && mxdmn >= 0.6 && cv <= 8.0)
                            h[1] = -2;

            //h[2]
            //vegetative homeostasis
            if (mxdmn <= 0.06 && amo >= 80 && si >= 500)
                h[2] = 2;
            else
                if (mxdmn <= 0.15 && amo >= 50 && si >= 200)
                    h[2] = 1;
                else
                    if (mxdmn >= 0.30 && amo <= 30 && si <= 50)
                        h[2] = -1;
                    else
                        if (mxdmn >= 0.50 && amo <= 15 && si <= 25)
                            h[2] = -2;

            //h[3]
            //regulation stability
            if (cv <= 3 || cv >= 6)
                h[3] = 2;

            //h[4]
            //Basal activity of the nervous centers
            if (lfPercents / totalFPercents >= 0.70 && vlfPercents / totalFPercents >= 0.25 && hfPercents <= 0.05)
                h[4] = 2;
            else
                if (lfPercents / totalFPercents >= 0.60 && hfPercents <= 0.20)
                    h[4] = 1;
                else
                    if (lfPercents / totalFPercents <= 0.20 && hfPercents >= 0.40)
                        h[4] = -2;
                    else
                        if (lfPercents / totalFPercents <= 0.40 && hfPercents >= 0.30)
                            h[4] = -1;

            var rsai = Math.Abs( h[0] ) + Math.Abs( h[1] ) + Math.Abs( h[2] ) + Math.Abs( h[3] ) + Math.Abs( h[4] );
            var negatives = 0;
            for (int i = 0; i < 5; i++)
                if (h[i] < 0)
                    negatives += Math.Abs(h[i]);
            var negPercents = (int)(negatives / (double) rsai * 100);
            return new int[]{rsai, negPercents};
        }
    }
}