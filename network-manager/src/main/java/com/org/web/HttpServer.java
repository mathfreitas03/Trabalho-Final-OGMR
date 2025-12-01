package com.org.web;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class HttpServer {

    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Servidor HTTP rodando na porta " + port);

            var pool = Executors.newCachedThreadPool();

            while (true) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handleClient(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
            );
            BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)
            )
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isBlank()) return;

            System.out.println("Recebido: " + requestLine);

            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = parts[1];

            String line;
            int contentLength = 0;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            String body = null;
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                in.read(buf);
                body = new String(buf);
            }

            if ("POST".equals(method) && "/block".equals(path)) {

                JSONObject json = new JSONObject(body);

                List<Integer> ids =
                        json.getJSONArray("ids").toList().stream()
                        .map(o -> (Integer) o)
                        .toList();

                int durationSeconds = json.getInt("durationSeconds");

                String portsArg = ids.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                ProcessBuilder pb = new ProcessBuilder(
                        "/bin/bash",
                        "/caminho/do/script/schedule_ports.sh",
                        "block", portsArg,
                        String.valueOf(durationSeconds)
                );

                pb.inheritIO();

                pb.start().waitFor();

                sendJsonResponse(out, 200, "{\"status\":\"ok\"}");
                return;
            }

            if ("POST".equals(method) && "/unblock".equals(path)) {

                JSONObject json = new JSONObject(body);

                List<Integer> ids =
                        json.getJSONArray("ids").toList().stream()
                        .map(o -> (Integer) o)
                        .toList();

                String portsArg = ids.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                ProcessBuilder pb = new ProcessBuilder(
                        "/bin/bash",
                        "/caminho/do/script/schedule_ports.sh",
                        "unblock", portsArg
                );

                pb.inheritIO();

                pb.start().waitFor();

                sendJsonResponse(out, 200, "{\"status\":\"ok\"}");
                return;
            }

            if ("GET".equals(method) && "/state".equals(path)) {

                JSONArray rawState = SwitchStateLoader.loadCurrentState();
                JSONArray frontendArray = new JSONArray();

                for (int i = 0; i < rawState.length(); i++) {

                    JSONObject sw = rawState.getJSONObject(i);

                    JSONObject switchJson = new JSONObject();
                    switchJson.put("id", sw.getInt("id"));
                    switchJson.put("hostname", sw.getString("hostname"));

                    JSONArray portsArray = new JSONArray();

                    JSONArray ports = sw.getJSONArray("ports");

                    for (int j = 0; j < ports.length(); j++) {

                        JSONObject p = ports.getJSONObject(j);

                        JSONObject portJson = new JSONObject();

                        portJson.put("id", p.getInt("id"));
                        portJson.put("number", p.getInt("number"));
                        portJson.put("mac", p.optString("mac", ""));
                        portJson.put("ipv4", p.optString("ipv4", ""));
                        portJson.put("status", p.getBoolean("status"));
                        portJson.put("lockable", p.getBoolean("lockable"));

                        portsArray.put(portJson);
                    }

                    switchJson.put("ports", portsArray);

                    frontendArray.put(switchJson);
                }

                sendJsonResponse(out, 200, frontendArray.toString());
                return;
            }

            sendJsonResponse(out, 404, "{\"error\":\"Not found\"}");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendJsonResponse(BufferedWriter out, int statusCode, String json)
            throws IOException {

        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        String statusText = switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };

        String header =
                "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: " + data.length + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(header);
        out.write(json);
        out.flush();
    }

    public static void main(String[] args) {
        new HttpServer(8080).start();
    }
}
