package environments;

import agents.CleanerAgent;
import agents.PolluterAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.List;

public class Environment {
    public static final int SIZE = 10;  // 10x10 grid
    public static int[][] grid = new int[SIZE][SIZE]; // Initialize grid with 0s
    public static List<String> cleaners = List.of("Cleaner1", "Cleaner2", "Cleaner3");

    public static void displayGrid() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("-------------------------");
    }

    public static void main(String[] args) {
        // JADE Runtime setup
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer container = rt.createMainContainer(profile);

        try {
            // Start JADE’s built-in GUI (RMA)
            AgentController rma = container.createNewAgent("RMA", "jade.tools.rma.rma", null);
            rma.start();

            // Create and start the PolluterAgent
            AgentController polluter = container.createNewAgent("Polluter", PolluterAgent.class.getName(), null);

            // Create and start multiple CleanerAgents
            AgentController cleaner1 = container.createNewAgent("Cleaner1", CleanerAgent.class.getName(), null);
            AgentController cleaner2 = container.createNewAgent("Cleaner2", CleanerAgent.class.getName(), null);
            AgentController cleaner3 = container.createNewAgent("Cleaner3", CleanerAgent.class.getName(), null);

            polluter.start();
            cleaner1.start();
            cleaner2.start();
            cleaner3.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}