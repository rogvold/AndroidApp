using HrmMath.Data;

namespace HrmMath.Evaluation
{

    interface IEvaluation
    {
        Index Name { get; set; }
        object Evaluate(SessionData training);
    }
}
