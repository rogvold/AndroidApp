using HrmMath.Data;

namespace HrmMath.Evaluation
{
    public interface IEvaluation<out T>
    {
        T Evaluate(SessionData training);
    }
}
