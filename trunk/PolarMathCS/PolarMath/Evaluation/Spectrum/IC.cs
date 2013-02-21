using PolarMath.Data;

namespace PolarMath.Evaluation.Spectrum
{
    public class IC : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var hf = training.Evaluate( new HF() );
            var lf = training.Evaluate( new LF() );
            var vlf = training.Evaluate( new VLF() );

            return (lf + vlf) / hf;
        }
    }
}
