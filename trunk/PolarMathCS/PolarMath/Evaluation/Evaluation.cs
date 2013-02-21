using PolarMath.Data;

namespace PolarMath.Evaluation
{
    public interface IEvaluation<out T>
    {
        T Evaluate(SessionData training);
    }
}
