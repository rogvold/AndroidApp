using System.Collections.Generic;
using MongoDB.Bson;
using MongoDB.Driver;
using MongoDB.Driver.Builders;

namespace HeartRateMonitor.BusinessLayer.Helpers
{
    public class DBHelper
    {
        private static readonly MongoServer Server = MongoServer.Create();
        private static readonly MongoDatabase Db;
        private static readonly MongoCollection<UserDB> Users;
        private static readonly MongoCollection<SessionDB> Sessions;


        static DBHelper()
        {
            Db = Server.GetDatabase("HeartRateMonitor");
            Users = Db.GetCollection<UserDB>("Users");
            Sessions = Db.GetCollection<SessionDB>("Sessions");
        }

        public static void DropCollection(string collectionName)
        {
            Db.DropCollection(collectionName);
        }

        public static UserDB AddUser(string username)
        {
            if (string.IsNullOrEmpty(username))
                return null;
            var user = new UserDB()
                {
                    Username = username,
                    Sessions = new List<string>()
                };
            Users.Save(typeof(UserDB), user);
            return user;
        }

        public static bool AddUser(UserDB user)
        {
            var dbUser = GetUserByEmail(user.Email);
            if (dbUser != null)
                return false;
            Users.Save(typeof (UserDB), user);
            return true;
        }

        public static UserDB GetUserByEmail(string email)
        {
            return Users.FindOne(Query.EQ("Email", email));
        }

        public static UserDB GetUser(string id)
        {
            return Users.FindOneByIdAs<UserDB>(new ObjectId(id));
        }

        public static UserDB GetUser(string email, string password)
        {
            var user = Users.FindOne(Query.EQ("Email", email));
            if (user == null || user.Password != password)
                return null;
            return user;

        }

        public static SessionDB GetSession(string id)
        {
            return Sessions.FindOneByIdAs<SessionDB>(new ObjectId(id));
        }

        public static string AddSession(string userId, SessionDB session)
        {

            Sessions.Save(typeof (SessionDB), session);
            var id = session.Id.ToString();
            Users.Update(Query.EQ("_id", new ObjectId(userId)), Update<UserDB>.Push(u => u.Sessions, id));
            return id;
        }

        public static void AddSession(SessionDB session)
        {
            AddSession(session.UserId, session);
        }
       
        public static bool AddRateToSession(string sessionId, int rate)
        {
            if (rate < 0 || Sessions.FindOneByIdAs<SessionDB>(new ObjectId(sessionId)) == null)
                return false;
            Sessions.Update(Query.EQ("_id", new ObjectId(sessionId)), Update<SessionDB>.Push(u => u.Rates, rate));
            return true;
        }
    }
}