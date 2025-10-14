#include <Wire.h>
#include <LiquidCrystal_I2C.h>

LiquidCrystal_I2C lcd(0x27, 16, 2);

const int trigPin = 9;
const int echoPin = 10;
const int buzzerPin = 3;

unsigned long pushupsDone = 0;
unsigned long totalRequired = 30;
const int INITIAL_TARGET = 30;

const int ZONE_TOP = 0;
const int ZONE_MID = 1;
const int ZONE_BOTTOM = 2;

const int TOP_MIN = 25;
const int TOP_MAX = 35;
const int MID_MIN = 10;
const int MID_MAX = 25;
const int BOTTOM_MIN = 2;
const int BOTTOM_MAX = 10;

int currentZone = -1;
bool wasAtBottom = false;
bool wasAtTop = false;
bool goalReached = false;

bool beepedAtBottom = false;
bool beepedAtTop = false;

void setup() {
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  pinMode(buzzerPin, OUTPUT);

  lcd.init();
  lcd.backlight();

  Serial.begin(9600);
  Serial.println("=== БЫСТРЫЙ СЧЕТЧИК ОТЖИМАНИЙ ===");

  updateDisplay();
}

void loop() {
  if (goalReached) return; // Если цель достигнута — ничего не делаем

  int distance = getDistance();
  if (distance == -1) return;

  int zone = getZone(distance);

  if (zone != currentZone) {
    handleZoneChange(zone);
    currentZone = zone;
  }
}

int getDistance() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  long duration = pulseIn(echoPin, HIGH, 20000);
  if (duration == 0) return -1;

  int distance = duration * 0.034 / 2;
  if (distance < 2 || distance > 100) return -1;

  return distance;
}

int getZone(int distance) {
  if (distance >= TOP_MIN && distance <= TOP_MAX) return ZONE_TOP;
  if (distance >= MID_MIN && distance <= MID_MAX) return ZONE_MID;
  if (distance >= BOTTOM_MIN && distance <= BOTTOM_MAX) return ZONE_BOTTOM;
  return -1;
}

void handleZoneChange(int newZone) {
  if (goalReached) return;

  if (newZone == ZONE_TOP) {
    if (!beepedAtTop) {
      quickBeep(1);
      beepedAtTop = true;
      beepedAtBottom = false;
      Serial.print("ВВЕРХ - осталось: ");
      Serial.println(totalRequired - pushupsDone);
    }

    if (wasAtBottom) {
      wasAtTop = true;
      wasAtBottom = false;
      pushupsDone++;

      Serial.print("✓ ОТЖИМАНИЕ #");
      Serial.print(pushupsDone);
      Serial.print(" - осталось: ");
      Serial.println(totalRequired - pushupsDone);

      updateDisplay();

      if (pushupsDone >= totalRequired) {
        completionCelebration();
      }
    }
  }

  if (newZone == ZONE_MID) {
    static bool beepedAtMid = false;
    if (!beepedAtMid) {
      quickBeep(1);
      beepedAtMid = true;
      beepedAtTop = false;
      beepedAtBottom = false;
      Serial.println("СЕРЕДИНА (планка)");
    }
    if (currentZone != ZONE_MID) {
      beepedAtMid = false;
    }
  }

  if (newZone == ZONE_BOTTOM) {
    wasAtBottom = true;
    if (!beepedAtBottom) {
      quickBeep(2);
      beepedAtBottom = true;
      beepedAtTop = false;
      Serial.print("ВНИЗ - осталось: ");
      Serial.println(totalRequired - pushupsDone);
    }
  }
}

void quickBeep(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(buzzerPin, HIGH);
    delayMicroseconds(50000);
    digitalWrite(buzzerPin, LOW);
    if (i < times - 1) delayMicroseconds(30000);
  }
}

void updateDisplay() {
  lcd.clear();
  int remaining = totalRequired - pushupsDone;
  if (remaining < 0) remaining = 0;

  String text = "Count: " + String(remaining);
  int textLength = text.length();
  int startPos = (16 - textLength) / 2;
  if (startPos < 0) startPos = 0;

  lcd.setCursor(startPos, 0);
  lcd.print(text);
}

void completionCelebration() {
  goalReached = true; // Больше не продолжаем
  Serial.println("🎉 ЦЕЛЬ ДОСТИГНУТА!");
  lcd.clear();

  String text = "30 push-ups made";
  int textLength = text.length();
  int startPos = (16 - textLength) / 2;
  if (startPos < 0) startPos = 0;
  lcd.setCursor(startPos, 0);
  lcd.print(text);

  // Праздничные сигналы
  for (int i = 0; i < 5; i++) {
    digitalWrite(buzzerPin, HIGH);
    delay(100);
    digitalWrite(buzzerPin, LOW);
    delay(100);
  }

  Serial.println("💪 Тренировка завершена!");
}