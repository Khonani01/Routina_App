using Microsoft.AspNetCore.Mvc;

namespace project.Controllers
{
    public class DonateController : Controller
    {
        // GET: /Donate
        public IActionResult Index()
        {
            return View();
        }

        // POST: /Donate
        [HttpPost]
        public IActionResult Index(bool IsRecurring, string Currency, decimal Amount, bool IsAnonymous)
        {
            // Store in TempData to show on next page
            TempData["DonationMessage"] = $"Thank you for your {(IsRecurring ? "recurring" : "one-time")} donation of {Currency} {Amount:F2}!";

            // For now, we'll show the message on the same page
            ViewBag.Message = $"Thank you for your {(IsRecurring ? "recurring" : "one-time")} donation of {Currency} {Amount:F2}!";

            // Optional: If anonymous, show different message
            if (IsAnonymous)
            {
                ViewBag.Message += " You donated anonymously.";
            }

            return View();
        }
    }
}