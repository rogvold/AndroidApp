using HrmMath.Data;

namespace HrmMath.Evaluation.Spectrum
{
    public class LFPercents : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var lf = training.Evaluate( new LF() );
            var tp = training.Evaluate( new TP() );
            return (lf / tp) * 100;
        }
    }
}
