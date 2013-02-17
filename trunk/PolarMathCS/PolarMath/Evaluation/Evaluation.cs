using PolarMath.Data;

namespace PolarMath.Evaluation
{
    public interface IEvaluation<out T>
    {
        T Evaluate(Training training);
    }
}
