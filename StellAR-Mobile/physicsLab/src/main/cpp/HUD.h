#pragma once

#include "Collision.h"
#include "JNIBridge.h"
#include "Planet.h"
#include "raylib.h"
#include <vector>

enum EngineState { PAUSED, PLAYING, SUMMARY };

void DrawSelectionReticle(Planet *selectedPlanet, Camera3D &camera);

void DrawControlsPanel(int screenWidth, int screenHeight,
                       Planet *&selectedPlanet, bool &isTracking,
                       EngineState &currentState,
                       std::vector<Planet> &activePlanets,
                       std::vector<Planet> &initialPlanets,
                       std::vector<Fragment> &activeFragments,
                       Planet *&prevSelectedPlanet,
                       float &prevSliderMass, float &prevSliderRadius,
                       float &settledMass, float &toastTimer);

void DrawStatusPanel(int screenWidth, EngineState currentState,
                     float &cameraSpeed);

void ProcessToastAndNarration(int screenWidth, int screenHeight,
                              Planet *selectedPlanet,
                              Planet *&prevSelectedPlanet,
                              std::vector<Planet> &activePlanets,
                              float &prevSliderMass, float &prevSliderRadius,
                              float &settledMass, float &toastTimer,
                              char *toastText, size_t toastTextSize,
                              float dt);

void DrawToast(int screenWidth, float &toastTimer, char *toastText, float dt);
void DrawCaptions(int screenWidth, int screenHeight, float dt);
void DrawKillFeed(int screenWidth, float dt);
void DrawPlayPauseButton(int screenWidth, int screenHeight,
                         EngineState &currentState);
void DrawHelpBar(int screenHeight);
