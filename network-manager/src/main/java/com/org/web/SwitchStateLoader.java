package com.org.web;

import java.sql.*;

import org.json.JSONArray;
import org.json.JSONObject;

import com.org.db.PostgresPool;

public class SwitchStateLoader {

    /**
     * Carrega o estado atual de todos os switches e portas
     * @return JSONArray com os switches e suas portas
     */
    public static JSONArray loadCurrentState() {
        JSONArray result = new JSONArray();

        String switchQuery = "SELECT id, hostname, ipv4 FROM switch";
        String portQuery = "SELECT id, number, ipv4, mac, status, lockable FROM port WHERE switch_id = ?";

        try (Connection conn = PostgresPool.getConnection();
             PreparedStatement switchStmt = conn.prepareStatement(switchQuery);
             ResultSet switchRs = switchStmt.executeQuery()) {

            while (switchRs.next()) {
                int switchId = switchRs.getInt("id");
                String hostname = switchRs.getString("hostname");
                String switchIp = switchRs.getString("ipv4");

                JSONObject swJson = new JSONObject();
                swJson.put("id", switchId);
                swJson.put("hostname", hostname);
                swJson.put("ipv4", switchIp);

                try (PreparedStatement portStmt = conn.prepareStatement(portQuery)) {
                    portStmt.setInt(1, switchId);
                    try (ResultSet portRs = portStmt.executeQuery()) {
                        JSONArray portsJson = new JSONArray();
                        while (portRs.next()) {
                            JSONObject portJson = new JSONObject();
                            portJson.put("id", portRs.getInt("id"));
                            portJson.put("number", portRs.getInt("number"));
                            portJson.put("ipv4", portRs.getString("ipv4"));
                            portJson.put("mac", portRs.getString("mac"));
                            portJson.put("status", portRs.getBoolean("status"));
                            portJson.put("lockable", portRs.getBoolean("lockable"));
                            portsJson.put(portJson);
                        }
                        swJson.put("ports", portsJson);
                    }
                }

                result.put(swJson);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
