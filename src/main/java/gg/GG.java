package gg;

import robocode.*;
import robocode.util.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GG extends AdvancedRobot {

    //Enemies map sorted on angle and distance
    private VM vm = new VM();


    //

    public void run() {
        //Allows the robot's base, gun, and radar to rotate independently
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        //
        boolean isAlive = true;

        turnRadarRight(720);

        while(isAlive) {
            vm.updateStatus();

            //Choose nearest enemy to fire (scanning)
            EnemyInfo enemy = vm.enemyToShoot();

            //rotate and fire gun based on enemyToShoot
            //this is wrong
            turnGunRight(enemy.angle);
            fire(1); //maybe we can set the bullet power based on enemy's distance

            //Choose direction to move
            Movement movement = vm.nextMovement();

            //move based on nextMovement

            //Possibly need to scan environment again here
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        vm.store(e);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        vm.removeEnemy(e.getName());
    }

    enum radarBehaviors {
        MELEE, LOCK
    }

    //Virtual Map
    class VM {

        //Sorted on enemy's next position
        Map<String, List<ScannedRobotEvent>> map = new HashMap<>();

        radarBehaviors radarBehavior = radarBehaviors.MELEE;

        void store(ScannedRobotEvent e) {
            List events = map.getOrDefault(e.getName(), new LinkedList<>());
            events.add(e);
            //Only keep 10 last events
            if (events.size() > 10) {
                events.remove(0);
            }
            map.put(e.getName(), events);
            if (map.size() == 1) {
                radarBehavior = radarBehaviors.LOCK;
            }

            //lock radar on enemy
            if (radarBehavior == radarBehaviors.LOCK) {
                double radarTurn =
                        // Absolute bearing to target
                        getHeadingRadians() + e.getBearingRadians()
                        // Subtract current radar heading to get turn required
                        - getRadarHeadingRadians();
                setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));
            }

        }

        EnemyInfo enemyToShoot() {
            //return Structure for angle and distance (distance to predict how many steps and the bullet energy)
            return null;
        }

        Movement nextMovement() {
            //return structure of direction and acceleration
            return null;
        }

        void updateStatus() {
            //our own tank status
        }

        void removeEnemy(String name) {
            map.remove(name);
            if (map.size() == 1) {
                radarBehavior = radarBehaviors.LOCK;
            }
        }
    }



    class EnemyInfo {
        double angle;
        double distance;
    }

    class Movement {
        double direction;
        double acceleration;
    }


}


