/*
  Ex1.java

  PREAMBLE
  This robot controller will always lead the robot to the goal if given enough time
  and in fact there is a finite maximum number of steps the robot can take to reach
  the goal as every single tile type will at some point be exhausted of all possible
  directions to reach it from and then, due to the backtracking mode and the fact the
  robot will ignore previously explored routes in explore mode, never be reached again
  on the same run. Therefore there must be a finite cap on the number of runs it will take,
  the worst case being the run in which the robot exhausts every tile of possible directions
  to find it from and checking the goal tile last.

  In order to save the time of repeatedly initialising an array of all 4 directions, or iterating
  through the directions using a for loop from [0, 4) I created a class wide array called allDirections
  which contains {IRobot.AHEAD, IRobot.LEFT, IRobot.RIGHT, IRobot.BEHIND} which I then used in mutiples
  methods when the robot was checking the state of tiles in all 4 directions using the:
    for (int dir : allDirections)
  for loop. This is used in the two Exit counting methods passageExits() and nonwallExits()
  and also in the 3 control methods for each of deadends, corridors and junctions.

  The deadendControl() method simply looks in each direction and chooses the first one
  it finds to be a non-wall as this both deals with the turning around at a deadend and
  also when the robot stars turns 1 in a deadend. The most efficient method might instead
  be to simply turn to IRobot.BEHIND in all cases except when robot.getRuns() == 0, in
  which case use this for loop.

  The corridorControl() method is similar in that it checks each direction for non-wall
  exits and returns the first directions if finds that it can move into UNLESS it is
  BEHIND the robot, in which case it is ignored as we don't want the robot to turn arround
  in a corridor scenario. This means it will always move forward or take the only turn left
  or right.

  The junctionControl() method again uses this for loop but instead this time to look for
  PASSAGE and BEENBEFORE tiles seperately and add them to an array of each, WALL tiles are ignored.
  The method then checks if there are any exits it has taken before other than from where it
  just came from and if not, records the the new junction using RobotData. Regarding actually
  choosing a direction to travel the method first checks if there are any PASSAGE exits available
  using passagePointer which doubles as the number of passages in the array and randomly chooses
  one from the list if so. If not then a BEENBEFORE exit is instead randomly chosen from the
  beenbefores list.

  Crossroads are of course dealt with in the same way as junctions and so also use the junctionControl()
  method, as it is robust enough to handle having both 3 and 4 nonwall exits.

  I implemented the RobotData class slightly differently to the suggested method by using instead
  a 2D array for the junction positions as it allowed both the x and y coordinates of the junctions
  to be more easily accessed and stored. I defined the maximum size of the arrays to be 200*200 which
  is excessive for almost all the mazes but as the maximum height and width of the mazes being generated
  are 200 then in theory there could be 200*200 tiles that are all classed as junctions with 4 nonwall
  exits surrounding them so just to be sure. Another method would be to use Lists and to set the values
  of elements of indices < the current size of the list, but use the .add() method to append the list when it
  isnt yet big enough in order to save memory space in the majority of maze layout cases.

  Another difference in the approached I used fopr backtracking was storing the heading equivilent to IROBOT.BEHIND
  in the RobotData.arrivedFroms array rather than the direction the robot is currently travelling as then in future
  when converting the junction's arrivedFrom heading to a relative direction it does not need to be reversed. In
  order to easily convert heading to direction and direction to heading i created the convertDirectionToHeading()
  and convertHeadingToDirection() methods which make use of the properties of the constants:
    AHEAD + 1 = LEFT, LEFT + 1 = BEHIND, BEHIND + 1 = RIGHT, RIGHT + 1 = AHEAD (when under mod 4)
  and similarly with the headings. This meant all that was being executed was a few additions, subtractions and
  a remainder divison rather than a bunch of if-elseif-elses or switch-cases.

  The approach of using a boolean exploreMode I believe is not the optimal approach for efficiency in executing
  or in writting as much of the behaviour is the same for both modes. Instead I would add a conditional return
  statement to the junctionControl() method which in the case of a junctions exits being entirely explored, then accessed
  the RobotData.junctions and .arrivedFroms to find the backtrack route which would remove the need for an exploreMode
  variable and the behaviours being split up into two methods exploreControl() and backtrackControl().
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.text.MessageFormat;

public class Ex1 {
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
            robotData.searchJunction(robot.getLocation().x, robot.getLocation().y)
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
    switch (robotData.arrivedFroms[robotData.junctionCounter]){
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
    System.out.println(MessageFormat.format ("Junction {0}: ({1}, {2}) from {3}",
      robotData.junctionCounter, robotData.junctions[robotData.junctionCounter][0],
      robotData.junctions[robotData.junctionCounter][1], heading
    ));
  }

  private void recordJunction(IRobot robot) {
    //Add x,y coords to the list of junction positions
    robotData.junctions[robotData.junctionCounter] = new int[] {
      robot.getLocation().x,
      robot.getLocation().y
    };

    //Get the heading equivilent to BEHIND as that is where robot came from
    robotData.arrivedFroms[robotData.junctionCounter] = convertDirectionToHeading(robot, IRobot.BEHIND);

    //Print debug info and increment counter
    printJunction();
    robotData.junctionCounter++;
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
    public int[][] junctions = new int[200*200][2];
    public int[] arrivedFroms = new int[200*200];

    public void resetJunctionCounter() {
      junctionCounter = 0;
    }

    public int searchJunction (int x, int y) {
      for (int i = 0; i < junctions.length; i++) {
        if (x == junctions[i][0] && y == junctions[i][1])
          return arrivedFroms[i];
      }

      //Should never be reached
      return -1;
    }
  }
}
