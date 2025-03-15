package agents;

import environments.Environment;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class CleanerAgent extends Agent {
    private Random random = new Random();
    private int charge = 100;
    private int x = 4, y = 4; // Start at grid center
    private int currentCost;

    protected void setup() {
        // Register with Yellow Pages (DF Service)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("cleaner");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Agent " + getLocalName() + " is registered in Yellow Pages as a cleaner");
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new BidBehaviour());
        
    }

    private class BidBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.CFP) {
                synchronized (Environment.grid) {
                    currentCost = random.nextInt(15) + 1;

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(currentCost + "," + x + "," + y + "," + charge);
                    send(reply);
                }
            } else if (msg != null && msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                synchronized (Environment.grid) {
                    String[] parts = msg.getContent().split(",");
                    int X = Integer.parseInt(parts[0]);
                    int Y = Integer.parseInt(parts[1]);

                    Environment.grid[X][Y] = 0;
                    System.out.println(getLocalName() + " cleaned (" + X + ", " + Y + ")");
                    Environment.displayGrid();

                    double distance = Math.sqrt(Math.pow(X - x, 2) + Math.pow(Y - y, 2));
                    int totalCost = (int) (currentCost * distance);
                    charge -= totalCost;

                    x = X;
                    y = Y;

                    if (charge <= 0) {
                        System.out.println(getLocalName() + " is out of charge and shutting down!");
                        deregisterAndTerminate();
                    }
                }
            } else {
                block();
            }
        }
    }

    private void deregisterAndTerminate() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        doDelete();
    }
}
