using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Mvc;
using HeartRateMonitor.BusinessLayer.Helpers;
using HeartRateMonitor.Math.Evaluation;
using HeartRateMonitor.Math.Evaluation.Geometry;
using HeartRateMonitor.Server.Helpers;


namespace HeartRateMonitor.Server.Controllers
{
    public class EvaluationController : Controller
    {
        public JsonResult EvaluateSession()
        {
            try
            {
                var request = StreamHelper.ReadJsonFromStream(Request.InputStream);
                var session = DBHelper.GetSession(request.session_id);
                if (session == null)
                {
                    return Json(new
                        {
                            fail = "No such session in database"
                        });
                }
                var intervals = session.Rates;

                var sessionData = new Math.SessionData()
                    {
                        Intervals = intervals
                    };
                var average = sessionData.Evaluate(new Average());
                var sdnn = sessionData.Evaluate(new SDNN());
                var rmssd = sessionData.Evaluate(new RMSSD());
                var pnn50 = sessionData.Evaluate(new PNN50());
                var cv = sessionData.Evaluate(new CV());
                var histogram = sessionData.Evaluate(new EvaluateBasicHistogram());

                return Json(new
                    {
                        average,
                        sdnn,
                        rmssd,
                        pnn50,
                        cv,
                        histogram
                    });
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
