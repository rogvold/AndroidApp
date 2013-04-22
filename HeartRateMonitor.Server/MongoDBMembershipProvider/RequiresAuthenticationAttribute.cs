using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Web.Mvc;
using System.Web.Security;

namespace MongoDBMembershipProvider
{
    public class RequiresAuthenticationAttribute : ActionFilterAttribute
    {
        public override void OnActionExecuting(ActionExecutingContext filterContext)
        {
            //redirect if not authenticated
            if (filterContext.HttpContext.User.Identity.IsAuthenticated) 
                return;
            //use the current url for the redirect
            var redirectOnSuccess = filterContext.HttpContext.Request.Url.AbsolutePath;

            //send them off to the login page
            var redirectUrl = string.Format("?ReturnUrl={0}", redirectOnSuccess);
            var loginUrl = FormsAuthentication.LoginUrl + redirectUrl;
            filterContext.HttpContext.Response.Redirect(loginUrl, true);
        }
    }
}
