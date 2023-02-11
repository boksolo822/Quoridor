import game.engine.Engine;

import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) {

        int randomSeed= ThreadLocalRandom.current().nextInt();
        String seed=Integer.toString(randomSeed);
        String args1[]={"10","game.quoridor.QuoridorGame", seed, "5000000", "game.quoridor.players.HumanPlayer", "Agent"};
        try{
            Engine.main(args1);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}