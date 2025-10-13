import serial
import serial.tools.list_ports
import threading
import time
import tkinter as tk
from tkinter import messagebox
import re
import os
import sys

# === Автоопределение COM-порта ===
def find_arduino_port():
    ports = list(serial.tools.list_ports.comports())
    for port in ports:
        if "Arduino" in port.description or "CH340" in port.description or "USB-SERIAL" in port.description:
            return port.device
    # Если Arduino не найдена, спросим вручную
    messagebox.showwarning("Arduino не найдена", "⚠️ Не удалось найти Arduino. Подключи её и попробуй снова.")
    return None

# === Ищем Arduino ===
SERIAL_PORT = find_arduino_port()
BAUD_RATE = 9600

ser = None
if SERIAL_PORT:
    try:
        ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
        print(f"✅ Подключено к {SERIAL_PORT}")
    except Exception as e:
        messagebox.showerror("Ошибка", f"❌ Не удалось подключиться к {SERIAL_PORT}\n\n{e}")
else:
    print("❌ Arduino не найдена.")
    sys.exit(1)

# === Глобальные переменные ===
count = 0
goal_reached = False
started = False
last_count = 0

# === Функция чтения данных с Arduino ===
def read_from_arduino():
    global count, goal_reached, started, last_count
    while True:
        if ser and ser.in_waiting:
            line = ser.readline().decode(errors='ignore').strip()

            if "ОТЖИМАНИЕ #" in line:
                match = re.search(r"ОТЖИМАНИЕ #(\d+)", line)
                if match:
                    count = int(match.group(1))
                    if not started and count > 0:
                        started = True
                        label_status.config(text="🏃 Ты начал делать отжимания", fg="blue")
                    elif started and count != last_count:
                        label_status.config(text="💪 В процессе...", fg="orange")
                    last_count = count

            elif "ЦЕЛЬ ДОСТИГНУТА" in line:
                goal_reached = True
                label_status.config(text="🎉 Ты закончил отжимания!", fg="green")
                messagebox.showinfo("Поздравляю!", "🎉 Ты закончил отжимания!")
        time.sleep(0.1)

# === Интерфейс ===
window = tk.Tk()
window.title("Счётчик отжиманий 💪")
window.geometry("320x160")

label_status = tk.Label(window, text="Подключи Arduino и начни!", font=("Arial", 14))
label_status.pack(pady=40)

# === Кнопка выхода ===
def close_app():
    if ser:
        ser.close()
    window.destroy()

btn_exit = tk.Button(window, text="Выход", command=close_app, font=("Arial", 12))
btn_exit.pack(pady=10)

# === Поток чтения данных ===
thread = threading.Thread(target=read_from_arduino, daemon=True)
thread.start()

window.mainloop()
