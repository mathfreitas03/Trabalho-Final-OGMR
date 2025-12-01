package com.org.snmp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.org.db.PostgresPool;

public class SnmpTrapListener {
    private Snmp snmp;

    public SnmpTrapListener(int trapPort) throws IOException {
        snmp = new Snmp(new DefaultUdpTransportMapping(new UdpAddress(trapPort)));
        snmp.addCommandResponder(this::processTrap);
        snmp.listen();
    }

    private void processTrap(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        if (pdu == null) return;

        System.out.println("Trap recebido: " + pdu);

        String switchIp = null;
        Integer portNumber = null;
        String eventType = null;

        // Supondo que o trap contenha VarBinds: "switchIp", "portNumber", "eventType"
        try {
            switchIp = pdu.getVariableBindings().stream()
                            .filter(vb -> vb.getOid().toString().endsWith("1.1")) // substituir pelo OID correto
                            .map(vb -> vb.getVariable().toString())
                            .findFirst().orElse(null);

            portNumber = pdu.getVariableBindings().stream()
                            .filter(vb -> vb.getOid().toString().endsWith("1.2"))
                            .map(vb -> Integer.parseInt(vb.getVariable().toString()))
                            .findFirst().orElse(null);

            eventType = pdu.getVariableBindings().stream()
                            .filter(vb -> vb.getOid().toString().endsWith("1.3"))
                            .map(vb -> vb.getVariable().toString())
                            .findFirst().orElse(null);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (switchIp == null || portNumber == null || eventType == null) {
            System.out.println("Trap incompleto, ignorando.");
            return;
        }

        try {
            PostgresPool.init("localhost", 5432, "meubanco", "usuario", "senha");

            try (Connection conn = PostgresPool.getConnection()) {
                conn.setAutoCommit(false);

                // 1. Identifica o switch
                int switchId = -1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM switch WHERE ipv4 = ?")) {
                    ps.setString(1, switchIp);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            switchId = rs.getInt("id");
                        } else {
                            System.out.println("Switch não cadastrado: " + switchIp);
                            conn.rollback();
                            return;
                        }
                    }
                }

                // 2. Identifica a porta
                int portId = -1;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM port WHERE switch_id = ? AND number = ?")) {
                    ps.setInt(1, switchId);
                    ps.setInt(2, portNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            portId = rs.getInt("id");
                        } else {
                            System.out.println("Porta não cadastrada: " + portNumber);
                            conn.rollback();
                            return;
                        }
                    }
                }

                // 3. Atualiza status da porta
                boolean isUp = eventType.equalsIgnoreCase("link_up");
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE port SET status = ? WHERE id = ?")) {
                    ps.setBoolean(1, isUp);
                    ps.setInt(2, portId);
                    ps.executeUpdate();
                }

                // 4. Insere trap_event
                try (PreparedStatement ps = conn.prepareStatement(
                        "insert into trap_event(switch_id, port_id, event_type, raw_data) values (?,?,?,?)")) {
                    ps.setInt(1, switchId);
                    ps.setInt(2, portId);
                    ps.setString(3, eventType);
                    ps.setString(4, pdu.toString());
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
