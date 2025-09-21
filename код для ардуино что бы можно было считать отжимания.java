#include <Wire.h>
#include <LiquidCrystal_I2C.h>

LiquidCrystal_I2C lcd(0x27, 16, 2);

// Пины ультразвукового датчика
const int trigPin = 9;
const int echoPin = 10;

// Переменные для счетчика
unsigned long pushups = 0;
bool isDown = false;
unsigned long lastChangeTime = 0;

// Настройки позиций
const int DOWN_POSITION_MIN = 2;
const int DOWN_POSITION_MAX = 5;
const int UP_POSITION_MIN = 29;
const int UP_POSITION_MAX = 32;
const int DEBOUNCE_DELAY = 250;

// Анти-читерская система
const int HISTORY_SIZE = 6;
int movementHistory[HISTORY_SIZE];
int historyIndex = 0;
unsigned long lastMovementTime = 0;
int rapidMovements = 0;
unsigned long rapidCheckTime = 0;
int warningCount = 0;

// Фильтрация
int readings[5];
int readIndex = 0;
int total = 0;
int lastDistance = 0;

// ПОЛНЫЙ НАБОР РУССКИХ БУКВ 5x8 пикселей
// Каждый байт = строка, биты = пиксели (1=закрашено, 0=пусто)

// А
byte RUS_A[8] = {
  0b01110,  // .###.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11111,  // #####
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Б
byte RUS_B[8] = {
  0b11111,  // #####
  0b10000,  // #....
  0b10000,  // #....
  0b11110,  // ####.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11110,  // ####.
  0b00000   // .....
};

// В
byte RUS_V[8] = {
  0b11110,  // ####.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11110,  // ####.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11110,  // ####.
  0b00000   // .....
};

// Г
byte RUS_G[8] = {
  0b11111,  // #####
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b00000   // .....
};

// Д
byte RUS_D[8] = {
  0b00110,  // ..##.
  0b01010,  // .#.#.
  0b01010,  // .#.#.
  0b01010,  // .#.#.
  0b01010,  // .#.#.
  0b11111,  // #####
  0b10001,  // #...#
  0b00000   // .....
};

// Е
byte RUS_E[8] = {
  0b11111,  // #####
  0b10000,  // #....
  0b10000,  // #....
  0b11110,  // ####.
  0b10000,  // #....
  0b10000,  // #....
  0b11111,  // #####
  0b00000   // .....
};

// Ё
byte RUS_YO[8] = {
  0b10001,  // #...# (точки)
  0b11111,  // #####
  0b10000,  // #....
  0b11110,  // ####.
  0b10000,  // #....
  0b10000,  // #....
  0b11111,  // #####
  0b00000   // .....
};

// Ж
byte RUS_ZH[8] = {
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b01110,  // .###.
  0b00100,  // ..#..
  0b01110,  // .###.
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b00000   // .....
};

// З
byte RUS_Z[8] = {
  0b01110,  // .###.
  0b10001,  // #...#
  0b00001,  // ....#
  0b01110,  // .###.
  0b00001,  // ....#
  0b10001,  // #...#
  0b01110,  // .###.
  0b00000   // .....
};

// И
byte RUS_I[8] = {
  0b10001,  // #...#
  0b10001,  // #...#
  0b10011,  // #..##
  0b10101,  // #.#.#
  0b11001,  // ##..#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Й
byte RUS_II[8] = {
  0b01010,  // .#.#. (краткая)
  0b10001,  // #...#
  0b10011,  // #..##
  0b10101,  // #.#.#
  0b11001,  // ##..#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// К
byte RUS_K[8] = {
  0b10001,  // #...#
  0b10010,  // #..#.
  0b10100,  // #.#..
  0b11000,  // ##...
  0b10100,  // #.#..
  0b10010,  // #..#.
  0b10001,  // #...#
  0b00000   // .....
};

// Л
byte RUS_L[8] = {
  0b00111,  // ..###
  0b01001,  // .#..#
  0b01001,  // .#..#
  0b01001,  // .#..#
  0b01001,  // .#..#
  0b11001,  // ##..#
  0b10001,  // #...#
  0b00000   // .....
};

// М
byte RUS_M[8] = {
  0b10001,  // #...#
  0b11011,  // ##.##
  0b10101,  // #.#.#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Н
byte RUS_N[8] = {
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b11111,  // #####
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// О
byte RUS_O[8] = {
  0b01110,  // .###.
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b01110,  // .###.
  0b00000   // .....
};

// П
byte RUS_P[8] = {
  0b11111,  // #####
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Р
byte RUS_R[8] = {
  0b11110,  // ####.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11110,  // ####.
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b00000   // .....
};

// С
byte RUS_S[8] = {
  0b01110,  // .###.
  0b10001,  // #...#
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b10001,  // #...#
  0b01110,  // .###.
  0b00000   // .....
};

// Т
byte RUS_T[8] = {
  0b11111,  // #####
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00000   // .....
};

// У
byte RUS_U[8] = {
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b01111,  // .####
  0b00001,  // ....#
  0b00001,  // ....#
  0b00001,  // ....#
  0b00000   // .....
};

// Ф
byte RUS_F[8] = {
  0b00100,  // ..#..
  0b01110,  // .###.
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b01110,  // .###.
  0b00100,  // ..#..
  0b00100,  // ..#..
  0b00000   // .....
};

// Х
byte RUS_H[8] = {
  0b10001,  // #...#
  0b10001,  // #...#
  0b01010,  // .#.#.
  0b00100,  // ..#..
  0b01010,  // .#.#.
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Ц
byte RUS_TS[8] = {
  0b10010,  // #..#.
  0b10010,  // #..#.
  0b10010,  // #..#.
  0b10010,  // #..#.
  0b10010,  // #..#.
  0b11111,  // #####
  0b00001,  // ....#
  0b00000   // .....
};

// Ч
byte RUS_CH[8] = {
  0b10001,  // #...#
  0b10001,  // #...#
  0b10001,  // #...#
  0b01111,  // .####
  0b00001,  // ....#
  0b00001,  // ....#
  0b00001,  // ....#
  0b00000   // .....
};

// Ш
byte RUS_SH[8] = {
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b11111,  // #####
  0b00000   // .....
};

// Щ
byte RUS_SCH[8] = {
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b11111,  // #####
  0b00001,  // ....#
  0b00000   // .....
};

// Ъ
byte RUS_TVERD[8] = {
  0b11000,  // ##...
  0b01000,  // .#...
  0b01000,  // .#...
  0b01110,  // .###.
  0b01001,  // .#..#
  0b01001,  // .#..#
  0b01110,  // .###.
  0b00000   // .....
};

// Ы
byte RUS_Y[8] = {
  0b10000,  // #....
  0b10000,  // #....
  0b10110,  // #.##.
  0b11001,  // ##..#
  0b10001,  // #...#
  0b11001,  // ##..#
  0b10110,  // #.##.
  0b00000   // .....
};

// Ь
byte RUS_MYAGK[8] = {
  0b10000,  // #....
  0b10000,  // #....
  0b10000,  // #....
  0b11110,  // ####.
  0b10001,  // #...#
  0b10001,  // #...#
  0b11110,  // ####.
  0b00000   // .....
};

// Э
byte RUS_EH[8] = {
  0b01110,  // .###.
  0b10001,  // #...#
  0b00001,  // ....#
  0b01111,  // .####
  0b00001,  // ....#
  0b10001,  // #...#
  0b01110,  // .###.
  0b00000   // .....
};

// Ю
byte RUS_YU[8] = {
  0b10010,  // #..#.
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b11101,  // ###.#
  0b10101,  // #.#.#
  0b10101,  // #.#.#
  0b10010,  // #..#.
  0b00000   // .....
};

// Я
byte RUS_YA[8] = {
  0b01111,  // .####
  0b10001,  // #...#
  0b10001,  // #...#
  0b01111,  // .####
  0b01001,  // .#..#
  0b10001,  // #...#
  0b10001,  // #...#
  0b00000   // .....
};

// Переменная для отслеживания загруженных символов
int currentSymbolSet = 0;

// Функции загрузки наборов символов для конкретных слов
void loadSymbolsForSCHYOT() {
  if (currentSymbolSet != 1) {
    lcd.createChar(0, RUS_S);   // С
    lcd.createChar(1, RUS_CH);  // Ч
    lcd.createChar(2, RUS_YO);  // Ё
    lcd.createChar(3, RUS_T);   // Т
    currentSymbolSet = 1;
  }
}

void loadSymbolsForVNIMANIE() {
  if (currentSymbolSet != 2) {
    lcd.createChar(0, RUS_V);   // В
    lcd.createChar(1, RUS_N);   // Н
    lcd.createChar(2, RUS_I);   // И
    lcd.createChar(3, RUS_M);   // М
    lcd.createChar(4, RUS_A);   // А
    lcd.createChar(5, RUS_N);   // Н (повторно)
    lcd.createChar(6, RUS_I);   // И (повторно)
    lcd.createChar(7, RUS_E);   // Е
    currentSymbolSet = 2;
  }
}

void loadSymbolsForUMNYI() {
  if (currentSymbolSet != 3) {
    lcd.createChar(0, RUS_U);   // У
    lcd.createChar(1, RUS_M);   // М
    lcd.createChar(2, RUS_N);   // Н
    lcd.createChar(3, RUS_Y);   // Ы
    lcd.createChar(4, RUS_I);   // И
    currentSymbolSet = 3;
  }
}

// Функции для печати русских слов
void printSCHYOT() {
  loadSymbolsForSCHYOT();
  lcd.write(byte(0)); // С
  lcd.write(byte(1)); // Ч
  lcd.write(byte(2)); // Ё
  lcd.write(byte(3)); // Т
}

void printVNIMANIE() {
  loadSymbolsForVNIMANIE();
  lcd.write(byte(0)); // В
  lcd.write(byte(1)); // Н
  lcd.write(byte(2)); // И
  lcd.write(byte(3)); // М
  lcd.write(byte(4)); // А
  lcd.write(byte(5)); // Н
  lcd.write(byte(6)); // И
  lcd.write(byte(7)); // Е
}

void printUMNYI() {
  loadSymbolsForUMNYI();
  lcd.write(byte(0)); // У
  lcd.write(byte(1)); // М
  lcd.write(byte(2)); // Н
  lcd.write(byte(3)); // Ы
  lcd.write(byte(4)); // И
}

void setup() {
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  lcd.init();
  lcd.backlight();
  
  // Приветственный экран с полными русскими словами
  lcd.setCursor(0, 0);
  printUMNYI();
  lcd.print(" ");
  printSCHYOT();
  
  lcd.setCursor(0, 1);
  lcd.print("Anti-Cheat ON");
  
  // Инициализация массивов
  for (int i = 0; i < 5; i++) {
    readings[i] = 0;
  }
  
  for (int i = 0; i < HISTORY_SIZE; i++) {
    movementHistory[i] = 0;
  }

  Serial.begin(9600);
  Serial.println("=== УМНЫЙ СЧЕТЧИК ОТЖИМАНИЙ ===");
  Serial.println("Все буквы нарисованы по пикселям 5x8");
  
  delay(3000);
  updateMainDisplay();
}

void loop() {
  unsigned long currentTime = millis();
  
  int distance = getFilteredDistance();
  
  if (distance == -1) {
    delay(50);
    return;
  }
  
  // Отладка
  static unsigned long lastDebug = 0;
  if (currentTime - lastDebug > 1000) {
    Serial.print("Расстояние: ");
    Serial.print(distance);
    Serial.print(" см, Down: ");
    Serial.print(isInDownPosition(distance) ? "ДА" : "НЕТ");
    Serial.print(", Up: ");
    Serial.print(isInUpPosition(distance) ? "ДА" : "НЕТ");
    lastDebug = currentTime;
  }
  
  if (currentTime - rapidCheckTime > 10000) {
    rapidMovements = max(0, rapidMovements - 1);
    rapidCheckTime = currentTime;
  }
  
  int movement = 0;
  if (distance > lastDistance + 2) movement = 1;
  else if (distance < lastDistance - 2) movement = -1;
  
  if (movement != 0) {
    addMovementToHistory(movement);
    lastMovementTime = currentTime;
  }
  
  lastDistance = distance;
  
  bool currentlyDown = isInDownPosition(distance);
  bool currentlyUp = isInUpPosition(distance);
  
  if (currentlyDown && !isDown) {
    if (currentTime - lastChangeTime > DEBOUNCE_DELAY) {
      isDown = true;
      lastChangeTime = currentTime;
      Serial.println("✓ ВНИЗ");
      
      updateMainDisplay();
      lcd.setCursor(15, 0);
      lcd.print("v");
    }
  }
  
  else if (currentlyUp && isDown) {
    if (currentTime - lastChangeTime > DEBOUNCE_DELAY) {
      
      bool suspicious = false;
      String reason = "";
      
      if (isRapidMovementPattern()) {
        rapidMovements++;
        if (rapidMovements >= 5) {
          suspicious = true;
          reason = "Быстро!";
        }
      }
      
      if (!suspicious && isMovementTooMechanical()) {
        suspicious = true;
        reason = "Подозр.!";
      }
      
      if (!suspicious) {
        pushups++;
        isDown = false;
        lastChangeTime = currentTime;
        warningCount = 0;
        
        Serial.print("🎉 ОТЖИМАНИЕ #");
        Serial.println(pushups);
        
        updateMainDisplay();
        showSuccess();
        
      } else {
        warningCount++;
        Serial.print("⚠️ ПРЕДУПРЕЖДЕНИЕ: ");
        Serial.println(reason);
        
        showWarning(reason);
        isDown = false;
        
        if (warningCount >= 3) {
          showCooldownPeriod();
          warningCount = 0;
        }
      }
      
      lcd.setCursor(15, 0);
      lcd.print("^");
    }
  }
  
  delay(50);
}

// Остальные функции остаются такими же...
void addMovementToHistory(int movement) {
  movementHistory[historyIndex] = movement;
  historyIndex = (historyIndex + 1) % HISTORY_SIZE;
}

bool isRapidMovementPattern() {
  int directionChanges = 0;
  for (int i = 1; i < HISTORY_SIZE; i++) {
    int prev = movementHistory[(historyIndex - i - 1 + HISTORY_SIZE) % HISTORY_SIZE];
    int curr = movementHistory[(historyIndex - i + HISTORY_SIZE) % HISTORY_SIZE];
    if (prev != 0 && curr != 0 && prev != curr) {
      directionChanges++;
    }
  }
  return directionChanges > 3;
}

bool isMovementTooMechanical() {
  int upCount = 0, downCount = 0;
  for (int i = 0; i < HISTORY_SIZE; i++) {
    if (movementHistory[i] == 1) upCount++;
    else if (movementHistory[i] == -1) downCount++;
  }
  bool tooBalanced = abs(upCount - downCount) <= 1;
  bool noStability = (upCount + downCount) == HISTORY_SIZE;
  return tooBalanced && noStability && (upCount > 2);
}

void showCooldownPeriod() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("СТОП!");
  lcd.setCursor(0, 1);
  lcd.print("Жди 3 сек");
  
  for (int i = 3; i > 0; i--) {
    lcd.setCursor(14, 1);
    lcd.print(i);
    delay(1000);
  }
  updateMainDisplay();
}

int getFilteredDistance() {
  int rawDistance = measureDistance();
  if (rawDistance == -1) return -1;
  
  total = total - readings[readIndex];
  readings[readIndex] = rawDistance;
  total = total + readings[readIndex];
  readIndex = (readIndex + 1) % 5;
  
  return total / 5;
}

int measureDistance() {
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);

  long duration = pulseIn(echoPin, HIGH, 30000);
  if (duration == 0) return -1;
  
  int distance = duration * 0.034 / 2;
  if (distance < 1 || distance > 400) return -1;
  
  return distance;
}

bool isInDownPosition(int distance) {
  return (distance >= DOWN_POSITION_MIN && distance <= DOWN_POSITION_MAX);
}

bool isInUpPosition(int distance) {
  return (distance >= UP_POSITION_MIN && distance <= UP_POSITION_MAX);
}

void updateMainDisplay() {
  lcd.clear();
  lcd.setCursor(0, 0);
  
  printSCHYOT();
  lcd.print(": ");
  lcd.print(pushups);
  
  if (warningCount > 0) {
    lcd.setCursor(0, 1);
    lcd.print("Предупр: ");
    lcd.print(warningCount);
    lcd.print("/3");
  }
}

void showSuccess() {
  for (int i = 0; i < 3; i++) {
    lcd.setCursor(15, 1);
    lcd.print("!");
    delay(150);
    lcd.setCursor(15, 1);
    lcd.print(" ");
    delay(150);
  }
}

void showWarning(String reason) {
  lcd.clear();
  lcd.setCursor(0, 0);
  
  printVNIMANIE();
  lcd.print("!");
  
  lcd.setCursor(0, 1);
  lcd.print(reason);
  
  delay(1500);
  updateMainDisplay();
}