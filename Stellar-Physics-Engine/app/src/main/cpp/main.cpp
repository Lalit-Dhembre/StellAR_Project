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
  Vector3 position;
  float radius;
  float maxRadius;
  float alpha;
  bool isAlive;
};
std::vector<Explosion> activeExplosions;

int main() {
  const int screenWidth = 1920;
  const int screenHeight = 1080;

  InitWindow(screenWidth, screenHeight, "Stardust");

  // Lighting Setup
#if defined(PLATFORM_ANDROID)
  Shader lightShader = LoadShader("resources/shaders/glsl100/lighting.vs",
                                  "resources/shaders/glsl100/lighting.fs");
#else
  Shader lightShader = LoadShader("resources/shaders/glsl330/lighting.vs",
                                  "resources/shaders/glsl330/lighting.fs");
#endif

  int viewPosLoc = GetShaderLocation(lightShader, "viewPos");

  // Sun point light
  Light sun = CreateLight(LIGHT_POINT,
                          Vector3{0.0f, 0.0f, 0.0f},
                          Vector3Zero(),
                          WHITE, lightShader);

  // Camera Configuration
  Camera3D camera = {};
  camera.position = Vector3{0.0f, 160.0f, 200.0f};
  camera.target = Vector3{0.0f, 0.0f, 0.0f};
  camera.up = Vector3{0.0f, 1.0f, 0.0f};
  camera.fovy = 45.0f;
  camera.projection = CAMERA_PERSPECTIVE;

  // Planet Storage
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

  // Asset Loading
  for (size_t i = 0; i < initialPlanets.size(); i++) {
    planetModels.push_back(LoadModel(initialPlanets[i].modelPath.c_str()));
  }

  Shader defaultSunShader = planetModels[0].materials[0].shader;

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

  // Toast System State
  float toastTimer = 0.0f;
  char toastText[192] = {0};
  float prevSliderMass = -1.0f;
  float prevSliderRadius = -1.0f;

  const float G = 1.0f;

  while (!WindowShouldClose()) {
    float dt = GetFrameTime();

    // Camera Control (PC)
    if (IsMouseButtonDown(MOUSE_BUTTON_RIGHT)) {
      if (!isCameraActive) {
        DisableCursor();
        isCameraActive = true;
      }

      UpdateCamera(&camera, CAMERA_FREE);

      Vector3 forward = Vector3Normalize(Vector3Subtract(camera.target, camera.position));
      Vector3 right = Vector3Normalize(Vector3CrossProduct(forward, camera.up));
      float moveAmount = cameraSpeed * dt;

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

      float wheel = GetMouseWheelMove();
      if (wheel != 0.0f) {
        cameraSpeed = Clamp(cameraSpeed + wheel * 1.0f, 0.5f, 50.0f);
      }
    } else {
      if (isCameraActive) {
        EnableCursor();
        isCameraActive = false;
      }
    }

    // Input Handling
    if (IsKeyPressed(KEY_SPACE)) {
      currentState = (currentState == PLAYING) ? PAUSED : PLAYING;
    }

    // Selection Logic
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
      const int SUB_STEPS = 10;
      float subDt = dt / SUB_STEPS;

      for (int step = 0; step < SUB_STEPS; step++) {
        for (size_t i = 0; i < activePlanets.size(); i++) {
          for (size_t j = i + 1; j < activePlanets.size(); j++) {
            if (!activePlanets[i].isAlive || !activePlanets[j].isAlive) continue;

            ApplyGravity(activePlanets[i], activePlanets[i].mass, activePlanets[j], G, subDt);
            ApplyGravity(activePlanets[j], activePlanets[j].mass, activePlanets[i], G, subDt);

            // Collision Detection
            float distSqr = Vector3DistanceSqr(activePlanets[i].position, activePlanets[j].position);
            float combinedRadii = activePlanets[i].radius + activePlanets[j].radius;
            float collisionThreshold = combinedRadii * 0.8f;

            if (distSqr < (collisionThreshold * collisionThreshold)) {
              activePlanets[i].isAlive = false;
              activePlanets[j].isAlive = false;

              if (selectedPlanet == &activePlanets[i] || selectedPlanet == &activePlanets[j]) {
                selectedPlanet = nullptr;
              }

              // Spawn Explosion
              Vector3 midpoint = Vector3Lerp(activePlanets[i].position, activePlanets[j].position, 0.5f);
              activeExplosions.push_back({midpoint, combinedRadii * 0.5f, combinedRadii * 3.0f, 1.0f, true});
            }
          }
        }
      }

      for (size_t i = 0; i < activePlanets.size(); i++) {
        if (activePlanets[i].isAlive) UpdatePosition(activePlanets[i], dt);
      }
    }

    // Shader Update
    float camPos[3] = {camera.position.x, camera.position.y, camera.position.z};
    SetShaderValue(lightShader, viewPosLoc, camPos, SHADER_UNIFORM_VEC3);

    // Rendering
    BeginDrawing();
    ClearBackground(BLACK);
    BeginMode3D(camera);

    for (size_t i = 0; i < activePlanets.size(); i++) {
      if (!activePlanets[i].isAlive) continue;

      Vector3 modelScale = {activePlanets[i].radius, activePlanets[i].radius, activePlanets[i].radius};
      
      if (currentState == PLAYING) {
        activePlanets[i].rotationAngle += activePlanets[i].rotationSpeed * dt * RAD2DEG;
      }

      // Emissive Sun handling
      if (i == 0) {
        for (int m = 0; m < planetModels[i].materialCount; m++) planetModels[i].materials[m].shader = defaultSunShader;
      }

      DrawModelEx(planetModels[i], activePlanets[i].position, Vector3{0.0f, 1.0f, 0.0f}, activePlanets[i].rotationAngle, modelScale, WHITE);

      if (i == 0) {
        for (int m = 0; m < planetModels[i].materialCount; m++) planetModels[i].materials[m].shader = lightShader;
      }
    }

    if (selectedPlanet != nullptr) {
      DrawSphereWires(selectedPlanet->position, selectedPlanet->radius * 1.1f, 16, 16, YELLOW);
    }

    // Explosions
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
    activeExplosions.erase(std::remove_if(activeExplosions.begin(), activeExplosions.end(), [](const Explosion &e) { return !e.isAlive; }), activeExplosions.end());

    EndMode3D();

    // 2D Interface
    {
      const float PNL_X = 10.0f;
      const float PNL_Y = 10.0f;
      const float PNL_W = 449.0f;
      const float PNL_H = 221.0f;
      const float PAD = 16.0f;
      const float SLD_X = PNL_X + 110.0f;
      const float SLD_W = PNL_W - 110.0f - PAD - 68.0f;
      const float ROW_H = 40.0f;

      DrawRectangleRounded({PNL_X, PNL_Y, PNL_W, PNL_H}, 0.06f, 8, {5, 5, 15, 175});
      DrawRectangleRoundedLinesEx({PNL_X, PNL_Y, PNL_W, PNL_H}, 0.06f, 8, 1.5f, {55, 55, 85, 210});

      if (selectedPlanet != nullptr) {
        DrawText(selectedPlanet->name.c_str(), (int)(PNL_X + PAD), (int)(PNL_Y + PAD), 26, YELLOW);
        GuiSlider({SLD_X, PNL_Y + 62.0f, SLD_W, ROW_H}, "Mass", TextFormat("%.2f", selectedPlanet->mass), &selectedPlanet->mass, selectedPlanet->massMin, selectedPlanet->massMax);
        GuiSlider({SLD_X, PNL_Y + 113.0f, SLD_W, ROW_H}, "Radius", TextFormat("%.2f", selectedPlanet->radius), &selectedPlanet->radius, selectedPlanet->radiusMin, selectedPlanet->radiusMax);
      } else {
        DrawText("Tap a planet to select", (int)(PNL_X + PAD), (int)(PNL_Y + PAD), 22, {100, 100, 100, 200});
      }

      if (GuiButton({PNL_X + PAD, PNL_Y + 164.0f, PNL_W - PAD * 2.0f, ROW_H}, "RESET SIMULATION")) {
        selectedPlanet = nullptr;
        prevSelectedPlanet = nullptr;
        currentState = PAUSED;
        activePlanets = initialPlanets;
        activeExplosions.clear();
      }
    }

    // Status Panel
    {
      const float RP_W = 422.0f;
      const float RP_X = (float)screenWidth - RP_W - 10.0f;
      const float RP_H = 122.0f;
      DrawRectangleRounded({RP_X, 10.0f, RP_W, RP_H}, 0.06f, 8, {5, 5, 15, 175});
      DrawRectangleRoundedLinesEx({RP_X, 10.0f, RP_W, RP_H}, 0.06f, 8, 1.5f, {55, 55, 85, 210});

      bool isPlaying = (currentState == PLAYING);
      DrawCircleV({RP_X + 24.0f, 39.0f}, 10.0f, isPlaying ? GREEN : RED);
      DrawText(isPlaying ? "PLAYING" : "PAUSED", (int)(RP_X + 43.0f), 27, 26, isPlaying ? GREEN : RED);
      GuiSlider({RP_X + 116.0f, 78.0f, RP_W - 134.0f, 37.0f}, "Cam Spd", TextFormat("%.1f", cameraSpeed), &cameraSpeed, 0.5f, 50.0f);
    }

    // Toast Notifications
    if (selectedPlanet != nullptr) {
      if (selectedPlanet != prevSelectedPlanet) {
        prevSelectedPlanet = selectedPlanet;
        prevSliderMass = selectedPlanet->mass;
        prevSliderRadius = selectedPlanet->radius;
      }

      if (prevSliderMass != selectedPlanet->mass) {
        prevSliderMass = selectedPlanet->mass;
        toastTimer = 3.0f;
        double earthRatio = (double)selectedPlanet->mass / 10.0;
        snprintf(toastText, sizeof(toastText), "%s | %.3e kg | %.3gx Earth mass", selectedPlanet->name.c_str(), earthRatio * 5.972e24, earthRatio);
      }

      if (prevSliderRadius != selectedPlanet->radius) {
        prevSliderRadius = selectedPlanet->radius;
        toastTimer = 3.0f;
        double earthRadiusRatio = (double)selectedPlanet->radius / 0.55;
        snprintf(toastText, sizeof(toastText), "%s | %.0f km | %.3gx Earth radius", selectedPlanet->name.c_str(), earthRadiusRatio * 6371.0, earthRadiusRatio);
      }
    }

    if (toastTimer > 0.0f) {
      toastTimer -= dt;
      unsigned char alpha = (toastTimer < 0.5f) ? (unsigned char)((toastTimer / 0.5f) * 255.0f) : 255;
      int tw = MeasureText(toastText, 22);
      DrawRectangleRounded({(float)(screenWidth/2 - tw/2 - 22), 165, (float)(tw + 44), 44}, 0.45f, 8, {0, 0, 0, (unsigned char)(alpha * 0.65f)});
      DrawText(toastText, screenWidth/2 - tw/2, 175, 22, {255, 228, 110, alpha});
    }

    // Controls Hint
    DrawText("SPACE: Play/Pause | RMB + WASD: Free Camera | LMB: Select", 10, screenHeight - 28, 15, {90, 90, 90, 200});

    // Mobile Navigation ( PUBG Style )
    if (!isCameraActive) {
      const float JS_BASE_RADIUS = 156.0f;
      const float MARGIN = 96.0f;
      Vector2 jsBase = { MARGIN + JS_BASE_RADIUS, (float)screenHeight - MARGIN - JS_BASE_RADIUS };

      // Elevation control
      Rectangle btnUp = { (float)screenWidth - 160.0f, (float)screenHeight - 360.0f, 120.0f, 100.0f };
      Rectangle btnDown = { (float)screenWidth - 160.0f, (float)screenHeight - 220.0f, 120.0f, 100.0f };

      Vector3 camForward = Vector3Normalize(Vector3Subtract(camera.target, camera.position));
      Vector3 camRight = Vector3Normalize(Vector3CrossProduct(camForward, {0,1,0}));
      float moveAmount = cameraSpeed * dt;

      // ... [ Touch logic remains functionally identical but stripped of essays ] ...
      // I'll keep the touch logic block intact as it's purely logical
      int tc = GetTouchPointCount();
      auto isRectPressed = [&](Rectangle rect) {
        for (int t = 0; t < tc; t++) if (CheckCollisionPointRec(GetTouchPosition(t), rect)) return true;
        return IsMouseButtonDown(MOUSE_LEFT_BUTTON) && CheckCollisionPointRec(GetMousePosition(), rect);
      };

      if (isRectPressed(btnUp)) { camera.position.y += moveAmount; camera.target.y += moveAmount; }
      if (isRectPressed(btnDown)) { camera.position.y -= moveAmount; camera.target.y -= moveAmount; }

      bool currentJoystickHeld = false;
      bool currentLookHeld = false;

      for (int t = 0; t < tc; t++) {
        Vector2 tp = GetTouchPosition(t);
        int tid = GetTouchPointId(t);
        if (CheckCollisionPointRec(tp, btnUp) || CheckCollisionPointRec(tp, btnDown)) continue;

        if (joystickActive && tid == joystickTouchPointId) {
          currentJoystickHeld = true;
          float dist = Vector2Distance(tp, joystickCenter);
          if (dist > JS_BASE_RADIUS) joystickThumb = Vector2Add(joystickCenter, Vector2Scale(Vector2Normalize(Vector2Subtract(tp, joystickCenter)), JS_BASE_RADIUS));
          else joystickThumb = tp;
        } else if (lookActive && tid == lookTouchPointId) {
          currentLookHeld = true;
          Vector2 delta = Vector2Subtract(tp, lastLookPos);
          lastLookPos = tp;
          camForward = Vector3RotateByAxisAngle(camForward, {0,1,0}, -delta.x * 0.004f);
          Vector3 right = Vector3Normalize(Vector3CrossProduct(camForward, {0,1,0}));
          Vector3 newForward = Vector3RotateByAxisAngle(camForward, right, -delta.y * 0.004f);
          if (fabsf(Vector3DotProduct(newForward, {0,1,0})) < 0.95f) camForward = newForward;
          camera.target = Vector3Add(camera.position, camForward);
        } else if (!joystickActive && tp.x < screenWidth/2.0f) {
          joystickActive = true; joystickTouchPointId = tid; joystickCenter = jsBase; joystickThumb = tp; currentJoystickHeld = true;
        } else if (!lookActive && tp.x >= screenWidth/2.0f) {
          lookActive = true; lookTouchPointId = tid; lastLookPos = tp; currentLookHeld = true;
        }
      }

      if (!currentJoystickHeld) { joystickActive = false; joystickThumb = jsBase; }
      if (!currentLookHeld) lookActive = false;

      if (joystickActive) {
        float jX = (joystickThumb.x - joystickCenter.x) / JS_BASE_RADIUS;
        float jY = (joystickThumb.y - joystickCenter.y) / JS_BASE_RADIUS;
        if (fabsf(jX) > 0.1f) { camera.position = Vector3Add(camera.position, Vector3Scale(camRight, jX * moveAmount)); camera.target = Vector3Add(camera.target, Vector3Scale(camRight, jX * moveAmount)); }
        if (fabsf(jY) > 0.1f) { camera.position = Vector3Subtract(camera.position, Vector3Scale(camForward, jY * moveAmount)); camera.target = Vector3Subtract(camera.target, Vector3Scale(camForward, jY * moveAmount)); }
      }

      DrawCircleV(jsBase, JS_BASE_RADIUS, {50, 50, 50, 120});
      DrawCircleV(joystickThumb, 54.0f, joystickActive ? Color{255, 255, 255, 220} : Color{160, 160, 160, 170});
      GuiButton(btnUp, "^ FLY UP"); GuiButton(btnDown, "v FLY DN");
    }

    EndDrawing();
  }

  EnableCursor();
  for (size_t i = 0; i < planetModels.size(); i++) UnloadModel(planetModels[i]);
  UnloadShader(lightShader);
  CloseWindow();
  return 0;
}
