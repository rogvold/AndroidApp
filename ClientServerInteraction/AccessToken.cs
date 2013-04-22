using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ClientServerInteraction
{
    public class AccessToken
    {
        public string Token { get; set; }
        public long ExpiredDate { get; set; }
    }
}
