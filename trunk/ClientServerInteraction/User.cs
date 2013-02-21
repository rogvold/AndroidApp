using System.Collections.Generic;

namespace ClientServerInteraction
{
    public class User
    {

        public string IdString { get; set; }

        public List<string> Sessions { get; set; }

        public string Username { get; set; }

        public string Email { get; set; }

        public string Password { get; set; }

    }
}
