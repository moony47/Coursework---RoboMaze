/*
  Ex2.java

  PREAMBLE
  This program behaves identically to Ex1 as there were very few changes between
  them. The changes made were simply as follows:
    Removed RobotData.junctions[][] array
    Removed RobotData.searchJunction() function
    Removed the recording of coordinates of junctions in recordJunction() method
    Decrement junctionCounter each time a junction is backtracked through
    Take the heading from RobotData.arrivedFroms[junctionCounter-1]
    Reduce the size of the arrivedFroms[] array

  The backtrackControl() function now instead uses the latest entry to the
  arrivedFroms[] array which as the junctionCounter decrements each time one
  is backtracked through, will always be the last junction required to reach
  the robots current position. This could be seen as a sort of tree structure
  of basecamps or pivot points. Every element in the array was reached from
  the beginning by passing through every element (junction) before it and the
  robots current position reached by passing through all of them and then
  exploring using the most recent entry as a pivot. When backtracking though
  a junction the robot is cutting off a branch of the tree and going back to
  using the junction before as its pivot point. Once a branch is cutoff, eg
  a junction backtracked through, the robot will never again return to those
  tiles in the current run of the maze.

  As a result of this approach the maximum size of the arrivedFroms[] array
  could actually be reduced as the tree system doesnt just append new elements
  onto the end of the array, it removes junctions that are redundant and only
  stores those that would be necessary to backtrack all the way back to the
  start of the maze. From doing some testing on the 200*200 prim generated mazes
  I found the value of junction counter rarely ever goes above 600 (as opposed
  to Ex1 which seemed to average around 5000 and ranged between [2000, 11000]) and
  so I felt it was a safe bet to reduce the size of the array from 200*200 to 1000.
  This saves some memory space used by the program in addition to the elimination of
  the junctions[][] array which was using a relatively massive amount of space
  with 200*200*2 element slots, most of which were always empty anyway. Therefore
  all of the memory adresses that both arrays had reserved in Ex1 are now freed up
  in Ex2 for other programs to make use of.

  From some testing with editting the maze and the loopy generator making a
  a different form of maze I believe I have found that this controller does not
  work in a specific scenario that cannot appear when just using the prim
  generator. The scenario being that the robot loops around some walls and finds
  a junction that it has previously been to and most recently left in a direction
  different to the direction the robot is currently entering it from. This causes
  errors and a crash as the robot thinks it should be at a different junction, the
  previous junction it recorded, and so acts as if it is there and backtracks the
  wrong way and eventually loses track entirely to start colliding with walls in
  many different locations including in deadends.

  (welp. didnt realise both the tree analogy and the loop error would be the next excercise...)
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.text.MessageFormat;

public class Ex2 {
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
      default: return junctionControl(robot);
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
        } else {
          //Backtrack through the junction into the direction the junction was originally found from
          return convertHeadingToDirection(
            robot,
            robotData.arrivedFroms[--robotData.junctionCounter]
          );
        }
    }
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
    //Check all tiles and find the arrays of PASSAGEs and BEENBEFOREs
    int[] passages = new int[4];
    byte passagePointer = 0;
    int[] beenbefores = new int[4];
    byte beenbeforePointer = 0;

    //Add to the correct list or neither if a wall
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.PASSAGE) {
        passages[passagePointer] = dir;
        passagePointer++;
      } else if (robot.look(dir) == IRobot.BEENBEFORE) {
        beenbefores[beenbeforePointer] = dir;
        beenbeforePointer++;
      }

    //Check if this is a new junction before traversing it
    if (beenbeforePointer == 1)
      //Record new junction
      recordJunction(robot);

    //If there are non-beenbefore passages, randomly choose between those.
    //If not randomly choose between the beenbefores.
    if (passagePointer > 0)
      return passages[(int) Math.floor(Math.random() * passagePointer)];
    else
      return beenbefores[(int) Math.floor(Math.random() * beenbeforePointer)];
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
    public int[] arrivedFroms = new int[1000];

    public void resetJunctionCounter() {
      junctionCounter = 0;
    }
  }
}
