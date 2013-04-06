using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClientServerInteraction.Error;

namespace ClientServerInteraction
{
    public class ResponseCallback<T>
    {
        public Action<ServerResponseException> ServerError { get; set; }
        public Action<Exception> ClientError { get; set; }
        public Action<T> Success { get; set; }
    }
}
