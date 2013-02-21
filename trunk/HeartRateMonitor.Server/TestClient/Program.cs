using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using ClientServerInteraction;

namespace TestClient
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Beginning test.");
            Console.WriteLine(ServerHelper.RegisterUser(@"pogr.yuo@gmail.com", @"02034242", @"Yury Pogrebnyak") ? @"Registration successful" : @"User already exists");
            
            var user = ServerHelper.AuthorizeUser(@"pogr.yuo@gmail.com", @"02034242");

            Console.WriteLine(@"User sessions: ");
            foreach (var sessionId in user.Sessions)
            {
                Console.WriteLine(sessionId);
            }

            var session1 = ServerHelper.AddSession(new Session()
                {
                    UserId = user.IdString,
                    Rates = new List<int>()
                        {
                            1000,
                            1000,
                            1000
                        }
                });
            var session2 = ServerHelper.AddSession(new Session()
                {
                    UserId = user.IdString,
                    Rates = new List<int>()
                        {
                            500,
                            500,
                            500
                        }
                });

            var sessions = ServerHelper.GetSessions(new List<string>() {session1.IdString, session2.IdString});
            Console.WriteLine("Added sessions: ");
            foreach (var session in sessions)
            {
                Console.WriteLine(session.IdString);
            }
            Console.ReadKey();
        }
    }
}
