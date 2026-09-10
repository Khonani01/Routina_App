const request = require("supertest");
const app = require("../index");

describe("GET /", () => {
  it("should return a running message", async () => {
    const res = await request(app).get("/");
    expect(res.statusCode).toBe(200);
    expect(res.body.message).toBe("Routina API is running");
  });
});

describe("POST /auth/register", () => {
  it("should reject registration with missing fields", async () => {
    const res = await request(app).post("/auth/register").send({ email: "test@test.com" });
    expect(res.statusCode).toBe(400);
    expect(res.body.error).toBe("Name, email, and password are required");
  });
});

describe("POST /auth/login", () => {
  it("should reject login with missing fields", async () => {
    const res = await request(app).post("/auth/login").send({ email: "test@test.com" });
    expect(res.statusCode).toBe(400);
    expect(res.body.error).toBe("Email and password are required");
  });
});

describe("Protected routes", () => {
  it("should reject requests without a token", async () => {
    const res = await request(app).get("/settings");
    expect(res.statusCode).toBe(401);
    expect(res.body.error).toBe("No token provided");
  });
});
