package gatlingguns;

import robocode.*;

public class GatlingGunsRobot extends AdvancedRobot {

    public int crossover (int individual1, int individual2) {
        return individual1 + (individual2-individual1)*(int)Math.random();
    }

    public int mutation (int individual) {
        int ind2;
        do {
            ind2 = individual + (int)(-1+2*Math.random());
        } while (ind2 < 1 || ind2 >= 6);
        return ind2;
    }

    private enum behaviors {
        CRAZY, FORWARD_AND_REDO, CIRCULAR, WALLS, DODGE
    };

    behaviors individual;

    public void crazyMoves () {
        setTurnLeft(Math.random()*400*(-1 + 2*Math.random()));
        execute();
        ahead(Math.random()*400*(-1 + 2*Math.random()));
    }

    public void forwardAndRedoMoves () {
        ahead(80);
        ahead(-80);
    }

    private boolean circularDirection = false;

    public void circularMoves () {
        if (circularDirection) {
            setAhead(100);
            setTurnLeft(50);
        } else {
            setAhead(-100);
            setTurnLeft(-50);
        }
        execute();
    }

    public void circularCollision () {
        circularDirection = !circularDirection;
        setStop();
        execute();
        turnRight(90);
    }

    public void betaWallsMoves () {
        setAhead(99999);
        execute();
    }

    public void betaWallsCollision () {
        turnLeft(90);
        setTurnGunRight(99999);
        setAhead(99999);
        execute();
    }

    public void dontMove () {

    }

    private double previousEnergy = 100;
    private int movementDirection = 1;
    private int gunDirection = 1;

    public void dodgeMoves () {
        setTurnGunRight(99999);
        execute();
    }

    public void dodgeOnScanned (ScannedRobotEvent e) {
        setTurnRight(e.getBearing()+90-30*movementDirection);
        double changeInEnergy = previousEnergy-e.getEnergy();
        if (changeInEnergy>0 && changeInEnergy<=3) {
            movementDirection = -movementDirection;
            setAhead((e.getDistance()/4+25)*movementDirection);
        }
        gunDirection = -gunDirection;
        setTurnGunRight(99999*gunDirection);
        setFire (2) ;
        previousEnergy = e.getEnergy();
        execute();
    }

    int[] population = null;
    int[] elapsedTime = null;
    int select = 0;
    int time;
    int fitness;

    public void run () {
        population = new int[4];
        elapsedTime = new int[4];

        for (int i=0; i < 4; i++)
            population[i] = (int)(Math.random()*4 + 1);

        fitness =  (int)getEnergy();

        while (true) {
            time = (int)getTime();
            switch (population[select]) {
                case 1:
                    individual = behaviors.FORWARD_AND_REDO;
                    break;
                case 2:
                    individual = behaviors.CIRCULAR;
                    break;
                case 3:
                    individual = behaviors.DODGE;
                    break;
                case 4:
                    individual = behaviors.WALLS;
                    break;
                case 5:
                    individual = behaviors.CRAZY;
                    break;
                default:
                    System.out.println("Error!");
                    break;
            }

            switch (individual) {
                case CRAZY:
                    setTurnGunRight(99999);
                    execute();
                    crazyMoves();
                    break;
                case FORWARD_AND_REDO:
                    setTurnGunRight(99999);
                    execute();
                    forwardAndRedoMoves();
                    break;
                case CIRCULAR:
                    setTurnGunRight(99999);
                    execute();
                    circularMoves();
                    break;
                case WALLS:
                    setTurnGunRight(99999);
                    execute();
                    betaWallsMoves();
                    break;
                case DODGE:
                    dodgeMoves();
                    break;
            }
        }
    }

    public void onHitByBullet (HitByBulletEvent e) {
        if (fitness-getEnergy() > 15) {
            select++;
            if (select == 4) {
                elapsedTime[select-1] = (int)e.getTime()-time;
                time = (int)e.getTime();
                sortIndividuals();
                for (int i = 1; i < 4; i++)
                    if (Math.random() < 0.7)
                        population[i] = crossover(population[0], population[i]);
                for (int i = 0; i < 4; i++)
                    if (Math.random() < 0.1)
                        population[i] = mutation(population[i]);
                select = 0;
            }
            fitness = (int)getEnergy();
        }
    }

    public void sortIndividuals() {
        int best, aux;
        for (int i = 0; i < 4; i++) {
            best = elapsedTime[i];
            for (int j = i; j < 4; j++)
                if (elapsedTime[j] > best) {
                    aux = best;
                    best = elapsedTime[j];
                    elapsedTime[j] = aux;
                }
        }
    }


    public void onScannedRobot (ScannedRobotEvent e) {
        switch (individual) {
            case DODGE:
                dodgeOnScanned(e);
                break;
            default:
                setTurnGunRight(99999);
                setFire (2/(1+0.25*(e.getDistance()-getBattleFieldWidth())));
                execute();
                break;
        }
    }

    public void onHitWall (HitWallEvent e) {
        switch (individual) {
            case CIRCULAR:
                circularCollision();
                break;
            case WALLS:
                betaWallsCollision();
                break;
        }
    }

    public void onHitRobot (HitRobotEvent e) {
        switch (individual) {
            case CIRCULAR:
                circularCollision();
                break;
            case WALLS:
                betaWallsCollision();
                break;
        }
    }
}