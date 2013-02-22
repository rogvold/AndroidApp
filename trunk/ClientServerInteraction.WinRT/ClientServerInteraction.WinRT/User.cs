using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ClientServerInteraction.WinRT
{
    public sealed class User
    {
        public string IdString { get; private set; }

        public IList<string> Sessions { get; set; }

        public string Username { get; set; }

        public string Email { get; set; }

        public string Password { get; set; }
    }
}
