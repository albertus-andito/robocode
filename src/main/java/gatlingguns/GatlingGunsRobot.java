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
import robocode.util.Utils;


public class GatlingGunsRobot extends AdvancedRobot {

    private boolean isAlive = true;
    private VirtualMap map;

    @Override
    public void run () {
        map = new VirtualMap(getBattleFieldWidth(), getBattleFieldHeight());
        setBodyColor(Color.DARK_GRAY);
        setGunColor(Color.GREEN);
        setRadarColor(Color.GRAY);
        setBulletColor(Color.RED);
        setScanColor(Color.WHITE);

        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);

        while (isAlive) {
            out.println("In loop: " + getTime());
            Predictor predictor;
            switch (map.enemiesCount()) {
                case 0: // no enemy detected yet
                    out.println("move to center");
                    moveToCenter();
                    break;

                case 1: // only one enemy on radar
                    out.println("move to enemy back");
                    predictor = map.getEnemies().iterator().next();
                    moveToEnemyBack(predictor);
                    aim(predictor);
                    break;

                default: // many enemies
                    out.println("move to edge");
                    predictor = map.getEnemies().iterator().next();
                    moveToEdge();
                    aim(predictor);
            }
            out.println("before scanning");
            execute();
            out.println("after scanning");
        }
    }

    private Point2D getOwnPosition() {
        return new Point2D.Double(getX(), getY());
    }

    public void aim(Predictor predictor) {
        out.println("aiming");
        // Enemy absolute bearing
        double bx = predictor.current.pos.getX() - getX();
        double by = predictor.current.pos.getY() - getY();
        double absBearing = getHeadingRadians() + Math.acos(by / Math.sqrt(bx*bx+by+by));
        double gunTurn = absBearing - getGunHeadingRadians() - getHeadingRadians();
        setTurnGunRightRadians(Utils.normalRelativeAngle(gunTurn));

        setFire(0.2);
    }

    private void moveTo(double x, double y) {
        double a;
        setTurnRightRadians(Math.tan(
                a = Math.atan2(x -= getX(), y -= getY())
                        - getHeadingRadians()));
        setAhead(Math.hypot(x, y) * Math.cos(a));
    }

    public void moveTo(Point2D point) {
        Point2D self = getOwnPosition();
        double dx = point.getX() - self.getX();
        double dy = point.getY() - self.getY();
        double distance = Math.sqrt(dx*dx + dy*dy);
        double angle = Math.acos(dy/distance);
        setTurnRightRadians(angle);
        setAhead(distance);
    }

    public void moveToCenter() {
        moveTo(map.getCenter().getX(), map.getCenter().getY());
    }

    public void moveToEnemyBack(Predictor predictor) {
        Point2D pos = predictor.current.pos;
        double x = pos.getX() - 100 * Math.sin(predictor.current.heading);
        double y = pos.getY() - 100 * Math.cos(predictor.current.heading);
        if(x > getBattleFieldWidth()) {
            x -= 2*(x-getBattleFieldWidth());
        }
        if(y > getBattleFieldHeight()) {
            y -= 2*(y-getBattleFieldHeight());
        }
        moveTo(x, y);
    }

    public void moveToEdge() {
        // TODO: TBD
        moveToCenter(); // temporaly
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        out.println("enemy scanned");
        double bearing = absBeading(getHeadingRadians(), event.getBearingRadians());
        map.updateEnemy(
                event.getName(),
                addPoints(getOwnPosition(), scalar(bearing, event.getDistance())),
                event.getEnergy(),
                event.getHeading(),
                event.getVelocity());
    }

    // ============================= MAP ==================================
    class VirtualMap {

        private final double width;
        private final double height;
        private final Map<String, Predictor> enemies;

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
                double estimation = calcDistance(point, predictor.getPosition(getTime() + 1));
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

        void updateEnemy(String name, Point2D detected, double energy, double heading, double velocity) {
            Predictor predictor = enemies.getOrDefault(name, new Predictor());
            enemies.put(name, predictor);

            predictor.update(detected, energy, heading, velocity);
        }

        double absBearing(double alpha, double beta) {
            double gamma = alpha + beta;
            return gamma > 360.0 ? gamma - 360.0 : gamma < 0 ? 360.0 + gamma : gamma;
        }
    }

    // ============================ Predictor ==============================
    class Predictor {
        private RobotInfo current;
        private RobotInfo previous;

        void update(Point2D detected, double energy, double heading, double velocity) {
            out.println("update info");
            previous = current;
            current = new RobotInfo();
            current.pos = detected;
            current.energy = energy;
            current.heading = heading;
            current.velocity = velocity;
            current.turn = getTime();
        }

        Point2D getPosition(long turn) {
            out.println("Predict for turn " + turn);
            double tau = turn - current.turn;;
            double dx;
            double dy;
            if (previous == null) {
                dx = current.velocity * Math.sin(current.heading);
                dy = current.velocity * Math.cos(current.heading);
            } else {
                dx = current.pos.getX() - previous.pos.getX();
                dy = current.pos.getY() - previous.pos.getY();
            }
            return new Point2D.Double(current.pos.getX() + dx * tau, current.pos.getY() + dy * tau);
        }

    }

    // ============================= Robot Info ===============================
    class RobotInfo {
        Point2D pos;
        double energy;
        double heading;
        double velocity;
        long turn;
    }

    // === === === === === Linear Algebra === === === === ===
    private static final double PI = Math.PI;
    private static final double PI2 = PI * 2;

    private double absBeading(double ownHeading, double enemyBearing) {
        while (enemyBearing < 0) {
            enemyBearing += PI2;
        }

        double total = ownHeading + enemyBearing;

        while (total > PI2) {
            total += PI2;
        }

        return total;
    }

    private Point2D scalar(double angle, double distance) {
        return new Point2D.Double(distance * Math.sin(angle), distance * Math.cos(angle));
    }

    private Point2D addPoints(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() + b.getX(), a.getY() + b.getY());
    }

    private Point2D subPoints(Point2D a, Point2D b) {
        return new Point2D.Double(a.getX() - b.getX(), a.getY() - b.getY());
    }

    private boolean equ(double a, double b, double e) {
        return Math.abs(a - b) < e;
    }

    /*
     *           |
     *           |
     *  ---------x---------
     *           |
     *           |
     */
    private double calcHeading(Point2D self, Point2D target) {
        Point2D goal = subPoints(target, self);
        double dy = Math.abs(goal.getY());
        return Math.acos(dy / calcDistance(self, target));
    }

    private double calcDistance(Point2D self, Point2D target) {
        double dx = self.getX() - target.getX();
        double dy = self.getY() - target.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }
}
