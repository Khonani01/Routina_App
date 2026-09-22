using Microsoft.AspNetCore.Mvc;

namespace project.Controllers
{
    public class VolunteerController : Controller
    {
        // GET: /Volunteer
        public IActionResult Index()
        {
            return View();
        }

        // POST: /Volunteer
        [HttpPost]
        public IActionResult Index(string Name, string Email, string Skills, string Availability, string Phone)
        {
            ViewBag.Message = $"Thank you {Name}! You're now registered as a volunteer.";

            // Optional: Show additional info
            if (!string.IsNullOrEmpty(Skills))
            {
                ViewBag.Message += $" We've noted your skills: {Skills}.";
            }

            return View();
        }
    }
}