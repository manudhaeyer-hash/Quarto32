import com.codingame.gameengine.runner.MultiplayerGameRunner;

/**
 * Lanceur local. Voir COMPILE_RUN_LOCAL.txt pour les commandes exactes
 * (ATTENTION : mvn exec:exec, PAS exec:java -> sinon page 404).
 */
public class Main {
    public static void main(String[] args) {
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        gameRunner.setLeagueLevel(1);
        gameRunner.setSeed(42L);

        // Boss local : solver C++ (config/Boss.cpp) compile en binaire.
        //   Windows : g++ -O2 -o config/boss.exe config/Boss.cpp  -> "config\\boss.exe"
        //   Linux   : g++ -O2 -o config/boss    config/Boss.cpp  -> "config/boss"
        String boss = System.getProperty("os.name").toLowerCase().contains("win")
                ? "config\\boss.exe" : "config/boss";
        gameRunner.addAgent(boss, "Boss-Alpha");
        gameRunner.addAgent(boss, "Boss-Omega");

        // Ancien boss Java (archive) : javac config/Boss.java -> config/Player.class
        // gameRunner.addAgent("java -cp config Player", "Boss-Java");

        gameRunner.start();   // http://localhost:8888/test.html
    }
}
