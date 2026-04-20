#pragma once

#include "JNIBridge.h"
#include "Planet.h"
#include "raylib.h"
#include "raymath.h"
#include <vector>

struct Fragment {
  Vector3 position;
  Vector3 velocity;
  float size;
  float life;
  Color color;
  bool isAlive;
};

void ProcessCollisions(std::vector<Planet> &activePlanets,
                       std::vector<Fragment> &activeFragments,
                       Planet *&selectedPlanet, bool &isTracking);
