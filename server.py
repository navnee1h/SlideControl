#!/usr/bin/env python3
"""
SlideControl Server - Linux side
Run this on your laptop before starting the presentation.
"""

from flask import Flask, jsonify
import subprocess
import os

app = Flask(__name__)

def press_key(key):
    """Simulate a key press using xdotool"""
    display = os.environ.get('DISPLAY', ':0')
    env = os.environ.copy()
    env['DISPLAY'] = display
    result = subprocess.run(
        ['xdotool', 'key', key],
        env=env,
        capture_output=True,
        text=True
    )
    return result.returncode == 0

@app.route('/next')
def next_slide():
    """Volume UP -> Next slide (Right arrow)"""
    success = press_key('Right')
    return jsonify({'status': 'ok' if success else 'error', 'action': 'next'})

@app.route('/prev')
def prev_slide():
    """Volume DOWN -> Previous slide (Left arrow)"""
    success = press_key('Left')
    return jsonify({'status': 'ok' if success else 'error', 'action': 'prev'})

@app.route('/ping')
def ping():
    """Test connection from phone"""
    return jsonify({'status': 'ok', 'message': 'SlideControl server is running!'})

@app.route('/')
def index():
    return """
    <h2>SlideControl Server Running ✅</h2>
    <p>Endpoints:</p>
    <ul>
        <li><a href="/next">/next</a> - Next slide</li>
        <li><a href="/prev">/prev</a> - Previous slide</li>
        <li><a href="/ping">/ping</a> - Test connection</li>
    </ul>
    """

if __name__ == '__main__':
    import socket
    # Auto-detect local IP
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        local_ip = s.getsockname()[0]
    except:
        local_ip = '127.0.0.1'
    finally:
        s.close()

    print("=" * 40)
    print("  SlideControl Server")
    print("=" * 40)
    print(f"  Enter this IP in your Android app:")
    print(f"  👉  {local_ip}")
    print(f"  Port: 5000")
    print("=" * 40)
    print("  Volume UP   → Next slide  (→)")
    print("  Volume DOWN → Prev slide  (←)")
    print("=" * 40)

    app.run(host='0.0.0.0', port=5000, debug=False)
