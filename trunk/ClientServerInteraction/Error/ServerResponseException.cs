using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ClientServerInteraction.Error
{
    public class ServerResponseException : Exception
    {
        public ServerResponseException() : base() { }
        public ServerResponseException(string message) : base(message) { }
        public ServerResponseException(string message, Exception inner) : base(message, inner) { }
    }
}
