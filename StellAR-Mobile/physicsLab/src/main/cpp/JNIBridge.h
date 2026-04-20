#pragma once

#include <queue>
#include <string>
#include <vector>

struct CaptionEntry {
  std::string displayText;
  int wordCount;
};

struct KillFeedEntry {
  std::string survivorName;
  std::string victimName;
  float timer;
};

extern std::queue<CaptionEntry> captionQueue;
extern std::string currentTTSCaption;
extern float ttsCaptionTimer;
extern std::vector<KillFeedEntry> killFeed;

int CountWords(const char *s);

void RequestAINarration(const char *contextPrompt);
void PollAINarration();
