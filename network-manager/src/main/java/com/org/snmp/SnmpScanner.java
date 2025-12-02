package com.org.snmp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.org.db.PostgresPool;

public class SnmpScanner {

    public void scanAllSwitches() throws Exception {
        List<SwitchRecord> switches = loadSwitches();

        for (SwitchRecord sw : switches) {
            scanSwitch(sw);
        }
    }

    private List<SwitchRecord> loadSwitches() throws SQLException {

        String sql = "select id, hostname, ipv4 FROM switch";

        List<SwitchRecord> list = new ArrayList<>();

        try (Connection conn = PostgresPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new SwitchRecord(
                        rs.getInt("id"),
                        rs.getString("hostname"),
                        rs.getString("ipv4")
                ));
            }
        }
        return list;
    }

    private void scanSwitch(SwitchRecord sw) throws Exception {

        SnmpService service = new SnmpService(
                new SnmpService.SwitchInfo(sw.ipv4, "v1", "private")
        );

        Map<String, String> ifIndexMap = service.walk("1.3.6.1.2.1.2.2.1.1");
        Map<String, String> descrMap   = service.walk("1.3.6.1.2.1.2.2.1.2");
        Map<String, String> adminMap   = service.walk("1.3.6.1.2.1.2.2.1.7");
        Map<String, String> operMap    = service.walk("1.3.6.1.2.1.2.2.1.8");

        for (String oid : ifIndexMap.keySet()) {

            String suffix = oid.substring(oid.lastIndexOf('.') + 1);
            int ifIndex = Integer.parseInt(ifIndexMap.get(oid));

            String descr = descrMap.get("1.3.6.1.2.1.2.2.1.2." + suffix);
            if (descr == null) descr = "unknown";

            // portas físicas sempre aparecem como "Ethernet Interface"
            if (!descr.equalsIgnoreCase("Ethernet Interface")) {
                continue;
            }

            int admin = parseSafe(adminMap.get("1.3.6.1.2.1.2.2.1.7." + suffix));
            int oper  = parseSafe(operMap.get("1.3.6.1.2.1.2.2.1.8." + suffix));

            upsertPort(sw.id, ifIndex, descr, admin, oper);
        }
    }

    private int parseSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return -1;
        }
    }

    private void upsertPort(int switchId, int ifIndex, String descr, int admin, int oper) throws SQLException {

        // admin=1 significa enabled
        boolean isBlocked = admin != 1;

        try (Connection conn = PostgresPool.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                insert into port (switch_id, ifindex, hostname, number, is_blocked)
                values (?, ?, ?, ?, ?)
                on conflict (switch_id, number)
                do update set
                    ifindex = excluded.ifindex,
                    hostname = excluded.hostname,
                    is_blocked = excluded.is_blocked
            """)) {

            ps.setInt(1, switchId);
            ps.setInt(2, ifIndex);
            ps.setString(3, descr);
            ps.setInt(4, ifIndex);  // número da porta = próprio ifIndex
            ps.setBoolean(5, isBlocked);

            ps.executeUpdate();
        }
    }

    public record SwitchRecord(int id, String hostname, String ipv4) {}
}
