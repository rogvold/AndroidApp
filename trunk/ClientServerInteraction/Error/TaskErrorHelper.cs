using System;
using System.Threading.Tasks;

namespace ClientServerInteraction.Error
{
    public class TaskErrorHelper
    {
        internal static Task ErrorAsync(Exception e)
        {
            return ErrorAsync<object>(e);
        }

        internal static Task<T> ErrorAsync<T>(Exception e)
        {
            var tcs = new TaskCompletionSource<T>();
            tcs.SetException(e);
            return tcs.Task;
        }


    }
}
