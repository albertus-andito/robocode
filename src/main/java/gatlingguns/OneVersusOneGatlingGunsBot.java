package gatlingguns;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class OneVersusOneGatlingGunsBot extends AdvancedRobot {

    List<OneVersusOneGatlingGunsBot.WaveBullet> waves = new ArrayList<OneVersusOneGatlingGunsBot.WaveBullet>();
    static int[] stats = new int[31]; // 31 is the number of unique GuessFactors we're using
    // Note: this must be odd number so we can get
    // GuessFactor 0 at middle.
    int direction = 1;

    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    Point2D robotLocation;
    Point2D enemyLocation;
    double enemyDistance;
    double enemyAbsoluteBearing;
    double movementLateralAngle = 0.2;


    public void run() {
        //Allows the robot's base, gun, and radar to rotate independently
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        turnRadarRightRadians(Double.POSITIVE_INFINITY);
        do {
            scan();
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        robotLocation = new Point2D.Double(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation = vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation);

        move();

        // Enemy absolute bearing
        double absBearing = getHeadingRadians() + e.getBearingRadians();

        double radarTurn = absBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));

        // find our enemy's location:
        double ex = getX() + Math.sin(absBearing) * e.getDistance();
        double ey = getY() + Math.cos(absBearing) * e.getDistance();

        // Let's process the waves now:
        for (int i=0; i < waves.size(); i++)
        {
            OneVersusOneGatlingGunsBot.WaveBullet currentWave = (OneVersusOneGatlingGunsBot.WaveBullet)waves.get(i);
            if (currentWave.checkHit(ex, ey, getTime()))
            {
                waves.remove(currentWave);
                i--;
            }
        }

        double power = Math.min(3, Math.max(.1, calculateBulletPower(e.getDistance())));
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (e.getVelocity() != 0)
        {
            if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)
                direction = -1;
            else
                direction = 1;
        }
        int[] currentStats = stats; // This seems silly, but I'm using it to
        // show something else later
        OneVersusOneGatlingGunsBot.WaveBullet newWave = new OneVersusOneGatlingGunsBot.WaveBullet(getX(), getY(), absBearing, power,
                direction, getTime(), currentStats);

        int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<31; i++)
            if (currentStats[bestindex] < currentStats[i])
                bestindex = i;

        // this should do the opposite of the math in the WaveBullet:
        double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                / ((stats.length - 1) / 2);
        double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
        double gunAdjust = Utils.normalRelativeAngle(
                absBearing - getGunHeadingRadians() + angleOffset);
        setTurnGunRightRadians(gunAdjust);

        if (setFireBullet(power) != null) {
            waves.add(newWave);
        }
    }

    // Always try to move a bit further away from the enemy.
    // Only when a wall forces us we will close in on the enemy. We never bounce off walls.
    void move() {
        considerChangingDirection();
        Point2D robotDestination = null;
        double tries = 0;
        do {
            robotDestination = vectorToLocation(absoluteBearing(enemyLocation, robotLocation) + movementLateralAngle,
                    enemyDistance * (1.1 - tries / 100.0), enemyLocation);
            tries++;
        } while (tries < 100 && !fieldRectangle(WALL_MARGIN).contains(robotDestination));
        goTo(robotDestination);
    }

    void considerChangingDirection() {
        // Change lateral direction at random
        // Tweak this to go for flat movement
        double flattenerFactor = 0.05;
        if (Math.random() < flattenerFactor) {
            movementLateralAngle *= -1;
        }
    }

    RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
                getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
        double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
        // Hit the brake pedal hard if we need to turn sharply
        setMaxVelocity(Math.abs(getTurnRemaining()) > 33 ? 0 : MAX_VELOCITY);
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation) {
        return vectorToLocation(angle, length, sourceLocation, new Point2D.Double());
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
                sourceLocation.getY() + Math.cos(angle) * length);
        return targetLocation;
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    public double calculateBulletPower(double distance) {
        if (distance < 100) {
            return 3;
        }
        return 1;
    }

    public class WaveBullet {
        private double startX, startY, startBearing, power;
        private long fireTime;
        private int direction;
        private int[] returnSegment;

        public WaveBullet(double x, double y, double bearing, double power,
                          int direction, long time, int[] segment) {
            startX = x;
            startY = y;
            startBearing = bearing;
            this.power = power;
            this.direction = direction;
            fireTime = time;
            returnSegment = segment;
        }

        public double getBulletSpeed()
        {
            return 20 - power * 3;
        }

        public double maxEscapeAngle()
        {
            return Math.asin(8 / getBulletSpeed());
        }

        public boolean checkHit(double enemyX, double enemyY, long currentTime)
        {
            // if the distance from the wave origin to our enemy has passed
            // the distance the bullet would have traveled...
            if (Point2D.distance(startX, startY, enemyX, enemyY) <=
                    (currentTime - fireTime) * getBulletSpeed())
            {
                double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
                double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
                double guessFactor =
                        Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
                int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));
                returnSegment[index]++;
                return true;
            }
            return false;
        }
    }
}
