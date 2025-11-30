package snmp;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import db.PortDAO;
import db.SwitchDAO;

public class TrapReceiver implements CommandResponder {

    private final SwitchDAO switchDAO;
    private final PortDAO portDAO;

    public TrapReceiver(SwitchDAO switchDAO, PortDAO portDAO) {
        this.switchDAO = switchDAO;
        this.portDAO = portDAO;
    }

    public void start() throws Exception {
        TransportMapping<?> transport = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/162"));
        Snmp snmp = new Snmp(transport);

        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
        SecurityProtocols.getInstance().addDefaultProtocols();

        snmp.addCommandResponder(this);

        transport.listen();
        System.out.println("Trap Listener iniciado na porta 162...");
    }

    @Override
    public void processPdu(CommandResponderEvent event) {
        try {
            System.out.println("=== TRAP RECEBIDA ===");

            String senderIp = event.getPeerAddress().toString().replace("/", "").split(":")[0];
            System.out.println("Origem: " + senderIp);

            int switchId = switchDAO.findOrCreateSwitch(
                    "switch-" + senderIp.replace(".", "-"),
                    senderIp
            );

            PDU pdu = event.getPDU();
            if (pdu == null) {
                System.out.println("PDU nula.");
                return;
            }

            for (int i = 0; i < pdu.size(); i++) {
                VariableBinding vb = pdu.get(i);
                OID oid = vb.getOid();
                String value = vb.getVariable().toString();

                System.out.println("OID: " + oid + " | Valor: " + value);

                if (oid.startsWith(new OID("1.3.6.1.6.3.1.1.5.3"))) { // linkDown
                    int port = extractIfIndex(value);
                    handlePortEvent(switchId, port, false, "linkDown");
                } else if (oid.startsWith(new OID("1.3.6.1.6.3.1.1.5.4"))) { // linkUp
                    int port = extractIfIndex(value);
                    handlePortEvent(switchId, port, true, "linkUp");
                }
            }

            System.out.println("=======================\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int extractIfIndex(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            System.err.println("Valor inesperado para ifIndex: " + value);
            return -1;
        }
    }

    private void handlePortEvent(int switchId, int portNumber, boolean isUp, String eventType) {
        try {
            // Atualiza status da porta no BD (assume que a porta já exista na tabela)
            portDAO.updatePortStatus(switchId, portNumber, isUp);

            System.out.println("[EVENTO] Switch " + switchId +
                    " | Porta " + portNumber +
                    " | Status: " + (isUp ? "UP" : "DOWN") +
                    " | Tipo: " + eventType);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
