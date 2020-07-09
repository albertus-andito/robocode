package gg;

import robocode.*;

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
        while(isAlive) {
            vm.updateStatus();

            //Choose nearest enemy to fire (scanning)
            vm.enemyToShoot();

            //fire and rotate gun based on enemyToShoot

            //Choose direction to move
            vm.nextMovement();

            //move based on nextMovement
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        vm.store(e);
    }



    //Virtual Map
    class VM {

        //Sorted on enemy's next position
        Map<String, List<ScannedRobotEvent>> map = new HashMap<>();

        void store(ScannedRobotEvent e) {
            List events = map.getOrDefault(e.getName(), new LinkedList<>());
            events.add(e);
            //Only keep 10 last events

            map.put(e.getName(), events);

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


