using Microsoft.AspNetCore.Mvc;

namespace ITMONavServer.WebApi.Controllers
{
    [Route("/")]
    public class HomeController : Controller
    {
        [HttpGet]
        public string Get()
        {
            return "Hello from ITMONavServer!";
        }
    }
}
