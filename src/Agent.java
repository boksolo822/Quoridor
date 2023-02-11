import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import game.quoridor.MoveAction;
import game.quoridor.QuoridorGame;
import game.quoridor.QuoridorPlayer;
import game.quoridor.WallAction;
import game.quoridor.players.DummyPlayer;
import game.quoridor.utils.PlaceObject;
import game.quoridor.utils.QuoridorAction;
import game.quoridor.utils.WallObject;

public class Agent extends QuoridorPlayer {
    private final List<WallObject> walls = new LinkedList<WallObject>();
    private final QuoridorPlayer[] players = new QuoridorPlayer[2];
    private final List<PlaceObject> disabledMoves = new ArrayList<>();
    private int numWalls;
    private boolean startOnTop;

    public Agent(int i, int j, int color, Random random) {
        super(i, j, color, random);
        players[color] = this;
        players[1 - color] = new DummyPlayer((1 - color) * (QuoridorGame.HEIGHT - 1), j, 1 - color, null);
        numWalls = 0;
        if (i == 8) {
            startOnTop = false;
        } else {
            startOnTop = true;
        }
    }

    /**
     * Ha az enemy kevesebb mint 3 sor távolságra van a kezdőállapottól, akkor 20% eséllyel meghívódik a fel lerakó eljárás, vagy ha tud előre lép, ha nem akkor oldalra.
     * Ha legalább 3 sor távolságra van az enemy, akkor falakkal kikényszeríti hogy hátra kelljen lépnie.
     */
    @Override
    public QuoridorAction getAction(QuoridorAction prevAction, long[] remainingTimes) {
        if (prevAction instanceof WallAction) {
            WallAction a = (WallAction) prevAction;
            walls.add(new WallObject(a.i, a.j, a.horizontal));
        } else if (prevAction instanceof MoveAction) {
            MoveAction a = (MoveAction) prevAction;
            players[1 - color].i = a.to_i;
            players[1 - color].j = a.to_j;
        }

        int wallOrMove=random.nextInt(1,11);

        if (enemyDistanceToStart() < 3 && wallOrMove<=8)  {
            if (canMoveForward(i, j)) {
                return getForwardMove();
            }
            return getPromisingMove();
        } else {
            QuoridorAction wall = putWall();
            if (wall instanceof WallAction) {
                walls.add(new WallObject(((WallAction) wall).i, ((WallAction) wall).j, ((WallAction) wall).horizontal));
                numWalls++;
            }
            return wall;
        }
    }

