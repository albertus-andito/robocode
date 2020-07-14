package gatlingguns;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;


public class GatlingGunsRobot extends AdvancedRobot {

    private boolean isAlive = true;
    private final VirtualMap map = new VirtualMap(getBattleFieldWidth(), getBattleFieldHeight());
    int currentTurn = 0;

    @Override
    public void run () {
        setBodyColor(Color.DARK_GRAY);
        setGunColor(Color.GREEN);
        setRadarColor(Color.GRAY);
        setBulletColor(Color.RED);
        setScanColor(Color.WHITE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        turnRadarRightRadians(Double.POSITIVE_INFINITY);

        while (isAlive) {
            switch (map.enemiesCount()) {
                case 0: // no enemy detected yet
                    moveToCenter();
                    break;

                case 1: // only one enemy on radar
                    aim();
                    moveToEnemyBack();
                    break;

                default: // many enemies
                    aim();
                    moveToEdge();
            }

            scan();
        }
    }

    public void aim() {
        Point2D self = map.getOwnPosition();
        Predictor predictor = map.getNearest(self);
        double distance = calcDistance(self, predictor.getPosition(currentTurn + 1));
        int tau = (int) Math.round(distance / 17.0);
        Point2D goal = predictor.getPosition(currentTurn + tau);
        double angle = calcHeading(self, goal);
        if (equ(angle, getGunHeadingRadians(), 0.01)) {
            fire(0.5);
        } else {
            turnGunRightRadians(angle);
        }
    }

    public void moveToCenter() {
        Point2D self = map.getOwnPosition();
        Point2D center = map.getCenter();
        double angle = 0.0;

        if (equ(self.getX(), center.getX(), 0.01)) {
            if (self.getY() > center.getY()) {
                angle = Math.PI;
            }
            if (self.getY() < center.getY()) {
                angle = 0.0;
            }
        } else if (equ(self.getY(), center.getY(), 0.01)) {
            if (self.getX() > center.getX()) {
                angle = - Math.PI / 2;
            }
            if (self.getX() < center.getX()) {
                angle = Math.PI / 2;
            }
        } else {
            angle = calcHeading(self, center);
        }

        double distance = calcDistance(self, center);

        turnRightRadians(angle);
        ahead(distance);
    }

    public void moveToEnemyBack() {
        // TODO: TBD
        moveToCenter(); // temporaly
    }

    public void moveToEdge() {
        // TODO: TBD
        moveToCenter(); // temporaly
    }

    @Override
    public void onHitWall(HitWallEvent hitWallEvent) {
        // TODO:
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        double bearing = absBeading(getHeadingRadians(), event.getBearingRadians());
        map.updateEnemy(
                event.getName(),
                addPoints(map.getOwnPosition(), scalar(bearing, event.getDistance())),
                event.getEnergy(),
                event.getHeading(),
                event.getVelocity());
    }

    @Override
    public void onHitRobot(HitRobotEvent hitRobotEvent) {
        // TODO:
    }

    // ============================= MAP ==================================
    class VirtualMap {

        private final double width;
        private final double height;
        private final Map<String, Predictor> enemies;

        private int currentTurn = 0;

        private Point2D self;

        VirtualMap(double width, double height) {
            this.width = width;
            this.height = height;
            this.enemies = new HashMap<>();
        }

        int enemiesCount() {
            return enemies.size();
        }

        Collection<Predictor> getEnemies() {
            return enemies.values();
        }

        Predictor getNearest(Point2D point) {
            double distance = Double.POSITIVE_INFINITY;
            Predictor winer = null;
            for (Predictor predictor : enemies.values()) {
                double estimation = calcDistance(point, predictor.getPosition(currentTurn + 1));
                if (estimation < distance) {
                    distance = estimation;
                    winer = predictor;
                }
            }
            return winer;
        }

        Point2D getCenter() {
            return new Point2D.Double(width / 2, height / 2);
        }

        Point2D getOwnPosition() {
            return self;
        }

        void setOwnPosition(double x, double y) {
            this.self = new Point2D.Double(x, y);
        }

        void updateEnemy(String name, Point2D detected, double energy, double heading, double velocity) {
            Predictor predictor = enemies.getOrDefault(name, new Predictor());
            enemies.put(name, predictor);

            predictor.update(currentTurn, detected, energy, heading, velocity);
        }

        void incTurn() {
            currentTurn++;
        }

        double absBearing(double alpha, double beta) {
            double gamma = alpha + beta;
            return gamma > 360.0 ? gamma - 360.0 : gamma < 0 ? 360.0 + gamma : gamma;
        }
    }

    // ============================ Predictor ==============================
    class Predictor {
        private static final int PREDICTOR_SIZE = 1_000;
        private final RobotInfo[] infos = new RobotInfo[PREDICTOR_SIZE];

        int calcIndex(int turn) {
            while (turn < 0) {
                turn += PREDICTOR_SIZE;
            }
            return turn % PREDICTOR_SIZE;
        }

        RobotInfo getInfo(int turn) {
            return infos[calcIndex(turn)];
        }

        RobotInfo getInfoOrNew(int turn) {
            RobotInfo info = getInfo(turn);
            return info != null ? info : new RobotInfo();
        }

        void putInfo(int turn, RobotInfo info) {
            infos[calcIndex(turn)] = info;
        }

        void clearInfo(int turn) { // in circular buffer we should clear unused values
            infos[calcIndex(turn)] = null;
        }

        void update(int turn, Point2D detected, double energy, double heading, double velocity) {
            RobotInfo info = getInfoOrNew(turn);
            info.detected = detected;
            info.energy = energy;
            info.heading = heading;
            info.velocity = velocity;
        }

        Point2D getPosition(int turn) {
            RobotInfo info = getInfoOrNew(turn);

            if (info.detected != null) {
                return info.detected;
            }

            if (info.predicted != null) {
                return info.predicted;
            }

            // make prediction
            info.predicted = predictor(getPosition(turn - 1), getPosition(turn - 2));
            putInfo(turn, info);

            return info.predicted;
        }

        Point2D predictor(Point2D oneBack, Point2D twoBack) {
            return new Point2D.Double(
                    2 * oneBack.getX() - twoBack.getX(),
                    2 * oneBack.getY() - twoBack.getY());
        }
    }

    // ============================= Robot Info ===============================
    class RobotInfo {
        Point2D detected;
        Point2D predicted;
        double energy;
        double heading;
        double velocity;
    }

    // === === === === === Linear Algebra === === === === ===
    private static final double PI = Math.PI;
    private static final double PI2 = PI * 2;

    double absBeading(double ownHeading, double enemyBearing) {
        while (enemyBearing < 0) {
            enemyBearing += PI2;
        }

        double total = ownHeading + enemyBearing;

        while (total > PI2) {
            total += PI2;
        }

        return total;
    }

    Point2D scalar(double angle, double distance) {
        return new Point2D.Double(distance * Math.sin(angle), distance * Math.cos(angle));
    }

    Point2D addPoints(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() + b.getX(), a.getY() + b.getY());
    }

    Point2D subPoints(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    boolean equ(double a, double b, double e) {
        return Math.abs(a - b) < e;
    }

    /*
     *           |
     *           |
     *  ---------x---------
     *           |
     *           |
     */
    double calcHeading(Point2D self, Point2D target) {
        Point2D goal = subPoints(target, self);
        double dx = Math.abs(goal.getX());
        double dy = Math.abs(goal.getY());
        if (equ(dx, 0.0, 0.001)) {
            return goal.getY() > 0.0 ? 0.0 : PI;
        }
        if (equ(dy, 0.0, 0.001)) {
            return goal.getX() > 0.0 ? PI / 2 : - PI/2;
        }
        double angle = Math.atan(dx / dy);
        if (goal.getX() > 0.0) {
            return goal.getY() > 0.0 ? angle : PI - angle;
        } else {
            return goal.getY() > 0.0 ? PI2 - angle : PI + angle;
        }
    }

    double calcDistance(Point2D self, Point2D target) {
        return Math.sqrt(Math.pow(self.getX() - target.getX(), 2) + Math.pow(self.getY() - target.getY(), 2));
    }
}
