using PolarMath.Data;

namespace PolarMath.Evaluation.Spectrum
{
    public class ULFPercents : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var ulf = training.Evaluate( new ULF() );
            var tp = training.Evaluate( new TP() );
            return (ulf / tp) * 100;
        }
    }
}
