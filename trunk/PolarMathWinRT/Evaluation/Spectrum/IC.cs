using HrmMath.Data;

namespace HrmMath.Evaluation.Spectrum
{
    internal sealed class IC : IEvaluation
    {
        public Index Name
        {
            get { return Index.IC; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var hf = (double)training.Evaluate( new HF() );
            var lf = (double)training.Evaluate( new LF() );
            var vlf = (double)training.Evaluate( new VLF() );

            return (lf + vlf) / hf;
        }
    }
}
