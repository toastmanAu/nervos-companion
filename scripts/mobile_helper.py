#!/usr/bin/env python3
import sys
import subprocess
import os

APP_PACKAGE = "com.example.nervoscompanion"
DEFAULT_DEVICE = "R5CTC07MPYD"

def run_cmd(cmd):
    try:
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, check=True)
        return res.stdout.strip(), None
    except subprocess.CalledProcessError as e:
        return "", f"Error running {' '.join(cmd)}: {e.stderr.strip()}"

def get_adb_prefix(device_id):
    if device_id:
        return ["adb", "-s", device_id]
    return ["adb"]

def take_screenshot(device_id, output_path):
    adb = get_adb_prefix(device_id)
    print(f"Capturing screenshot from device {device_id or 'default'}...")
    
    # Run screencap and pipe output directly to local file
    cmd = adb + ["exec-out", "screencap", "-p"]
    try:
        with open(output_path, "wb") as f:
            subprocess.run(cmd, stdout=f, check=True)
        print(f"Screenshot successfully saved to: {output_path}")
        return True
    except Exception as e:
        print(f"Failed to capture screenshot: {e}", file=sys.stderr)
        return False

def show_logcat(device_id, lines_count=100):
    adb = get_adb_prefix(device_id)
    
    # Find App PID
    pid_cmd = adb + ["shell", "pidof", "-s", APP_PACKAGE]
    pid, err = run_cmd(pid_cmd)
    
    if err or not pid:
        print(f"App {APP_PACKAGE} is not currently running. Showing general logs filtered by package name...")
        log_cmd = adb + ["logcat", "-d", "-t", str(lines_count), f"*:V"]
    else:
        print(f"App {APP_PACKAGE} is running with PID {pid}. Fetching filtered logcat...")
        log_cmd = adb + ["logcat", "-d", "-t", str(lines_count), f"--pid={pid}"]
        
    log_output, log_err = run_cmd(log_cmd)
    if log_err:
        print(f"Failed to fetch logs: {log_err}", file=sys.stderr)
    else:
        print("--- LOGCAT START ---")
        print(log_output)
        print("--- LOGCAT END ---")

def launch_app(device_id):
    adb = get_adb_prefix(device_id)
    cmd = adb + ["shell", "am", "start", "-n", f"{APP_PACKAGE}/.MainActivity"]
    out, err = run_cmd(cmd)
    if err:
        print(f"Failed to launch app: {err}", file=sys.stderr)
    else:
        print("App launched successfully:", out)

def main():
    if len(sys.argv) < 2:
        print("Usage: mobile_helper.py <command> [args]")
        print("Commands:")
        print("  screenshot <output_path> [device_id]")
        print("  logcat [lines_count] [device_id]")
        print("  launch [device_id]")
        sys.exit(1)
        
    cmd = sys.argv[1]
    
    if cmd == "screenshot":
        if len(sys.argv) < 3:
            print("Error: screenshot command requires an output_path.")
            sys.exit(1)
        output_path = sys.argv[2]
        device_id = sys.argv[3] if len(sys.argv) > 3 else DEFAULT_DEVICE
        take_screenshot(device_id, output_path)
        
    elif cmd == "logcat":
        lines_count = int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 100
        device_id = sys.argv[3] if len(sys.argv) > 3 else DEFAULT_DEVICE
        show_logcat(device_id, lines_count)
        
    elif cmd == "launch":
        device_id = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_DEVICE
        launch_app(device_id)
        
    else:
        print(f"Unknown command: {cmd}")
        sys.exit(1)

if __name__ == "__main__":
    main()
