#!/usr/bin/env python3
import http.server
import socketserver
import urllib.request
import urllib.parse
import json

class TunnelHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/api/webhook/whatsapp':
            # Request body'yi oku
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            
            print(f"Webhook alındı: {post_data.decode('utf-8')}")
            
            # Local backend'e forward et
            try:
                req = urllib.request.Request(
                    'http://localhost:8080/api/webhook/whatsapp',
                    data=post_data,
                    headers={'Content-Type': 'application/json'}
                )
                response = urllib.request.urlopen(req)
                response_data = response.read()
                
                # Response'u döndür
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(response_data)
                print("Webhook başarıyla forward edildi")
                
            except Exception as e:
                print(f"Hata: {e}")
                self.send_response(500)
                self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()
    
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-Type', 'text/html')
        self.end_headers()
        self.wfile.write(b'<h1>Tunnel Calisiyor</h1>')

if __name__ == "__main__":
    PORT = 3000
    with socketserver.TCPServer(("", PORT), TunnelHandler) as httpd:
        print(f"Tunnel http://localhost:{PORT} adresinde calisiyor")
        print("Ngrok ile bu portu ac: ngrok http 3000")
        httpd.serve_forever()
