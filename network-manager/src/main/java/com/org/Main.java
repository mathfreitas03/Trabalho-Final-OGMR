package com.org;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
// import org.snmp4j.Target;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
// import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
// import org.snmp4j.transport.UdpTransportMapping;

import com.org.db.PostgresPool;
import com.org.web.HttpServer;

public class Main {
    public static void main(String[] args) {
        try {
            HttpServer server = new HttpServer(8080);
            server.start();

            TransportMapping transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget target = new CommunityTarget();
            // TODO: Usar comunidade "private" para sets
            target.setCommunity(new OctetString("public"));
            // TODO: Configurar endereço para 10.90.90.90 - Não esquecer de setar o IP do servidor e do cliente manualmente
            target.setAddress(new UdpAddress("127.0.0.1/161"));
            // TODO: Versão do SWITCH é 1
            target.setVersion(SnmpConstants.version2c);
            target.setRetries(2);
            target.setTimeout(1500);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.3.0")));
            pdu.setType(PDU.GET);

            ResponseEvent response = snmp.send(pdu, target);

            if(response != null){
                System.out.println(response.getResponse().get(0).getVariable().toString());
            } else {
                System.out.println("Erro ao obter response");
            }
            snmp.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
