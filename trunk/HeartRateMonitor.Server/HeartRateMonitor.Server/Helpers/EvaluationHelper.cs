using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using ClientServerInteraction;
using PolarMath.Data;
using PolarMath.Evaluation.HRV;
using PolarMath.Evaluation.Spectrum;
using PolarMath.Evaluation.Statistics;
using PolarMath.Evaluation.Time;
using PolarMath.Util;

namespace HeartRateMonitor.Server.Helpers
{
    public class EvaluationHelper
    {
        public static void Evaluate(Session session)
        {
            var sessionData = new SessionData()
                {
                    Intervals = session.Rates
                };
            sessionData.Intervals = Filter.Filtrate(sessionData);
            session.AMoPercents = sessionData.Evaluate(new AMoPercents());
            session.Mo = sessionData.Evaluate(new Mo());
            session.RSAI = sessionData.Evaluate(new RSAI());
            session.HF = sessionData.Evaluate(new HF());
            session.HFPercents = sessionData.Evaluate(new HFPercents());
            session.IC = sessionData.Evaluate(new IC());
            session.LF = sessionData.Evaluate(new LF());
            session.LFPercents = sessionData.Evaluate(new LFPercents());
            session.TP = sessionData.Evaluate(new TP());
            session.ULF = sessionData.Evaluate(new ULF());
            session.ULFPercents = sessionData.Evaluate(new ULFPercents());
            session.VLF = sessionData.Evaluate(new VLF());
            session.VLFPercents = sessionData.Evaluate(new VLFPercents());
            session.Average = sessionData.Evaluate(new Average());
            session.RMSSD = sessionData.Evaluate(new RMSSD());
            session.SDNN = sessionData.Evaluate(new SDNN());
            session.PNN50 = sessionData.Evaluate(new PNN50());



        }
    }
}