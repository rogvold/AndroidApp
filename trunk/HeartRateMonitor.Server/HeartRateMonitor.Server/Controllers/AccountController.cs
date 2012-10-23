using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Web.Mvc;
using HeartRateMonitor.BusinessLayer;
using HeartRateMonitor.BusinessLayer.Helpers;
using HeartRateMonitor.Server.Helpers;

namespace HeartRateMonitor.Server.Controllers
{
    public class AccountController : Controller
    {
        private static readonly string ErrorString = "Error executing request";

       public JsonResult GetUserInfo()
       {
           try
           {
               var request = StreamHelper.ReadJsonFromStream(Request.InputStream);
               var id = request.user_id;
               var user = DBHelper.GetUser(id);
               if (user == null)
               {
                   return Json(new
                       {
                           fail = "User does not exist and no username to create new user"
                       });
               }

               return Json(new
                   {
                       id = user.Id.ToString(),
                       name = user.Username,
                       sessions = user.Sessions
                   });

           } catch( Exception e)
           {
               Trace.WriteLine(e.Message);
               return Json(new
                   {
                       fail = ErrorString
                   });
           }
        }


        public JsonResult GetSessionInfo()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);
                var id = request.session_id;
                var session = DBHelper.GetSession(id);

                if (session == null)
                    return Json(new
                        {
                            fail = "Session does not exist"
                        });

                return Json(new
                {
                    id = session.Id.ToString(),
                    start_time = session.StartTime,
                    device_id = session.DeviceId,
                    device_name = session.DeviceName,
                    rates = session.Rates
                });

            }
            catch (Exception e)
            {
                Trace.WriteLine(e.Message);
                return Json(new
                {
                    fail = ErrorString
                });
            }
        }

        public JsonResult AddUser()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);

                var user = DBHelper.AddUser(request.username);
                if (user == null)
                    return Json(new
                        {
                            fail = "Error in request"
                        });

                return Json(new
                    {
                        id = user.Id.ToString()
                    });
            }
            catch (Exception e)
            {
                Trace.WriteLine(e.Message);
                return Json(new
                {
                    fail = ErrorString
                });
            }
        }

        public JsonResult AddSession()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);

                var userId = request.user_id;
                var user = DBHelper.GetUser(userId);
                
                if (user == null)
                    return Json(new
                        {
                            fail = "User does not exist"
                        });

                var session = new Session()
                    {
                        DeviceId = request.device_id,
                        DeviceName = request.device_name,
                        Rates = request.rates,
                        StartTime = request.start_time // TODO: calculate timestamp from time or vice versa
                    };
                if (session.Rates == null)
                    session.Rates = new List<int>();

                var sessionId = DBHelper.AddSession(userId, session);

                return Json(new
                {
                    session_id = sessionId
                });

            }
            catch (Exception e)
            {
                Trace.WriteLine(e.Message);
                return Json(new
                {
                    fail = ErrorString
                });
            }
        }

        public JsonResult AddRate()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);

                if (DBHelper.AddRateToSession(request.session_id, request.rate))
                    return Json(new
                    {
                        success = "Rate successfully added"
                    });
                else
                    return Json(new
                        {
                            fail = "Error in request"
                        });

            }
            catch (Exception e)
            {
                Trace.WriteLine(e.Message);
                return Json(new
                {
                    fail = ErrorString
                });
            }
        }


    }
}
