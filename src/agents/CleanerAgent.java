package agents;

import environments.Environment;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class CleanerAgent extends Agent {
    private Random random = new Random();
    int charge = 100;
    int x =4 , y=4; //middle of the grid
    int currentCost; 
    
    protected void setup() {
        // Add behaviors for bidding and cleaning
        addBehaviour(new BidBehaviour());
    }

    // Listens for CFP messages and responds with a bid
 // Listens for CFP messages and responds with a bid
    private class BidBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive(); // Check for incoming messages
            if (msg != null && msg.getPerformative() == ACLMessage.CFP) {
                synchronized (Environment.grid) { // Synchronize grid access
                	// If the cell is already clean, bid high; otherwise, bid low
                	currentCost = random.nextInt(20)+1;

                    // Send the bid back to the polluter
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(currentCost)+','+x+','+y+','+String.valueOf(charge));
                    send(reply);
                }
            }
            else if(msg != null && msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
            	
            	synchronized (Environment.grid) { // Synchronize grid access
                    String[] parts = msg.getContent().split(",");
                    int X = Integer.parseInt(parts[0]);
                    int Y = Integer.parseInt(parts[1]);

                    // Clean the grid
                    Environment.grid[X][Y] = 0;
                    System.out.println(getLocalName() + " cleaned (" + x + ", " + y + ")");
                    Environment.displayGrid();
                    
                    
                    double distance = Math.sqrt(Math.pow(X - x, 2) + Math.pow(Y - y, 2));
                    // Calculate total cost
                    int totalCost = (int) (currentCost * distance);
                    charge-=totalCost;
                
                    x = X;
                    y = Y;
                                        
                } }
            else {
            	try {
                    Thread.sleep(250); // Sleep for 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            }
        }
    }