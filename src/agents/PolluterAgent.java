package agents;

import environments.Environment;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
        // Pollutes every 8 seconds
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                synchronized (Environment.grid) { // Synchronize grid access
                    bids.clear(); // Reset bids before starting a new round
                    pollutedX = random.nextInt(SIZE);
                    pollutedY = random.nextInt(SIZE);

                    Environment.grid[pollutedX][pollutedY] = 1;
                    System.out.println("Polluter: Polluted cell (" + pollutedX + ", " + pollutedY + ")");
                    Environment.displayGrid();

                    // Ask all cleaners for bids
                    ACLMessage message = new ACLMessage(ACLMessage.CFP);
                    message.setContent(pollutedX + "," + pollutedY);
                    for (String cleaner : Environment.cleaners) {
                        message.addReceiver(new AID(cleaner, AID.ISLOCALNAME));
                    }
                    send(message);
                }
            }
        });

        // Handles cleaner bids continuously
        addBehaviour(new CleanerSelectionBehaviour());
    }

    private class CleanerSelectionBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.PROPOSE) {
                synchronized (Environment.grid) { // Synchronize grid access
                    String[] parts = msg.getContent().split(",");
                    String cleanerName = msg.getSender().getLocalName();
                    
                    //trying : 
                    
                    int moveCost = Integer.parseInt(parts[0]);
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int charge = Integer.parseInt(parts[3]);
                    
                    double distance = Math.sqrt(Math.pow(pollutedX - x, 2) + Math.pow(pollutedY - y, 2));

                    // Calculate total cost
                    int totalCost = (int) (moveCost * distance);

                    // Calculate bid value
                    int bidValue;
                    if (charge >= totalCost) {
                        // If charge is sufficient, bid is the total cost
                        bidValue = totalCost;
                    } else {
                        // If charge is insufficient, set a very high bid to disqualify the cleaner
                        bidValue = Integer.MAX_VALUE; // Use a very high value to ensure this cleaner is not chosen
                    }
                    
                    bids.put(cleanerName, bidValue);
                    System.out.println(
                    	    cleanerName + 
                    	    ", current position: (" + x + "," + y + ")" +
                    	    ", distance from (" + pollutedX + "," + pollutedY + "): " + String.format("%.2f", distance) +
                    	    ", charge: " + charge +
                    	    ", moveCost: " + moveCost +
                    	    ", bidValue: " + bidValue
                    	);

                    // Choose the best cleaner and assign the task
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
                        // Do not clear bids here
                    }
                }
            } else {
                // If no message is received, yield control to other behaviors
                // Remove block() and add a small delay to avoid busy-waiting
                try {
                    Thread.sleep(250); // Sleep for 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}