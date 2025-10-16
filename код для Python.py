import serial
import serial.tools.list_ports
import tkinter as tk
from tkinter import messagebox
import threading
import time
import requests

# --- Глобальные переменные ---
ser = None
connected = False
stop_thread = False


def find_arduino_port():
    """Автоматически находит COM-порт Arduino"""
    ports = serial.tools.list_ports.comports()
    for port in ports:
        if "Arduino" in port.description or "CH340" in port.description:
            return port.device
    return None


def connect_arduino():
    """Подключение к Arduino"""
    global ser, connected

    port = find_arduino_port()
    if not port:
        status_label.config(text="⚠️ Arduino не найдена. Подключите и нажмите 'Подключить'.", fg="orange")
        return

    try:
        ser = serial.Serial(port, 9600, timeout=1)
        connected = True
        status_label.config(text=f"✅ Подключено к {port}", fg="green")
        messagebox.showinfo("Подключено", f"Arduino подключена к {port}")
        threading.Thread(target=read_from_arduino, daemon=True).start()
    except Exception as e:
        status_label.config(text=f"❌ Ошибка подключения: {e}", fg="red")


def disconnect_arduino():
    """Отключение от Arduino"""
    global ser, connected
    if ser:
        ser.close()
    ser = None
    connected = False
    status_label.config(text="🔌 Arduino отключена", fg="gray")


def read_from_arduino():
    """Фоновое чтение данных от Arduino"""
    global ser, connected, stop_thread

    while not stop_thread:
        if connected and ser and ser.in_waiting > 0:
            try:
                line = ser.readline().decode(errors='ignore').strip()
                if "ОТЖИМАНИЕ" in line:
                    update_label("🏋️‍♂️ В процессе...")
                elif "ЦЕЛЬ ДОСТИГНУТА" in line:
                    update_label("🎉 Закончил!")
                elif "===" in line:
                    update_label("💪 Начал отжимания!")
            except Exception:
                pass
        time.sleep(0.1)


def update_label(text):
    """Обновление текста в окне"""
    info_label.config(text=text)


def fetch_pushup_goal():
    """Получает количество отжиманий с сайта"""
    if not connected or not ser:
        messagebox.showwarning("Ошибка", "Сначала подключите Arduino!")
        return

    try:
        url = "https://karatunov.net/readme.php?name=mih&pass=anna22"
        response = requests.get(url, timeout=5)
        response.raise_for_status()
        goal = int(response.text.strip())

        info_label.config(text=f"📋 Нужно сделать {goal} отжиманий")
        ser.write(f"{goal}\n".encode())  # отправляем в Arduino
        messagebox.showinfo("Задача получена", f"Твоя цель: {goal} отжиманий")
    except Exception as e:
        messagebox.showerror("Ошибка", f"Не удалось получить данные: {e}")


def on_close():
    """Закрытие приложения"""
    global stop_thread
    stop_thread = True
    disconnect_arduino()
    window.destroy()


# --- GUI интерфейс ---
window = tk.Tk()
window.title("Счётчик отжиманий 💪")
window.geometry("400x280")
window.resizable(False, False)

title_label = tk.Label(window, text="Счётчик отжиманий", font=("Arial", 18, "bold"))
title_label.pack(pady=10)

info_label = tk.Label(window, text="💤 Ожидание Arduino...", font=("Arial", 14))
info_label.pack(pady=20)

status_label = tk.Label(window, text="Не подключено", fg="gray", font=("Arial", 12))
status_label.pack(pady=10)

button_frame = tk.Frame(window)
button_frame.pack(pady=20)

connect_button = tk.Button(button_frame, text="🔌 Подключить Arduino", command=connect_arduino, width=20)
connect_button.grid(row=0, column=0, padx=5)

disconnect_button = tk.Button(button_frame, text="❎ Отключить", command=disconnect_arduino, width=20)
disconnect_button.grid(row=0, column=1, padx=5)

goal_button = tk.Button(window, text="🌐 Получить отжимания с сайта", command=fetch_pushup_goal, width=30)
goal_button.pack(pady=5)

exit_button = tk.Button(window, text="🚪 Выйти", command=on_close, width=20)
exit_button.pack(pady=10)

window.protocol("WM_DELETE_WINDOW", on_close)

# --- Запуск ---
window.mainloop()

