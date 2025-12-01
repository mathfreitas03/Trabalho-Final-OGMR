package com.org.snmp;

import java.sql.*;
import java.util.*;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.org.db.PostgresPool;

public class PortManager {

    private static final String SNMP_COMMUNITY = "private";
    private static final int SNMP_PORT = 161;

    // ifAdminStatus OID base
    private static final OID IF_ADMIN_STATUS = new OID("1.3.6.1.2.1.2.2.1.7");

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Uso: java PortManager <block|unblock> <portIds separados por vírgula>");
            return;
        }

        String action = args[0];
        boolean enable = action.equalsIgnoreCase("unblock");

        List<Integer> portIds = parsePortIds(args[1]);

        try {
            performAction(portIds, enable);
            System.out.println("Operação SNMP + DB executada com sucesso.");
        }
        catch (Exception ex) {
            System.err.println("Falha no PortManager:");
            ex.printStackTrace();
        }
    }

    private static List<Integer> parsePortIds(String input) {
        String[] parts = input.split(",");
        List<Integer> ids = new ArrayList<>();
        for (String p : parts)
            ids.add(Integer.parseInt(p.trim()));
        return ids;
    }

    private static void performAction(List<Integer> portIds, boolean enable) throws Exception {

        // 1) Busca os dados necessários no banco
        List<PortSNMPInfo> ports = loadPortsSNMPInfo(portIds);

        if (ports.isEmpty())
            throw new RuntimeException("Nenhuma porta encontrada no banco.");

        // 2) Envia SNMP SET para cada porta
        for (PortSNMPInfo p : ports) {

            boolean ok = sendSnmpSet(
                    p.switchIpv4,
                    p.physicalPortNumber,
                    enable
            );

            if (!ok)
                throw new RuntimeException(
                        "Falha SNMP SET: portaId=" + p.portId +
                        " | switch=" + p.switchIpv4 +
                        " | porta física=" + p.physicalPortNumber);
        }

        // 3) Atualiza o banco SOMENTE se todo SNMP deu certo
        updatePortsStatus(portIds, enable);
    }

    /**
     * Carrega:
     *   port.id
     *   port.number
     *   switch.ipv4
     */
    private static List<PortSNMPInfo> loadPortsSNMPInfo(List<Integer> portIds)
            throws SQLException {

        String placeholders =
            String.join(",", Collections.nCopies(portIds.size(), "?"));

        String sql = """
            SELECT
                p.id       AS port_id,
                p.number   AS port_number,
                s.ipv4     AS switch_ipv4
            FROM port p
            JOIN switch s ON s.id = p.switch_id
            WHERE p.id IN (%s)
        """.formatted(placeholders);

        List<PortSNMPInfo> list = new ArrayList<>();

        try (Connection conn = PostgresPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            for (Integer id : portIds)
                ps.setInt(i++, id);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(
                    new PortSNMPInfo(
                        rs.getInt("port_id"),
                        rs.getString("switch_ipv4"),
                        rs.getInt("port_number")
                    )
                );
            }
        }

        return list;
    }

    /**
     * SNMP SET ifAdminStatus
     */
    private static boolean sendSnmpSet(
            String switchIpv4,
            int physicalPortNumber,
            boolean enable) {

        try {
            Address targetAddress =
                GenericAddress.parse("udp:" + switchIpv4 + "/" + SNMP_PORT);

            TransportMapping<UdpAddress> transport =
                new DefaultUdpTransportMapping();
            transport.listen();

            Snmp snmp = new Snmp(transport);

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(SNMP_COMMUNITY));
            target.setAddress((UdpAddress) targetAddress);
            target.setVersion(SnmpConstants.version2c);
            target.setTimeout(4000);
            target.setRetries(2);

            int adminStatus = enable ? 1 : 2;   // 1 = up | 2 = down

            PDU pdu = new PDU();
            pdu.setType(PDU.SET);

            pdu.add(new VariableBinding(
                    new OID(IF_ADMIN_STATUS + "." + physicalPortNumber),
                    new Integer32(adminStatus)
            ));

            ResponseEvent<?> response = snmp.send(pdu, target);

            snmp.close();

            if (response == null || response.getResponse() == null)
                return false;

            return response.getResponse().getErrorStatus() == PDU.noError;

        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Atualiza STATUS no banco
     */
    private static void updatePortsStatus(List<Integer> portIds, boolean status)
            throws SQLException {

        try (Connection conn = PostgresPool.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE port SET status = ? WHERE id = ?")) {

                for (Integer id : portIds) {
                    ps.setBoolean(1, status);
                    ps.setInt(2, id);
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
            }
            catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * DTO interno
     */
    private record PortSNMPInfo(
            int portId,
            String switchIpv4,
            int physicalPortNumber
    ) {}
}
