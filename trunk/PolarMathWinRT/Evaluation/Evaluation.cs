using HrmMath.Data;

namespace HrmMath.Evaluation
{

    public interface IEvaluation
    {
        Index Name { get; set; }
        object Evaluate(SessionData training);
    }
}
