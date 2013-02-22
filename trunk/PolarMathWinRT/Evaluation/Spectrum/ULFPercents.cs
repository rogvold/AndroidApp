using HrmMath.Data;

namespace HrmMath.Evaluation.Spectrum
{
    internal sealed class ULFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.ULFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var ulf = training.Evaluate( new ULF() );
            var tp = training.Evaluate( new TP() );
            return ((double)ulf /(double) tp) * 100;
        }
    }
}
