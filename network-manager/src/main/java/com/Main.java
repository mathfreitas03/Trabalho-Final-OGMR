package com;


import org.snmp4j.CommunityTarget;
import org.smnp4j.PDU;
import org.smnp4.Target;
import org.smnp4.Smnp;
import org.smnp4.TransportMapping;
import org.smnp4.event.ResponseEvent;
import org.smnp4.smi.GenericAddress;
import org.smnp4.smi.OID;
import org.smnp4.smi.OctetString;
import org.smnp4.smi.UdpAddress;
import org.smnp4.smi.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.UdpTransportMapping;

public class Main {
    public static void main(String[] args) {
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            Smnp smnp = new Smnp();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
