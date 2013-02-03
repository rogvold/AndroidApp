namespace BLELib
{
    public class BLEUser
    {
        private string _password;
        private string _username;

        public BLEUser(string username, string password)
        {
            Username = username;
            Password = password;
        }

        public string Username
        {
            get { return _username; }
            set
            {
                if (value != null)
                    _username = value;
            }
        }

        public string Password
        {
            get { return _password; }
            set
            {
                if (value != null)
                    _password = value;
            }
        }
    }
}