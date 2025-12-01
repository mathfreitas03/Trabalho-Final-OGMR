package com.org.web;

import java.io.*;
import java.net.*;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.org.snmp.CronManager;

public class HttpServer {

    private int port;
    private boolean running = false;

    public HttpServer(int port) {
        this.port = port;
    }
    
    // Inicia o servidor e bloqueia a thread atual
    public void start() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor HTTP rodando na porta " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Permite parar o servidor (fora do try-with-resources, se necessário)
    public void stop() {
        running = false;
        System.out.println("Servidor parado.");
    }

    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            System.out.println("Recebido: " + requestLine);

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            String line;
            int contentLength = 0;
            while (!(line = in.readLine()).isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(" ")[1]);
                }
            }

            String body = null;
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                in.read(buf, 0, contentLength);
                body = new String(buf);
                System.out.println("Corpo recebido: " + body);
            }

            // POST /block
            if ("POST".equals(method) && "/block".equals(path)) {
                JSONObject json = new JSONObject(body);
                List<Integer> ids = json.getJSONArray("ids").toList().stream()
                                        .map(o -> (Integer) o)
                                        .toList();
                int durationSeconds = json.getInt("durationSeconds");

                CronManager.scheduleBlock(ids, durationSeconds);
                sendJsonResponse(out, 200, "{\"status\":\"ok\"}");

            } else if ("POST".equals(method) && "/unblock".equals(path)) {
                JSONObject json = new JSONObject(body);
                List<Integer> ids = json.getJSONArray("ids").toList().stream()
                                        .map(o -> (Integer) o)
                                        .toList();

                CronManager.unblockNow(ids);
                sendJsonResponse(out, 200, "{\"status\":\"ok\"}");
            } else if ("GET".equals(method) && "/state".equals(path)) {
                JSONArray rawState = SwitchStateLoader.loadCurrentState();
                JSONArray frontendArray = new JSONArray();

                for (int i = 0; i < rawState.length(); i++) {
                    JSONObject sw = rawState.getJSONObject(i);
                    int switchId = sw.getInt("id");
                    String hostname = sw.getString("hostname");
                    JSONArray ports = sw.getJSONArray("ports");

                    for (int j = 0; j < ports.length(); j++) {
                        JSONObject p = ports.getJSONObject(j);
                        JSONObject portJson = new JSONObject();
                        portJson.put("id", p.getInt("id"));
                        portJson.put("switch_id", switchId);
                        portJson.put("hostname", hostname);
                        portJson.put("porta", p.getInt("number"));
                        portJson.put("ip", p.optString("ipv4", ""));
                        portJson.put("status", p.getBoolean("status"));
                        portJson.put("lockable", p.getBoolean("lockable"));

                        frontendArray.put(portJson);
                    }
                }

                sendJsonResponse(out, 200, frontendArray.toString());
            }
            else{
                sendJsonResponse(out, 404, "{\"error\":\"Not found\"}");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void sendJsonResponse(BufferedWriter out, int statusCode, String json) throws IOException {
        String httpResponse =
            "HTTP/1.1 " + statusCode + " OK\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n" +
            "Content-Length: " + json.getBytes().length + "\r\n" +
            "\r\n" +
            json;

        out.write(httpResponse);
        out.flush();
    }

    // Main de exemplo
    // public static void main(String[] args) {
    //     HttpServer server = new HttpServer(8080);
    //     server.start();
    // }
}
