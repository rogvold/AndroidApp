using System.Collections.Generic;
using ClientServerInteraction;
using MongoDB.Bson;
using MongoDB.Driver;
using MongoDB.Driver.Builders;

namespace HeartRateMonitor.Server.Helpers
{
    public class DBHelper
    {
        private const string DBServerUrl = @"mongodb://omnihealth.cloudapp.net:27017";
        private static readonly MongoServer Server; 
        private static readonly MongoDatabase Db;
        private static readonly MongoCollection<User> Users;
        private static readonly MongoCollection<Session> Sessions;


        static DBHelper()
        {
            Server = MongoServer.Create(DBServerUrl);
            Db = Server.GetDatabase("HeartRateMonitor");
            Users = Db.GetCollection<User>("Users");
            Sessions = Db.GetCollection<Session>("Sessions");
        }

        public static void DropCollection(string collectionName)
        {
            Db.DropCollection(collectionName);
        }

        public static User AddUser(string username)
        {
            if (string.IsNullOrEmpty(username))
                return null;
            var user = new User()
                {
                    Username = username,
                    Sessions = new List<string>()
                };
            Users.Save(typeof(User), user);
            return user;
        }

        public static bool AddUser(User user)
        {
            var dbUser = GetUserByEmail(user.Email);
            if (dbUser != null)
                return false;
            Users.Save(typeof (User), user);
            return true;
        }

        public static User GetUserByEmail(string email)
        {
            return Users.FindOne(Query.EQ("Email", email));
        }

        public static User GetUser(string id)
        {
            return Users.FindOneByIdAs<User>(new ObjectId(id));
        }

        public static User GetUser(string email, string password)
        {
            var user = Users.FindOne(Query.EQ("Email", email));
            if (user == null || user.Password != password)
                return null;
            return user;

        }

        public static Session GetSession(string id)
        {
            return Sessions.FindOneByIdAs<Session>(new ObjectId(id));
        }

        public static string AddSession(string userId, Session session)
        {

            Sessions.Save(typeof (Session), session);
            var id = session.Id.ToString();
            Users.Update(Query.EQ("_id", new ObjectId(userId)), Update<User>.Push(u => u.Sessions, id));
            return id;
        }

        public static void AddSession(Session session)
        {
            AddSession(session.UserId, session);
        }
       
        public static bool AddRateToSession(string sessionId, int rate)
        {
            if (rate < 0 || Sessions.FindOneByIdAs<Session>(new ObjectId(sessionId)) == null)
                return false;
            Sessions.Update(Query.EQ("_id", new ObjectId(sessionId)), Update<Session>.Push(u => u.Rates, rate));
            return true;
        }
    }
}