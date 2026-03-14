#define _CRT_SECURE_NO_WARNINGS
#include "raylib.h"
#include "raymath.h"
#include <cmath>
#include <algorithm>
#include <vector>

#define RLIGHTS_IMPLEMENTATION
#include "rlights.h"

#include "Planet.h"
#include "Physics.h"

#define RAYGUI_IMPLEMENTATION
#include "raygui.h"

// Core engine states
enum EngineState { PAUSED, PLAYING, SUMMARY };

// Transient visual effects for collisions
struct Explosion {
  Vector3 position; // Center of the explosion in world space
  float radius;     // Current expanding radius
  float maxRadius;  // Maximum radius before the explosion dies
  float alpha;      // Opacity (1.0 = fully visible, 0.0 = invisible/dead)
  bool isAlive;     // Whether this explosion is still animating
};
std::vector<Explosion> activeExplosions;

int main() {
  const int screenWidth = 1920;
  const int screenHeight = 1080;

  InitWindow(screenWidth, screenHeight, "Stardust");

  // Load 3D lighting shaders
#if defined(PLATFORM_ANDROID)
  Shader lightShader = LoadShader("resources/shaders/glsl100/lighting.vs",
                                  "resources/shaders/glsl100/lighting.fs");
#else
  Shader lightShader = LoadShader("resources/shaders/glsl330/lighting.vs",
                                  "resources/shaders/glsl330/lighting.fs");
#endif

  int viewPosLoc = GetShaderLocation(lightShader, "viewPos");

  // Create point light at the Sun's position
  Light sun = CreateLight(LIGHT_POINT,
                          Vector3{0.0f, 0.0f, 0.0f}, 
                          Vector3Zero(),       
                          WHITE, lightShader); 

  Camera3D camera = {};
  camera.position = Vector3{0.0f, 160.0f, 200.0f};
  camera.target = Vector3{0.0f, 0.0f, 0.0f};
  camera.up = Vector3{0.0f, 1.0f, 0.0f};
  camera.fovy = 45.0f;
  camera.projection = CAMERA_PERSPECTIVE;

  // Scalable Planet Storage
  std::vector<Planet> initialPlanets;
  std::vector<Planet> activePlanets;
  std::vector<Model> planetModels;

  initialPlanets.reserve(16);
  activePlanets.reserve(16);
  planetModels.reserve(16);
  activeExplosions.reserve(32);

  // Solar System Initialization
  initialPlanets = {
      Planet({0.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 0.0f}, 2000.0f, 3.00f,
             "assets/sun.glb", YELLOW, 0.10f, "Sun", 500.0f, 5000.0f, 1.0f,
             6.0f),
      Planet({8.0f * cosf(1.0f), 0.0f, 8.0f * sinf(1.0f)},
             {-15.811f * sinf(1.0f), 0.0f, 15.811f * cosf(1.0f)}, 0.055f, 0.25f,
             "assets/mercury.glb", GRAY, 0.02f, "Mercury", 0.01f, 1.0f, 0.1f,
             1.0f),
      Planet({14.0f * cosf(3.0f), 0.0f, 14.0f * sinf(3.0f)},
             {-11.952f * sinf(3.0f), 0.0f, 11.952f * cosf(3.0f)}, 0.815f, 0.50f,
             "assets/venus.glb", ORANGE, -0.01f, "Venus", 0.1f, 5.0f, 0.1f,
             2.0f),
      Planet({20.0f * cosf(5.0f), 0.0f, 20.0f * sinf(5.0f)},
             {-10.000f * sinf(5.0f), 0.0f, 10.000f * cosf(5.0f)}, 10.00f, 0.55f,
             "assets/earth.glb", BLUE, 0.50f, "Earth", 1.0f, 20.0f, 0.2f, 3.0f),
      Planet({30.0f * cosf(0.5f), 0.0f, 30.0f * sinf(0.5f)},
             {-8.165f * sinf(0.5f), 0.0f, 8.165f * cosf(0.5f)}, 0.107f, 0.35f,
             "assets/mars.glb", RED, 0.48f, "Mars", 0.01f, 2.0f, 0.1f, 1.5f),
      Planet({55.0f * cosf(2.5f), 0.0f, 55.0f * sinf(2.5f)},
             {-6.030f * sinf(2.5f), 0.0f, 6.030f * cosf(2.5f)}, 3.00f, 1.80f,
             "assets/jupiter.glb", BEIGE, 1.50f, "Jupiter", 0.5f, 15.0f, 0.5f,
             4.0f),
      Planet({80.0f * cosf(4.5f), 0.0f, 80.0f * sinf(4.5f)},
             {-5.000f * sinf(4.5f), 0.0f, 5.000f * cosf(4.5f)}, 1.50f, 1.50f,
             "assets/saturn.glb", GOLD, 1.30f, "Saturn", 0.2f, 10.0f, 0.5f,
             3.5f),
      Planet({110.0f * cosf(1.5f), 0.0f, 110.0f * sinf(1.5f)},
             {-4.264f * sinf(1.5f), 0.0f, 4.264f * cosf(1.5f)}, 0.50f, 1.00f,
             "assets/uranus.glb", SKYBLUE, -0.80f, "Uranus", 0.1f, 5.0f, 0.3f,
             2.5f),
      Planet({140.0f * cosf(3.5f), 0.0f, 140.0f * sinf(3.5f)},
             {-3.780f * sinf(3.5f), 0.0f, 3.780f * cosf(3.5f)}, 0.60f, 0.95f,
             "assets/neptune.glb", DARKBLUE, 0.90f, "Neptune", 0.1f, 5.0f, 0.3f,
             2.5f),
      Planet({20.0f * cosf(5.0f) + 0.9f, 0.0f, 20.0f * sinf(5.0f)},
             {-10.000f * sinf(5.0f), 0.0f, 10.000f * cosf(5.0f) + 3.333f},
             0.012f, 0.15f, "assets/moon.glb", LIGHTGRAY, 0.05f, "Moon", 0.001f,
             0.5f, 0.05f, 0.5f),
  };

  // Conservation of momentum to fix system drift
  Vector3 totalMomentum = {0.0f, 0.0f, 0.0f};
  for (size_t i = 1; i < initialPlanets.size(); i++) {
    totalMomentum.x += initialPlanets[i].mass * initialPlanets[i].velocity.x;
    totalMomentum.y += initialPlanets[i].mass * initialPlanets[i].velocity.y;
    totalMomentum.z += initialPlanets[i].mass * initialPlanets[i].velocity.z;
  }

  initialPlanets[0].velocity.x = -totalMomentum.x / initialPlanets[0].mass;
  initialPlanets[0].velocity.y = -totalMomentum.y / initialPlanets[0].mass;
  initialPlanets[0].velocity.z = -totalMomentum.z / initialPlanets[0].mass;

  // Load Planet Models
  for (size_t i = 0; i < initialPlanets.size(); i++) {
    planetModels.push_back(LoadModel(initialPlanets[i].modelPath.c_str()));
  }

  Shader defaultSunShader = planetModels[0].materials[0].shader;

  // Setup generic lighting for planets
  for (size_t i = 0; i < planetModels.size(); i++) {
    for (int m = 0; m < planetModels[i].materialCount; m++) {
      planetModels[i].materials[m].shader = lightShader;
    }
  }

  activePlanets = initialPlanets;
  SetTargetFPS(180);

  EngineState currentState = PAUSED;
  Planet *selectedPlanet = nullptr;
  bool isCameraActive = false;
  float cameraSpeed = 2.0f;

  // Mobile Input State
  bool joystickActive = false;
  Vector2 joystickCenter = {0.0f, 0.0f};
  Vector2 joystickThumb = {0.0f, 0.0f};
  int joystickTouchPointId = -1;

  bool lookActive = false;
  Vector2 lastLookPos = {0.0f, 0.0f};
  int lookTouchPointId = -1;

  Planet *prevSelectedPlanet = nullptr;

  // Toast notification state
  float toastTimer = 0.0f;
  char toastText[192] = {0};
  float prevSliderMass = -1.0f; 
  float prevSliderRadius = -1.0f;

  const float G = 1.0f;

  while (!WindowShouldClose()) {
    float dt = GetFrameTime();

    // Camera Toggle: Right Mouse Button Control
    if (IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
      if (!isCameraActive) {
        DisableCursor();
        isCameraActive = true;
      }

      UpdateCamera(&camera, CAMERA_FREE);

      Vector3 forward = Vector3Normalize(Vector3Subtract(camera.target, camera.position));
      Vector3 right = Vector3Normalize(Vector3CrossProduct(forward, camera.up));

      float moveAmount = cameraSpeed * dt;

      // Manual WASD navigation at custom speed
      if (IsKeyDown(KEY_W)) {
        camera.position = Vector3Add(camera.position, Vector3Scale(forward, moveAmount));
        camera.target = Vector3Add(camera.target, Vector3Scale(forward, moveAmount));
      }
      if (IsKeyDown(KEY_S)) {
        camera.position = Vector3Subtract(camera.position, Vector3Scale(forward, moveAmount));
        camera.target = Vector3Subtract(camera.target, Vector3Scale(forward, moveAmount));
      }
      if (IsKeyDown(KEY_D)) {
        camera.position = Vector3Add(camera.position, Vector3Scale(right, moveAmount));
        camera.target = Vector3Add(camera.target, Vector3Scale(right, moveAmount));
      }
      if (IsKeyDown(KEY_A)) {
        camera.position = Vector3Subtract(camera.position, Vector3Scale(right, moveAmount));
        camera.target = Vector3Subtract(camera.target, Vector3Scale(right, moveAmount));
      }

      // Speed adjustment via scroll wheel
      float wheel = GetMouseWheelMove();
      if (wheel != 0.0f) {
        cameraSpeed += wheel * 1.0f;
        if (cameraSpeed < 0.5f) cameraSpeed = 0.5f;
        if (cameraSpeed > 50.0f) cameraSpeed = 50.0f;
      }
    } else {
      if (isCameraActive) {
        EnableCursor();
        isCameraActive = false;
      }
    }

    // Toggle Simulation State
    if (IsKeyPressed(KEY_SPACE)) {
      if (currentState == PAUSED) currentState = PLAYING;
      else if (currentState == PLAYING) currentState = PAUSED;
    }

    // Mouse Picking
    if (!isCameraActive && IsMouseButtonPressed(MOUSE_BUTTON_LEFT) &&
        GetMouseY() > 120 && GetMouseY() < (screenHeight - 280)) {
      Ray mouseRay = GetMouseRay(GetMousePosition(), camera);
      float closestDistance = 999999.0f;
      int closestIndex = -1;

      for (size_t i = 0; i < activePlanets.size(); i++) {
        RayCollision collision = GetRayCollisionSphere(mouseRay, activePlanets[i].position, activePlanets[i].radius);
        if (collision.hit && collision.distance < closestDistance) {
          closestDistance = collision.distance;
          closestIndex = (int)i;
        }
      }

      if (closestIndex >= 0) selectedPlanet = &activePlanets[closestIndex];
      else selectedPlanet = nullptr;
    }

    // Physics Pipeline
    if (currentState == PLAYING) {
      // Physics Sub-stepping
      const int SUB_STEPS = 10;
      float subDt = dt / SUB_STEPS;

      for (int step = 0; step < SUB_STEPS; step++) {
        // N-Body Gravity Loop
        for (size_t i = 0; i < activePlanets.size(); i++) {
          for (size_t j = i + 1; j < activePlanets.size(); j++) {
            if (!activePlanets[i].isAlive || !activePlanets[j].isAlive) continue;

            ApplyGravity(activePlanets[i], activePlanets[i].mass, activePlanets[j], G, subDt);
            ApplyGravity(activePlanets[j], activePlanets[j].mass, activePlanets[i], G, subDt);

            // Volumetric Collision Detection
            float distSqr = Vector3DistanceSqr(activePlanets[i].position, activePlanets[j].position);
            float combinedRadii = activePlanets[i].radius + activePlanets[j].radius;
            float collisionThreshold = combinedRadii * 0.8f;

            if (distSqr < (collisionThreshold * collisionThreshold)) {
              activePlanets[i].isAlive = false;
              activePlanets[j].isAlive = false;

              if (selectedPlanet == &activePlanets[i] || selectedPlanet == &activePlanets[j]) {
                selectedPlanet = nullptr;
              }

              // Spawn Explosion at midpoint
              Vector3 midpoint = {
                  (activePlanets[i].position.x + activePlanets[j].position.x) / 2.0f,
                  (activePlanets[i].position.y + activePlanets[j].position.y) / 2.0f,
                  (activePlanets[i].position.z + activePlanets[j].position.z) / 2.0f};

              Explosion exp;
              exp.position = midpoint;
              exp.radius = combinedRadii * 0.5f; 
              exp.maxRadius = combinedRadii * 3.0f; 
              exp.alpha = 1.0f;         
              exp.isAlive = true;
              activeExplosions.push_back(exp);
            }
          }
        }
      }

      for (size_t i = 0; i < activePlanets.size(); i++) {
        if (!activePlanets[i].isAlive) continue;
        UpdatePosition(activePlanets[i], dt);
      }
    }

    // Update light shader with camera view position
    float camPos[3] = {camera.position.x, camera.position.y, camera.position.z};
    SetShaderValue(lightShader, viewPosLoc, camPos, SHADER_UNIFORM_VEC3);

    BeginDrawing();
    ClearBackground(BLACK);
    BeginMode3D(camera);

    // Rendering Loop
    for (size_t i = 0; i < activePlanets.size(); i++) {
      if (!activePlanets[i].isAlive) continue;

      const float visualScaleFactor = 1.0f;
      Vector3 modelScale = {activePlanets[i].radius * visualScaleFactor,
                            activePlanets[i].radius * visualScaleFactor,
                            activePlanets[i].radius * visualScaleFactor};

      if (currentState == PLAYING) {
        activePlanets[i].rotationAngle += activePlanets[i].rotationSpeed * dt * RAD2DEG;
      }

      // Emissive Sun Trick
      if (i == 0) {
        for (int m = 0; m < planetModels[i].materialCount; m++) {
          planetModels[i].materials[m].shader = defaultSunShader;
        }
      }

      DrawModelEx(planetModels[i], activePlanets[i].position, Vector3{0.0f, 1.0f, 0.0f},
          activePlanets[i].rotationAngle, modelScale, WHITE);

      if (i == 0) {
        for (int m = 0; m < planetModels[i].materialCount; m++) {
          planetModels[i].materials[m].shader = lightShader;
        }
      }
    }

    // Selection Halo
    if (selectedPlanet != nullptr) {
      DrawSphereWires(selectedPlanet->position, selectedPlanet->radius * 1.1f, 16, 16, YELLOW);
    }

    // Explosion Rendering
    BeginBlendMode(BLEND_ADDITIVE);
    for (size_t e = 0; e < activeExplosions.size(); e++) {
      if (!activeExplosions[e].isAlive) continue;

      activeExplosions[e].radius += 3.0f * dt;
      activeExplosions[e].alpha -= 0.5f * dt;

      if (activeExplosions[e].alpha <= 0.0f || activeExplosions[e].radius >= activeExplosions[e].maxRadius) {
        activeExplosions[e].isAlive = false;
        continue;
      }

      unsigned char byteAlpha = (unsigned char)(activeExplosions[e].alpha * 255.0f);
      DrawSphereWires(activeExplosions[e].position, activeExplosions[e].radius, 8, 8, Color{255, 161, 0, byteAlpha});
      DrawSphereWires(activeExplosions[e].position, activeExplosions[e].radius * 0.6f, 8, 8, Color{255, 255, 0, byteAlpha});
    }
    EndBlendMode();

    // Pool Cleanup
    activeExplosions.erase(std::remove_if(activeExplosions.begin(), activeExplosions.end(), 
        [](const Explosion &e) { return !e.isAlive; }), activeExplosions.end());

    EndMode3D();

    // Planet Controls Panel
    {
      const float PNL_X = 10.0f;
      const float PNL_Y = 10.0f;
      const float PNL_W = 449.0f;        
      const float PNL_H = 221.0f;        
      const float PAD = 16.0f;           
      const float SLD_X = PNL_X + 110.0f; 
      const float SLD_W = PNL_W - 110.0f - PAD - 68.0f; 
      const float ROW_H = 40.0f;         

      DrawRectangleRounded({PNL_X, PNL_Y, PNL_W, PNL_H}, 0.06f, 8, Color{5, 5, 15, 175});
      DrawRectangleRoundedLinesEx({PNL_X, PNL_Y, PNL_W, PNL_H}, 0.06f, 8, 1.5f, Color{55, 55, 85, 210});

      float r1Y = PNL_Y + PAD;
      if (selectedPlanet != nullptr) {
        DrawText(selectedPlanet->name.c_str(), (int)(PNL_X + PAD), (int)r1Y, 26, YELLOW); 
        DrawText("SELECTED", (int)(PNL_X + PNL_W - 108.0f), (int)(r1Y + 5), 14, Color{200, 200, 70, 150});
      } else {
        DrawText("Tap a planet to select", (int)(PNL_X + PAD), (int)r1Y, 22, Color{100, 100, 100, 200});
      }

      DrawLineEx({PNL_X + PAD, r1Y + 35.0f}, {PNL_X + PNL_W - PAD, r1Y + 35.0f}, 1.0f, Color{55, 55, 80, 180});

      float r2Y = r1Y + 46.0f; 
      if (selectedPlanet != nullptr) {
        GuiSlider({SLD_X, r2Y, SLD_W, ROW_H}, "Mass", TextFormat("%.2f", selectedPlanet->mass), &selectedPlanet->mass, selectedPlanet->massMin, selectedPlanet->massMax);
      } else {
        GuiSetState(STATE_DISABLED);
        float dummy = 0.5f;
        GuiSlider({SLD_X, r2Y, SLD_W, ROW_H}, "Mass", "--", &dummy, 0.0f, 1.0f);
        GuiSetState(STATE_NORMAL);
      }

      float r3Y = r2Y + ROW_H + 11.0f; 
      if (selectedPlanet != nullptr) {
        GuiSlider({SLD_X, r3Y, SLD_W, ROW_H}, "Radius", TextFormat("%.2f", selectedPlanet->radius), &selectedPlanet->radius, selectedPlanet->radiusMin, selectedPlanet->radiusMax);
      } else {
        GuiSetState(STATE_DISABLED);
        float dummy = 0.5f;
        GuiSlider({SLD_X, r3Y, SLD_W, ROW_H}, "Radius", "--", &dummy, 0.0f, 1.0f);
        GuiSetState(STATE_NORMAL);
      }

      float r4Y = r3Y + ROW_H + 11.0f;
      if (GuiButton({PNL_X + PAD, r4Y, PNL_W - PAD * 2.0f, ROW_H}, "RESET SIMULATION")) {
        selectedPlanet = nullptr;
        prevSelectedPlanet = nullptr;
        prevSliderMass = -1.0f;
        prevSliderRadius = -1.0f;
        toastTimer = 0.0f;
        currentState = PAUSED;
        activePlanets = initialPlanets;
        activeExplosions.clear();
      }
    }

    // Status Panel
    {
      const float RP_W = 422.0f; 
      const float RP_X = (float)screenWidth - RP_W - 10.0f;
      const float RP_Y = 10.0f;
      const float RP_H = 122.0f; 

      DrawRectangleRounded({RP_X, RP_Y, RP_W, RP_H}, 0.06f, 8, Color{5, 5, 15, 175});
      DrawRectangleRoundedLinesEx({RP_X, RP_Y, RP_W, RP_H}, 0.06f, 8, 1.5f, Color{55, 55, 85, 210});

      bool isPlaying = (currentState == PLAYING);
      DrawCircleV({RP_X + 24.0f, RP_Y + 29.0f}, 10.0f, isPlaying ? Color{60, 220, 80, 255} : Color{220, 60, 60, 255});
      DrawText(isPlaying ? "PLAYING" : "PAUSED", (int)(RP_X + 43.0f), (int)(RP_Y + 17.0f), 26, isPlaying ? GREEN : RED);

      DrawLineEx({RP_X + 14.0f, RP_Y + 58.0f}, {RP_X + RP_W - 14.0f, RP_Y + 58.0f}, 1.0f, Color{55, 55, 80, 180});
      GuiSlider({RP_X + 116.0f, RP_Y + 68.0f, RP_W - 134.0f, 37.0f}, "Cam Spd", TextFormat("%.1f", cameraSpeed), &cameraSpeed, 0.5f, 50.0f);
    }

    // Toast Notification logic
    if (selectedPlanet != nullptr) {
      if (selectedPlanet != prevSelectedPlanet) {
        prevSelectedPlanet = selectedPlanet;
        prevSliderMass = selectedPlanet->mass;
        prevSliderRadius = selectedPlanet->radius;
      }

      if (prevSliderMass != selectedPlanet->mass) {
        prevSliderMass = selectedPlanet->mass;
        toastTimer = 3.0f;

        const double SIM_EARTH_MASS = 10.0;
        const double REAL_EARTH_MASS = 5.972e24; 
        double earthRatio = (double)selectedPlanet->mass / SIM_EARTH_MASS;
        double realKg = earthRatio * REAL_EARTH_MASS;

        snprintf(toastText, sizeof(toastText), "%s  |  %.3e kg  |  %.3gx Earth mass",
                 selectedPlanet->name.c_str(), realKg, earthRatio);
      }

      if (prevSliderRadius != selectedPlanet->radius) {
        prevSliderRadius = selectedPlanet->radius;
        toastTimer = 3.0f;

        const double SIM_EARTH_RADIUS = 0.55;
        const double REAL_EARTH_RADIUS = 6371.0; 
        double earthRadiusRatio = (double)selectedPlanet->radius / SIM_EARTH_RADIUS;
        double realKm = earthRadiusRatio * REAL_EARTH_RADIUS;

        snprintf(toastText, sizeof(toastText), "%s  |  %.0f km  |  %.3gx Earth radius",
                 selectedPlanet->name.c_str(), realKm, earthRadiusRatio);
      }
    } else {
      prevSelectedPlanet = nullptr;
      prevSliderMass = -1.0f;
      prevSliderRadius = -1.0f;
    }

    // Drew Toast message
    if (toastTimer > 0.0f) {
      toastTimer -= dt;
      if (toastTimer < 0.0f) toastTimer = 0.0f;

      unsigned char alpha = (toastTimer < 0.5f) ? (unsigned char)((toastTimer / 0.5f) * 255.0f) : 255;
      const int FS = 22; 
      int tw = MeasureText(toastText, FS);
      int tx = screenWidth / 2 - tw / 2;
      int ty = 175;

      DrawRectangleRounded({(float)(tx - 22), (float)(ty - 10), (float)(tw + 44), (float)(FS + 22)},
                           0.45f, 8, Color{0, 0, 0, (unsigned char)(alpha * 0.65f)});
      DrawText(toastText, tx, ty, FS, Color{255, 228, 110, alpha});
    }

    DrawText("SPACE: Play/Pause  |  Hold RMB + WASD: Free Camera  |  LMB: Select Planet", 10, screenHeight - 28, 15, Color{90, 90, 90, 200});

    // Mobile Navigation: PUBG Style
    if (!isCameraActive) {
      const float JS_BASE_RADIUS  = 156.0f; 
      const float JS_THUMB_RADIUS = 54.0f;  
      const float JS_DEAD_ZONE    = 0.10f;
      const float MARGIN          = 96.0f;

      Vector2 jsBase = { MARGIN + JS_BASE_RADIUS, (float)screenHeight - MARGIN - JS_BASE_RADIUS };

      // Elevation control
      Rectangle btnUp   = { (float)screenWidth - 160.0f, (float)screenHeight - 360.0f, 120.0f, 100.0f };
      Rectangle btnDown = { (float)screenWidth - 160.0f, (float)screenHeight - 220.0f, 120.0f, 100.0f };

      Vector3 camForward = Vector3Normalize(Vector3Subtract(camera.target, camera.position));
      Vector3 camRight   = Vector3Normalize(Vector3CrossProduct(camForward, camera.up));
      Vector3 worldUp    = {0.0f, 1.0f, 0.0f};
      float   moveAmount = cameraSpeed * dt;

      int tc = GetTouchPointCount();

      auto isRectPressed = [&](Rectangle rect) -> bool {
        if (tc > 0) {
          for (int t = 0; t < tc; t++) if (CheckCollisionPointRec(GetTouchPosition(t), rect)) return true;
          return false;
        }
        return IsMouseButtonDown(MOUSE_LEFT_BUTTON) && CheckCollisionPointRec(GetMousePosition(), rect);
      };

      if (isRectPressed(btnUp)) {
        camera.position = Vector3Add(camera.position, Vector3Scale(worldUp, moveAmount));
        camera.target   = Vector3Add(camera.target,   Vector3Scale(worldUp, moveAmount));
      }
      if (isRectPressed(btnDown)) {
        camera.position = Vector3Subtract(camera.position, Vector3Scale(worldUp, moveAmount));
        camera.target   = Vector3Subtract(camera.target,   Vector3Scale(worldUp, moveAmount));
      }

      bool currentJoystickHeld = false;
      bool currentLookHeld     = false;

      // Exclusion Zones
      Rectangle leftPanelRec  = { 10.0f, 10.0f, 449.0f, 221.0f };
      Rectangle rightPanelRec = { (float)screenWidth - 432.0f, 10.0f, 422.0f, 122.0f };
      Rectangle playPauseRec  = { (float)screenWidth / 2.0f - 120.0f, (float)screenHeight - 140.0f, 240.0f, 110.0f };

      if (tc > 0) {
        for (int t = 0; t < tc; t++) {
          Vector2 tp = GetTouchPosition(t);
          int tid = GetTouchPointId(t);

          if (CheckCollisionPointRec(tp, btnUp) || CheckCollisionPointRec(tp, btnDown) ||
              CheckCollisionPointRec(tp, leftPanelRec) || CheckCollisionPointRec(tp, rightPanelRec) ||
              CheckCollisionPointRec(tp, playPauseRec)) continue;

          if (joystickActive && tid == joystickTouchPointId) {
            currentJoystickHeld = true;
            float dx = tp.x - joystickCenter.x;
            float dy = tp.y - joystickCenter.y;
            float dist = sqrtf(dx * dx + dy * dy);
            if (dist > JS_BASE_RADIUS) { dx = (dx / dist) * JS_BASE_RADIUS; dy = (dy / dist) * JS_BASE_RADIUS; }
            joystickThumb = {joystickCenter.x + dx, joystickCenter.y + dy};
          } else if (lookActive && tid == lookTouchPointId) {
            currentLookHeld = true;
            Vector2 delta = {tp.x - lastLookPos.x, tp.y - lastLookPos.y};
            lastLookPos = tp;

            camForward = Vector3RotateByAxisAngle(camForward, worldUp, -delta.x * 0.004f);
            camRight = Vector3Normalize(Vector3CrossProduct(camForward, worldUp));
            Vector3 newForward = Vector3RotateByAxisAngle(camForward, camRight, -delta.y * 0.004f);
            if (Vector3DotProduct(newForward, worldUp) < 0.95f && Vector3DotProduct(newForward, worldUp) > -0.95f) camForward = newForward;
            camera.target = Vector3Add(camera.position, camForward);
          } else if (!joystickActive && tp.x < screenWidth / 2.0f) {
            joystickActive = true; joystickTouchPointId = tid; joystickCenter = jsBase; joystickThumb = tp; currentJoystickHeld = true;
          } else if (!lookActive && tp.x >= screenWidth / 2.0f) {
            lookActive = true; lookTouchPointId = tid; lastLookPos = tp; currentLookHeld = true;
          }
        }
      } else if (IsMouseButtonDown(MOUSE_LEFT_BUTTON)) {
        Vector2 mp = GetMousePosition();
        if (!CheckCollisionPointRec(mp, leftPanelRec) && !CheckCollisionPointRec(mp, rightPanelRec) &&
            !CheckCollisionPointRec(mp, playPauseRec) && !CheckCollisionPointRec(mp, btnUp) &&
            !CheckCollisionPointRec(mp, btnDown) && mp.x < screenWidth / 2.0f) {
          joystickActive = true; currentJoystickHeld = true; joystickCenter = jsBase;
          float dx = mp.x - jsBase.x; float dy = mp.y - jsBase.y;
          float dist = sqrtf(dx * dx + dy * dy);
          if (dist > JS_BASE_RADIUS) { dx = (dx / dist) * JS_BASE_RADIUS; dy = (dy / dist) * JS_BASE_RADIUS; }
          joystickThumb = {joystickCenter.x + dx, joystickCenter.y + dy};
        }
      }

      if (!currentJoystickHeld) { joystickActive = false; joystickTouchPointId = -1; joystickThumb = jsBase; }
      if (!currentLookHeld) { lookActive = false; lookTouchPointId = -1; }

      if (joystickActive) {
        float normX = (joystickThumb.x - joystickCenter.x) / JS_BASE_RADIUS;
        float normY = (joystickThumb.y - joystickCenter.y) / JS_BASE_RADIUS;
        if (fabsf(normX) > JS_DEAD_ZONE) {
          camera.position = Vector3Add(camera.position, Vector3Scale(camRight, normX * moveAmount));
          camera.target = Vector3Add(camera.target, Vector3Scale(camRight, normX * moveAmount));
        }
        if (fabsf(normY) > JS_DEAD_ZONE) {
          camera.position = Vector3Subtract(camera.position, Vector3Scale(camForward, normY * moveAmount));
          camera.target = Vector3Subtract(camera.target, Vector3Scale(camForward, normY * moveAmount));
        }
      }

      DrawCircleV(jsBase, JS_BASE_RADIUS, {50, 50, 50, 120});
      DrawCircleLinesV(jsBase, JS_BASE_RADIUS, {255, 255, 255, 160});
      DrawCircleV(joystickActive ? joystickThumb : jsBase, JS_THUMB_RADIUS, joystickActive ? Color{255, 255, 255, 220} : Color{160, 160, 160, 170});
      DrawText("MOVE", (int)(jsBase.x - 36), (int)(jsBase.y - JS_BASE_RADIUS - 42), 29, LIGHTGRAY);

      GuiSetStyle(DEFAULT, TEXT_SIZE, 24);
      GuiSetAlpha(0.75f);
      GuiButton(btnUp, "^ FLY UP");
      GuiButton(btnDown, "v FLY DN");
      GuiSetAlpha(1.0f);
      GuiSetStyle(DEFAULT, TEXT_SIZE, 10);

      if (!lookActive && tc == 0) DrawText("[ Drag Right Half to Look Around ]", screenWidth - 420, screenHeight / 2, 22, {255, 255, 255, 100});
    }

    // Play/Pause Overlay
    {
      const float PP_W = 220.0f;
      const float PP_H = 88.0f;
      const float PP_X = (float)screenWidth / 2.0f - PP_W / 2.0f;
      const float PP_Y = (float)screenHeight - PP_H - 40.0f; 

      DrawRectangleRounded({PP_X - 5.0f, PP_Y - 5.0f, PP_W + 10.0f, PP_H + 10.0f}, 0.40f, 8,
          (currentState == PLAYING) ? Color{50, 180, 70, 80} : Color{200, 60, 60, 80});

      GuiSetStyle(DEFAULT, TEXT_SIZE, 28);
      GuiSetAlpha(0.92f);
      if (GuiButton({PP_X, PP_Y, PP_W, PP_H}, (currentState == PLAYING) ? "|| PAUSE" : ">  PLAY")) {
        currentState = (currentState == PLAYING) ? PAUSED : PLAYING;
      }
      GuiSetAlpha(1.0f);
      GuiSetStyle(DEFAULT, TEXT_SIZE, 10);
    }

    EndDrawing();
  }

  EnableCursor();
  for (size_t i = 0; i < planetModels.size(); i++) UnloadModel(planetModels[i]);
  UnloadShader(lightShader);
  CloseWindow();
  return 0;
}
