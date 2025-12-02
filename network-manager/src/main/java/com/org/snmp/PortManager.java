package com.org.snmp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.org.db.PostgresPool;

public class PortManager {

    private static final OID IF_ADMIN_STATUS = new OID("1.3.6.1.2.1.2.2.1.7");

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Uso: java PortManager <block|unblock> <portIds separados por vírgula>");
            return;
        }

        boolean enable = args[0].equalsIgnoreCase("unblock");

        List<Integer> portIds = Arrays.stream(args[1].split(","))
                .map(s -> Integer.parseInt(s.trim()))
                .toList();

        try {
            performAction(portIds, enable);
            System.out.println("Bloqueios executados com sucesso.");
        } catch (Exception ex) {
            System.err.println("Erro no PortManager:");
            ex.printStackTrace();
        }
    }

    //  EXECUÇÃO

    private static void performAction(List<Integer> portIds, boolean enable) throws Exception {

        List<PortSNMPInfo> ports = loadPortsSNMPInfo(portIds);

        if (ports.isEmpty())
            throw new RuntimeException("Nenhuma porta encontrada no banco.");

        // Agrupar portas por switch (cada switch = 1 sessão SNMP)
        Map<Integer, List<PortSNMPInfo>> grouped = groupBySwitch(ports);

        for (int switchId : grouped.keySet()) {

            List<PortSNMPInfo> plist = grouped.get(switchId);

            try (
                TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            ) {
                transport.listen();
                Snmp snmp = new Snmp(transport);

                CommunityTarget<Address> target = buildTarget(plist.get(0));

                for (PortSNMPInfo p : plist) {
                    boolean ok = sendSnmpSet(snmp, target, p.ifIndex(), enable);

                    if (!ok) {
                        throw new RuntimeException("Falha SNMP SET na porta " + p.portId());
                    }
                }

                snmp.close();
            }
        }

        updatePortsStatus(portIds, enable);
    }

    //  CONSULTA DB

    private static List<PortSNMPInfo> loadPortsSNMPInfo(List<Integer> portIds) throws SQLException {

        String placeholders = String.join(",", Collections.nCopies(portIds.size(), "?"));

        String sql = """
            SELECT 
                p.id,
                p.ifindex,
                s.id AS switch_id,
                s.ipv4,
                s.hostname,
                s.community
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
                list.add(new PortSNMPInfo(
                        rs.getInt("id"),
                        rs.getInt("ifindex"),
                        rs.getInt("switch_id"),
                        rs.getString("ipv4"),
                        rs.getString("community")
                ));
            }
        }

        return list;
    }

    // Agrupar portas por switch
    private static Map<Integer, List<PortSNMPInfo>> groupBySwitch(List<PortSNMPInfo> ports) {
        Map<Integer, List<PortSNMPInfo>> map = new HashMap<>();

        for (PortSNMPInfo p : ports) {
            map.computeIfAbsent(p.switchId(), k -> new ArrayList<>()).add(p);
        }

        return map;
    }


    //  SNMP

    private static CommunityTarget<Address> buildTarget(PortSNMPInfo p) {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setAddress(GenericAddress.parse("udp:" + p.switchIp() + "/161"));
        target.setCommunity(new OctetString(p.community()));
        target.setVersion(SnmpConstants.version2c);
        target.setTimeout(1500);
        target.setRetries(1);
        return target;
    }

    private static boolean sendSnmpSet(Snmp snmp, CommunityTarget<Address> target, int ifIndex, boolean enable) {
        try {
            int adminStatus = enable ? 1 : 2;

            PDU pdu = new PDU();
            pdu.setType(PDU.SET);
            pdu.add(new VariableBinding(
                    new OID(IF_ADMIN_STATUS + "." + ifIndex),
                    new Integer32(adminStatus)
            ));

            ResponseEvent<?> response = snmp.send(pdu, target);

            return response.getResponse() != null &&
                   response.getResponse().getErrorStatus() == PDU.noError;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //  UPDATE DB

    private static void updatePortsStatus(List<Integer> portIds, boolean enable) throws SQLException {

        try (Connection conn = PostgresPool.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "update port set is_blocked = ? where id = ?"
            );

            for (int id : portIds) {
                ps.setBoolean(1, !enable);
                ps.setInt(2, id);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private record PortSNMPInfo(
            int portId,
            int ifIndex,
            int switchId,
            String switchIp,
            String community
    ) {}
}
