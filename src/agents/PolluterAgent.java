package agents;

import environments.Environment;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PolluterAgent extends Agent {
    private static final int SIZE = Environment.SIZE;
    private Random random = new Random();
    private int pollutedX, pollutedY;
    private Map<String, Integer> bids = new HashMap<>();

    protected void setup() {
        // Pollutes every 5 seconds
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                synchronized (Environment.grid) {
                    bids.clear();
                    pollutedX = random.nextInt(SIZE);
                    pollutedY = random.nextInt(SIZE);
                    Environment.grid[pollutedX][pollutedY] = 1;
                    System.out.println("Polluter: Polluted cell (" + pollutedX + ", " + pollutedY + ")");
                    Environment.displayGrid();

                    // Find all active cleaners using DF Service
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("cleaner");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] results = DFService.search(myAgent, template);
                        if (results.length == 0) {
                            System.out.println("No active cleaners found.");
                            return;
                        }

                        ACLMessage message = new ACLMessage(ACLMessage.CFP);
                        message.setContent(pollutedX + "," + pollutedY);

                        for (DFAgentDescription result : results) {
                            message.addReceiver(result.getName());
                        }

                        send(message);
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Handles cleaner bids
        addBehaviour(new CleanerSelectionBehaviour());
    }

    private class CleanerSelectionBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.PROPOSE) {
                synchronized (Environment.grid) {
                    String[] parts = msg.getContent().split(",");
                    String cleanerName = msg.getSender().getLocalName();

                    int moveCost = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int charge = Integer.parseInt(parts[3]);

                    double distance = Math.sqrt(Math.pow(pollutedX - x, 2) + Math.pow(pollutedY - y, 2));
                    int totalCost = (int) (moveCost * distance);
                    int bidValue = (charge >= totalCost) ? totalCost : Integer.MAX_VALUE;

                    bids.put(cleanerName, bidValue);
                    System.out.println(
                    	    cleanerName + 
                    	    ", current position: (" + x + "," + y + ")" +
                    	    ", distance from (" + pollutedX + "," + pollutedY + "): " + String.format("%.2f", distance) +                    	  
                    	    ", moveCost: " + moveCost +
                    	    ", charge: " + charge +
                    	    ", bidValue: " + bidValue
                    	);

                    if (bids.size() == Environment.cleaners.size()) {
                        String bestCleaner = bids.entrySet().stream()
                                .min(Map.Entry.comparingByValue())
                                .get()
                                .getKey();

                        ACLMessage request = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        request.setContent(pollutedX + "," + pollutedY);
                        request.addReceiver(new AID(bestCleaner, AID.ISLOCALNAME));
                        send(request);

                        System.out.println("Polluter: Assigned " + bestCleaner + " to clean (" + pollutedX + ", " + pollutedY + ")");
                    }
                }
            } else {
                block();
            }
        }
    }
}

