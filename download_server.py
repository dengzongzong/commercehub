#!/usr/bin/env python3
"""简易文件下载服务，仅提供 commercehub.zip 下载"""
import http.server
import os
import socketserver

PORT = 8000
FILE = "/workspace/commercehub.zip"

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", "/commercehub.zip", "/download"):
            try:
                size = os.path.getsize(FILE)
                self.send_response(200)
                self.send_header("Content-Type", "application/zip")
                self.send_header("Content-Disposition", 'attachment; filename="commercehub.zip"')
                self.send_header("Content-Length", str(size))
                self.end_headers()
                with open(FILE, "rb") as f:
                    while True:
                        chunk = f.read(65536)
                        if not chunk:
                            break
                        self.wfile.write(chunk)
                print(f"[OK] served download, size={size}")
            except Exception as e:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(str(e).encode())
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found. Visit /commercehub.zip")

    def log_message(self, fmt, *args):
        print("[req] " + (fmt % args))

with socketserver.TCPServer(("0.0.0.0", PORT), Handler) as httpd:
    print(f"Download server on :{PORT}")
    httpd.serve_forever()
