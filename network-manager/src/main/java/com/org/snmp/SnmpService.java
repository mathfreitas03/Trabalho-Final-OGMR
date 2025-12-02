// IMPORTANTE: ESSA CLASSE NÃO É MAIS USADA

package com.org.snmp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

public class SnmpService {

    private final SwitchInfo sw;
    private final int timeout = 1500;
    private final int retries = 2;

    public SnmpService(SwitchInfo sw) {
        this.sw = sw;
    }

    // SNMP SESSION
    
    private Snmp createSession() throws Exception {

        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        transport.listen();

        return new Snmp(transport);
    }

    private CommunityTarget<Address> buildTarget() {
        Address targetAddress = GenericAddress.parse("udp:" + sw.ip + "/161");

        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(sw.community));
        target.setAddress(targetAddress);
        target.setTimeout(timeout);
        target.setRetries(retries);

        if ("v1".equalsIgnoreCase(sw.snmpVersion)) {
            target.setVersion(SnmpConstants.version1);
        } else {
            target.setVersion(SnmpConstants.version2c);
        }

        return target;
    }

    // SNMP WALK

    public java.util.Map<String, String> walk(String oidBase) throws Exception {

        Snmp snmp = createSession();
        CommunityTarget<Address> target = buildTarget();

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidBase)));
        pdu.setType(PDU.GETNEXT);

        java.util.Map<String, String> result = new java.util.HashMap<>();

        boolean finished = false;

        while (!finished) {
            ResponseEvent<?> response = snmp.send(pdu, target);
            PDU resp = response.getResponse();

            if (resp == null) break;

            VariableBinding vb = resp.get(0);

            if (!vb.getOid().startsWith(new OID(oidBase))) {
                finished = true;
            } else {
                result.put(vb.getOid().toDottedString(), vb.getVariable().toString());
                pdu.setRequestID(new Integer32(0));
                pdu.set(0, vb);
            }
        }

        snmp.close();
        return result;
    }

    // SNMP GET

    public String get(String oid) throws Exception {

        Snmp snmp = createSession();
        CommunityTarget<Address> target = buildTarget();

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));
        pdu.setType(PDU.GET);

        ResponseEvent<?> response = snmp.send(pdu, target);
        snmp.close();

        if (response.getResponse() == null) {
            return null;
        }

        return response.getResponse().get(0).getVariable().toString();
    }

    public SnmpSetResult setPortState(int ifIndex, boolean enable) throws Exception {

        int value = enable ? 1 : 2;  // 1=up, 2=down
        String oid = "1.3.6.1.2.1.2.2.1.7." + ifIndex;

        Snmp snmp = createSession();
        CommunityTarget<Address> target = buildTarget();

        PDU pdu = new PDU();
        pdu.setType(PDU.SET);
        pdu.add(new VariableBinding(new OID(oid), new Integer32(value)));

        ResponseEvent<?> response = snmp.send(pdu, target);
        snmp.close();

        if (response == null || response.getResponse() == null) {
            return new SnmpSetResult(false, ifIndex, value, null, -1, "No response");
        }

        PDU resp = response.getResponse();

        boolean success = resp.getErrorStatus() == PDU.noError;

        return new SnmpSetResult(
                success,
                ifIndex,
                value,
                resp.toString(),
                resp.getErrorStatus(),
                resp.getErrorStatusText()
        );
    }
    public PortStatus getPortStatus(int ifIndex) throws Exception {

        String oidAdmin = "1.3.6.1.2.1.2.2.1.7." + ifIndex;
        String oidOper  = "1.3.6.1.2.1.2.2.1.8." + ifIndex;

        String admin = get(oidAdmin);
        String oper  = get(oidOper);

        return new PortStatus(
                ifIndex,
                admin != null ? Integer.parseInt(admin) : 0,
                oper  != null ? Integer.parseInt(oper)  : 0
        );
    }

    private void updateHostBlocked(int hostId, boolean blocked) throws SQLException {
        try (Connection conn = PostgresPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "update port set is_blocked = ? where id = ?")) {

            ps.setBoolean(1, blocked);
            ps.setInt(2, hostId);
            ps.executeUpdate();
        }
    }

    // RECORDS

    public record SwitchInfo(String ip, String snmpVersion, String community) {}
    public record Host(int id, Integer portIfIndex) {}
    public record PortStatus(int ifIndex, int ifAdminStatus, int ifOperStatus) {}
    public record SnmpSetResult(
            boolean success,
            int ifIndex,
            int value,
            String raw,
            int errno,
            String error
    ) {}
}
