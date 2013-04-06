using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ClientServerInteraction.Error
{
    public class ServerResponseException : Exception
    {
        public const int InvalidToken = 20;
        public const int OtherError = 21;

        public ServerResponseException(string message, int code) : base(message)
        {
            ErrorCode = code;
        }

        public int ErrorCode { get; set; }
    }
}
