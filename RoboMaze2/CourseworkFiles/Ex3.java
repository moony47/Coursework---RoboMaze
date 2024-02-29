/*
  Ex3.java

  PREAMBLE
  This final iteration of the depth search robot controller will now always find
  its way to the end of any maze capped at a finite number of runs and is able to
  deal with normal mazes, loopy mazes and the blank maze of any size up to the 200*200
  size.

  The changes between this iteration and Ex2 are as follows:
    Encountering a junction while in explore mode ALWAYS record the new junction
    not just when it has exactly 1 BEENBEFORE exit.
    Explore mode now checks whether a junction has at least 1 unexplored PASSAGE
    exit and if not, enters backtrack mode and backtracks though the junction
    itself immediately.
    The backtrack code is in its own method so both explore and backtrack modes
    can access it.
    The arrivedFroms[] array is again larger as the robot can now record a junction
    multiple times each and so the maximum junctions currenly stored can reach up to
    35000 ish in the 200*200 loopy maze from some random tests, so 50000 array slots
    are reserved to be safe.
    Some minor efficiency changes such as the junctionControl() method no longer making
    a list of BEENBEFORE exits as this feature was never used, either there is a passage
    to explore down, or the exit is chosen by the backtracking method.

  The previous iteration was unable to deal with loops in the mazes as its condition for
  recording new junctions and the direction to backtrack them through was if the junction
  hadnt been encountered before at all and so there was only 1 BEENBEFORE exit. However
  with loopy mazes it is possible to pass through a junction and then while exploring from
  there, find the same junction again but from a different direction, which the robot then
  thought was the previous junction it had recorded so attempted to backtrack though it using
  the wrong data. This caused collisions and confusion about the robots current location.
  In this case the robot should record it a second time so that it can keep on exploring and
  then backtrack the correct direction. This iteration fullfills this requirement by recording
  any junction encountered in explore mode, not just those that are brand new. An extended issue
  from this was that it would record the new junction then try to continue exploring even if the
  junctions exits had all been explored, so the functionality to immediately backtrack the junction
  it had just recorded if there are na PASSAGE exits was required.
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.text.MessageFormat;

public class Ex3 {
  private int[] allDirections = new int[] {IRobot.AHEAD, IRobot.LEFT, IRobot.RIGHT, IRobot.BEHIND};
  private int pollRun = 0;
  private RobotData robotData;
  private boolean exploreMode = true;

  public void controlRobot(IRobot robot) {
    int direction = 0;

    if (robot.getRuns() == 0 && pollRun == 0)
      robotData = new RobotData();

    //Chose behaviour based on current mode
    direction = exploreMode ? exploreControl(robot) : backtrackControl(robot);

    pollRun++;
    robot.face(direction);
  }

  private int exploreControl (IRobot robot) {
    byte exits = nonwallExits(robot);

    switch (exits) {
      case 1:
        //Enter backtrack mode and turn around at deadend
        exploreMode = false;
        return deadendControl(robot);
      case 2: return corridorControl(robot);
      default:
        //New direction for this junction so start new branch
        recordJunction(robot);

        //If can explore, then explore
        if (passageExits(robot) > 0)
          return junctionControl(robot);

        //If not, start backtracking
        exploreMode = false;
        return backtrack(robot);
    }
  }

  private int backtrackControl (IRobot robot) {
    byte exits = nonwallExits(robot);

    switch (exits) {
      //case 1: return deadendControl(robot); DEADENDS NEVER REACHED IN BACKTRACK MODE
      case 2: return corridorControl(robot);
      default:
        if (passageExits(robot) > 0) {
          //Go back into exploreMode and act as normal
          exploreMode = true;
          return junctionControl(robot);
        }

        //Backtrack through the junction into the direction the junction was originally found from
        return backtrack(robot);
    }
  }

  private int backtrack (IRobot robot) {
    return convertHeadingToDirection(robot, robotData.arrivedFroms[--robotData.junctionCounter]);
  }

  private int deadendControl (IRobot robot) {
    //This deals with turning around at a deadend but also with starting in a deadend
    //such that IRobot.BEHIND is not actually the correct direction

    //Search through the 4 surrounding tiles and return the first one which is not a wall
    for (int dir : allDirections)
      if (robot.look(dir) != IRobot.WALL)
        return dir;

    //0 only returned if error occured
    return 0;
  }

  private int corridorControl (IRobot robot) {
    //Travel forwards, or turn left or right if at corner
    for (int dir : allDirections)
      if (dir != IRobot.BEHIND && robot.look(dir) != IRobot.WALL)
        return dir;

    //0 only returned if error occured
    return 0;
  }

  private int junctionControl (IRobot robot) {
    //Check all tiles and find the arrays of PASSAGEs
    int[] passages = new int[4];
    byte passagePointer = 0;

    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.PASSAGE) {
        passages[passagePointer] = dir;
        passagePointer++;
      }

    //If there are non-beenbefore passages, randomly choose between those.
    return passages[(int) Math.floor(Math.random() * passagePointer)];
  }

  private int convertHeadingToDirection (IRobot robot, int heading) {
    //Reduce the numbers all down to 0, 1, 2, 3 by using the fact AHEAD and NORTH are identities
    //and that the 4 directions and 4 headings loop round in %4

    //The offset is the heading equivilent to IRobot.AHEAD
    byte offset = (byte) (robot.getHeading() - IRobot.NORTH);
    heading -= IRobot.NORTH;

    //The direction equivilent to the given heading is heading - offset but wrapped
    //arround back to 0 after 3 as there is no direction 4, 5, 6, etc
    //the +4 just ensures it is positive so the %4 always returns [0, 4), not (-4, 4)
    int direction = (heading - offset + 4) % 4;

    //Convert direction back to the required constants standard
    //eg if direction is 2: AHEAD+2 => BEHIND
    direction += IRobot.AHEAD;

    return direction;
  }

  private int convertDirectionToHeading (IRobot robot, int direction) {
    //Reduce the numbers all down to 0, 1, 2, 3 by using the fact AHEAD and NORTH are identities
    //and that the 4 directions and 4 headings loop round in %4

    //The offset is the heading equivilent to IRobot.AHEAD
    byte offset = (byte) (robot.getHeading() - IRobot.NORTH);
    direction -= IRobot.AHEAD;

    //The heading equivilent to the given direction is direction + offset but wrapped
    //arround back to 0 after 3 as there is no heading 4, 5, 6, etc
    int heading = (offset + direction) % 4;

    //Convert heading back to the required constants standard
    //eg if direction is 2: NORTH+2 => SOUTH
    heading += IRobot.NORTH;

    return heading;
  }

  private void printJunction () {
    //Get correct name for heading
    String heading = "";
    switch (robotData.arrivedFroms[robotData.junctionCounter-1]){
      case IRobot.NORTH:  heading = "NORTH";
        break;
      case IRobot.EAST:   heading = "EAST";
        break;
      case IRobot.WEST:   heading = "WEST";
        break;
      case IRobot.SOUTH:  heading = "SOUTH";
        break;
    }

    //Print junction information for debugging
    System.out.println(MessageFormat.format ("Junction {0}: from {1}", robotData.junctionCounter, heading));
  }

  private void recordJunction(IRobot robot) {
    //Get the heading equivilent to BEHIND as that is where robot came from
    robotData.arrivedFroms[robotData.junctionCounter++] = convertDirectionToHeading(robot, IRobot.BEHIND);

    //Print debug info
    printJunction();
  }

  private byte nonwallExits (IRobot robot) {
    //Count the number of surrounding tiles are not WALLs
    byte count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) != IRobot.WALL)
        count++;

    //Return the count value
    return count;
  }

  private byte passageExits (IRobot robot) {
    //Count the number of surrounding tiles are PASSAGEs
    byte count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.PASSAGE)
        count++;

    //Return the count value
    return count;
  }

  public void reset() {
    robotData.resetJunctionCounter ();
    exploreMode = true;
  }

  private class RobotData {
    public int junctionCounter = 0;
    public int[] arrivedFroms = new int[50000];

    public void resetJunctionCounter() {
      junctionCounter = 0;
    }
  }
}