    /**
     * Azt ellenőrzi, hogy lehetséges előre lépés. Máshogy kell ellenőrizni ha fekete vagy fehér a "bábu"
     * @param i a kiindulási helyzet sorszáma
     * @param j a kiindulási helyzet oszlopszáma
     * @return ha léphet előre akkor true, egyébként false
     */
    public boolean canMoveForward(int i, int j) {
        if (startOnTop) {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i + 1, j), walls, players) && !isDisabled(new MoveAction(i, j, i + 1, j)) && !inBlackHole(i+1,j)) {
                return true;
            } else {
                return false;
            }
        } else {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i - 1, j), walls, players) && !isDisabled(new MoveAction(i, j, i - 1, j))&& !inBlackHole(i-1,j)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Visszaadja hogy az enemy léphet-e előre
     * Ha egyébként tudna előre lépni, de azzal feketelyukba kerülne, akkor úgy értelmezi hogy nem tud.
     * @param i a kiindulási helyzet sorszáma
     * @param j a kiindulási helyzet oszlopszáma
     * @return ha léphet előre, akkor true
     */
    public boolean canEnemyMoveForward(int i, int j) {
        if (startOnTop) {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i - 1, j), walls, players)) {
                return true;
            } else {
                return false;
            }
        } else {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i + 1, j), walls, players) && !isDisabled(new MoveAction(i, j, i + 1, j))) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Azt ellenőrzi, hogy lehetséges-e hátra lépés. Máshogy kell ellenőrizni ha fekete vagy fehér a "bábu"
     * @param i a kiindulási helyzet sorszáma
     * @param j a kiindulási helyzet oszlopszáma
     * @return ha léphet hátra akkor true, egyébként false
     */
    public boolean canMoveBack(int i, int j) {
        if (startOnTop) {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i - 1, j), walls, players) && !isDisabled(new MoveAction(i, j, i - 1, j))) {
                return true;
            } else {
                return false;
            }
        } else {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i + 1, j), walls, players) && !isDisabled(new MoveAction(i, j, i + 1, j))) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Abban az esetben kerül meghívásra, ha nem tud előre lépni.
     * Ekkor egy "ígéretes" lépést keres, a következők szerint:
     * Megnézi hogy jobbra vagy balra érdemes-e lépnie annak érdekében hogy minél hamarabb előre tudjon lépni.
     * Ha nincs ilyen, akkor hátra lép egyet, és elmenti hogy utána ne lépjen előre, mivel felesleges.
     * @return a választott lépést adja vissza
     */
    public QuoridorAction getPromisingMove() {

        if (canMoveForward(i, j) ) {
            return getForwardMove();
        }

        boolean right = false;
        boolean left = false;
        int leftCount = 0;
        int rightCount = 0;
        int tmpJ = j;

        while (tmpJ >= 0) {
            if (!QuoridorGame.isWallBetween(walls, new PlaceObject(i, tmpJ), new PlaceObject(i, tmpJ - 1)) && tmpJ != 0) {
                tmpJ--;
                leftCount++;
                if (canMoveForward(i, tmpJ)) {
                    left = true;
                    break;
                }
            } else {
                leftCount = Integer.MAX_VALUE;
                break;
            }
        }
        tmpJ = j;
        while (tmpJ <= 8) {
            if (!QuoridorGame.isWallBetween(walls, new PlaceObject(i, tmpJ), new PlaceObject(i, tmpJ + 1)) && tmpJ != 8) {
                tmpJ++;
                rightCount++;
                if (canMoveForward(i, tmpJ)) {
                    right = true;
                    break;
                }
            } else {
                rightCount = Integer.MAX_VALUE;
                break;
            }
        }

        if (left == false && right == false) {
            if (canMoveBack(i, j)) {
                if(!QuoridorGame.isPlayerOn(players,new PlaceObject(getForwardMove().to_i,getForwardMove().to_j))){
                    disabledMoves.add(new PlaceObject(i, j));
                }
                return getBackMove();
            } else {
                if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i, j - 1), walls, players)) {
                    return new MoveAction(i, j, i, j - 1);
                } else if(QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i, j + 1), walls, players)) {
                    return new MoveAction(i, j, i, j + 1);
                }
            }
        } else if (leftCount >= rightCount) {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i, j + 1), walls, players)) {
                return new MoveAction(i, j, i, j + 1);
            }
        } else {
            if (QuoridorGame.checkCandidateMove(new PlaceObject(i, j), new PlaceObject(i, j - 1), walls, players)) {
                return new MoveAction(i, j, i, j - 1);
            }
        }
        return getRandomMove();
    }

    /**
     * @return Visszaad a hátralépéshez egy MoveActiont. Itt is külön kell vizsgálni hogy fekete vagy fehér-e.
     */
    public MoveAction getBackMove() {
        if (startOnTop == true) {
            return new MoveAction(i, j, i - 1, j);
        } else {
            return new MoveAction(i, j, i + 1, j);
        }
    }

    /**
     * @return Visszaad az előrelépéshez egy MoveActiont. Itt is külön kell vizsgálni hogy fekete vagy fehér-e.
     */
    public MoveAction getForwardMove() {
        if (startOnTop == true) {
            return new MoveAction(i, j, i + 1, j);
        } else {
            return new MoveAction(i, j, i - 1, j);
        }
    }

    /**
     * ELlenőrzi hogy az adott előrelépés szerepel-e a letiltottak között. Oda-vissza lépegetés elkerülése végett.
     * @param action a kérdéses előrelépés
     * @return ha szerepel a tiltottak között akkor true, ha nem akkor false
     */
    public boolean isDisabled(MoveAction action) {
        for (PlaceObject act : disabledMoves) {
            if (act.i == action.to_i && act.j == action.to_j) {
                return true;
            }
        }
        return false;
    }

    /**
     * lerak egy falat hogy az ellenfelet hátralépésre kényszerítse, vagyis "körbekeríti". Ha már körbe van kerítve,akkor a getPromisingMove()-t hívja meg.
     * @return A végrehajtandó akció
     */
    public QuoridorAction putWall() {

        if (canEnemyMoveForward(players[1 - color].i, players[1 - color].j)) {
            if (startOnTop) {
                if (QuoridorGame.checkWall(new WallObject(players[1 - color].i - 1, players[1 - color].j, true), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                    return new WallAction(players[1 - color].i - 1, players[1 - color].j, true);
                } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i - 1, players[1 - color].j - 1, true), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                    return new WallAction(players[1 - color].i - 1, players[1 - color].j - 1, true);
                } else {
                    return getPromisingMove();
                }
            }

            if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j, true), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                return new WallAction(players[1 - color].i, players[1 - color].j, true);
            } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j - 1, true), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                return new WallAction(players[1 - color].i, players[1 - color].j - 1, true);
            } else {
                return getPromisingMove();
            }
        } else {
            if (canEnemyMoveForward(players[1 - color].i, players[1 - color].j - 1)) {

                if(startOnTop){
                    if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j - 1, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i, players[1 - color].j - 1, false);
                    } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i - 1, players[1 - color].j - 1, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i - 1, players[1 - color].j - 1, false);
                    }
                }
                else{
                    if (QuoridorGame.checkWall(new WallObject(players[1 - color].i-1, players[1 - color].j - 1, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i-1, players[1 - color].j - 1, false);
                    } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j - 1, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i, players[1 - color].j - 1, false);
                    }
                }

            } else if (canEnemyMoveForward(players[1 - color].i, players[1 - color].j + 1)) {

                if(startOnTop){
                    if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i, players[1 - color].j, false);
                    } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i - 1, players[1 - color].j, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i - 1, players[1 - color].j, false);
                    }
                }
                else{
                    if (QuoridorGame.checkWall(new WallObject(players[1 - color].i-1, players[1 - color].j, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i-1, players[1 - color].j, false);
                    } else if (QuoridorGame.checkWall(new WallObject(players[1 - color].i, players[1 - color].j, false), walls, players) && numWalls < QuoridorGame.MAX_WALLS) {
                        return new WallAction(players[1 - color].i, players[1 - color].j, false);
                    }
                }
            }
            return getPromisingMove();
        }
    }

    /**
     * @return Visszaadja hogy az ellenfél hány sornyit lépett előre
     */
    public int enemyDistanceToStart() {
        if (startOnTop) {
            return 8 - players[1 - color].i;
        } else {
            return players[1 - color].i;
        }
    }

    /**
     * Visszaad egy random lépést a lehetségesek közül
     * @return a kapott MoveAction
     */
    public MoveAction getRandomMove() {
        List<QuoridorAction> allAction = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            for (int j = 0; j <= 8; j++) {
                if (QuoridorGame.checkCandidateMove(new PlaceObject(this.i, this.j), new PlaceObject(i, j), walls, players)) {
                    allAction.add(new MoveAction(this.i, this.j, i, j));
                }
            }
        }
        return (MoveAction) allAction.get(random.nextInt(0, allAction.size()));
    }

    /**
     * Ez az eljárás a feketelyuk vizsgáló metódushoz készíti el a szükséges falakat tároló listát.
     */
    public List<WallObject> createAlternativeWalls(int i, int j){
        List<WallObject> alternativeWalls=new ArrayList<>();
        alternativeWalls.addAll(walls);
        if(startOnTop){
            for(int k=0; k<=7; k++){
                alternativeWalls.add(new WallObject(i-1,k,true));
            }
            return alternativeWalls;
        }
        for(int k=0; k<=7; k++){
            alternativeWalls.add(new WallObject(i,k,true));
        }
        return alternativeWalls;
    }

    /**
     * Ez az eljárás a feketelyuk vizsgáló metódushoz készíti el a szükséges játékosokat tároló tömböt.
     */
    public QuoridorPlayer[] createAlternativePlayers(int i,int j){
        QuoridorPlayer[] alternativePlayers=new QuoridorPlayer[2];
        QuoridorPlayer playerEnemy=new DummyPlayer(players[1-color].i,players[1-color].j, 1-color,players[1-color].random);
        alternativePlayers[1-color]=playerEnemy;

        QuoridorPlayer playerAgent=new DummyPlayer(i,j, color,random);
        alternativePlayers[color]=playerAgent;

        return alternativePlayers;
    }

    /**
     * Ez az eljárás azt vizsgálja, hogy egy esetleges előre lépéssel az ágens úgynevezett "fekete lyukba" kerülne-e
     * Fekete lyuk azt jelenti, hogy az adott pozícióbol hátralépés nélkül nem létezik útvonal a célhoz.
     * @param i a tervezett lépés sorszáma
     * @param j a tervezett lépés oszlopszáma
     * @return ha fekete lyukba kerülne akkor true, különben false
     */
    public boolean inBlackHole(int i, int j){
        List<WallObject> alternativeWalls=createAlternativeWalls(i,j);
        QuoridorPlayer[] alternativePlayers=createAlternativePlayers(i,j);

        if(QuoridorGame.pathExists(alternativeWalls,alternativePlayers,color)){
            return false;
        }
        return true;
    }
}