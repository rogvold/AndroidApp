using System.Collections.Generic;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace ClientServerInteraction
{
    public class User
    {
        public int Id { get; set; }
        public string About { get; set; }
        public string BirthDate { get; set; }
        public string Department { get; set; }
        public string Description { get; set; }
        public string Diagnosis { get; set; }
        public string Email { get; set; }
        public string FirstName { get; set; }
        public float Height { get; set; }
        public string LastName { get; set; }
        public string Password { get; set; }
        public int Sex { get; set; }
        public string StatusMessage { get; set; }
        public float Weight { get; set; }
    }
}
