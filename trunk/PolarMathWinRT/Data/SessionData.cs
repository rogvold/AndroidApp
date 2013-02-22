using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HrmMath.Evaluation;

namespace HrmMath.Data
{
    public enum Index
    {
        Average,    //0
        RMSSD,      //1
        SDNN,       //2
        AMoPercents,//3
        CV,         //4
        Mo,         //5
        MxDMn,      //6
        RSAI,       //7
        SI,         //8
        HF,         //9
        HFPercents, //10
        IC,         //11
        LF,         //12
        LFPercents, //13
        TP,         //14
        ULF,        //15
        ULFPercents,//16
        VLF,        //17
        VLFPercents,//18
        PNN50,      //19
        Lomb,       //20
    };

    internal sealed class SessionData
    {
        public String IdString { get; set; }
        public IList<int> Intervals { get; set; }

        internal readonly SessionDataCache Cache = new SessionDataCache();

        internal object Evaluate(IEvaluation evaluation) {
            if (Cache.Contains(evaluation)) {
                return Cache.Get(evaluation);
            } else {
                object evaluationResult = evaluation.Evaluate(this);
                Cache.Add(evaluation, evaluationResult);
                return evaluationResult;
            }
        }

        //public static Training readTraining(InputStream ins) throw IOException {
        //    return new TrainingReader().readTraining(ins);
        //}
    }
}
