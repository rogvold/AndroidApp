using HrmMath.Data;

namespace HrmMath.Evaluation.Spectrum
{
    internal sealed class LFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.LFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var lf = (double)training.Evaluate( new LF() );
            var tp = (double)training.Evaluate( new TP() );
            return (lf / tp) * 100;
        }
    }
}
