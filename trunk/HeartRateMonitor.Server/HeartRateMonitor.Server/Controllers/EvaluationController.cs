using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Mvc;
using ClientServerInteraction;
using HeartRateMonitor.BusinessLayer;
using HeartRateMonitor.BusinessLayer.Helpers;
using HeartRateMonitor.Server.Helpers;
using PolarMath;
using PolarMath.Data;



namespace HeartRateMonitor.Server.Controllers
{
    public class EvaluationController : Controller
    {
        public JsonResult AddSession()
        {
            try
            {
                var session = new SessionDB(SerializationHelper.DeserializeSession(StreamHelper.ReadJsonToString(Request.InputStream)));

                EvaluationHelper.Evaluate(session);

                DBHelper.AddSession(session);

                return Json(session);
            }
            catch (Exception e)
            {
                return Json(new
                    {
                        fail = "Error executing request: " + e.Message
                    });
            }
        }

        public JsonResult GetSessions()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);

                var sessions = request.Sessions;

                var sessionList = new List<Session>();

                foreach (var sessionId in sessions)
                {
                    sessionList.Add(DBHelper.GetSession(sessionId));   
                }

                return Json(sessionList);
            }
            catch (Exception e)
            {
                return Json(new
                {
                    fail = "Error executing request: " + e.Message
                });
            }
        }

    }
}
