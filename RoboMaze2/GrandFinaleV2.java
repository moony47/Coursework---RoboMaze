/*
  GrandFinaleV2.java

  PREAMBLE
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.text.MessageFormat;

public class GrandFinaleV2 {
  private int[] allDirections = new int[] {IRobot.AHEAD, IRobot.LEFT, IRobot.RIGHT, IRobot.BEHIND};
  private int pollRun = 0;
  private RobotData robotData;
  private boolean exploreMode = true;

  public void controlRobot(IRobot robot) {
    int direction = 0;

    if (pollRun == 0){
      if (robot.getRuns() == 0)
        robotData = new RobotData();
      exploreMode = true;
      robot.setHeading(IRobot.SOUTH);
    }

    direction = exploreMode ? exploreControl(robot) : backtrackControl(robot);

    //Make sure starts in explore mode even if started in a deadend
    if (pollRun++ == 0)
      exploreMode = true;

    robot.face(direction);
  }

  private int exploreControl (IRobot robot) {
    byte exits = nonwallExits(robot);
    double chance = Math.pow (robot.getRuns(), 2);

    switch (exits) {
      case 1:
        exploreMode = false;
        return deadendControl(robot);
      case 2: return corridorControl(robot);
      default:
        //If there is a direction to travel in from this junction saved, go that way
        if (robotData.junctions[robot.getLocation().x][robot.getLocation().y][1] > 0 && Math.floor(Math.random() * chance) > 0)
          return convertHeadingToDirection(robot, robotData.junctions[robot.getLocation().x][robot.getLocation().y][1]);

          //If not record the junction as brand new
          recordJunction(robot);

        //If can explore, then explore
        if (passageExits(robot) > 0) {
          //Record the direction travelling in from this junction
          robotData.junctions[robot.getLocation().x][robot.getLocation().y][2] = convertDirectionToHeading(robot, junctionControl(robot));
          return convertHeadingToDirection(robot, robotData.junctions[robot.getLocation().x][robot.getLocation().y][2]);
        }

        //If not, start backtracking
        exploreMode = false;
        return backtrack(robot);
    }
  }

  private int backtrackControl (IRobot robot) {
    byte exits = nonwallExits(robot);

    switch (exits) {
      case 2: return corridorControl(robot);
      default:
        if (passageExits(robot) > 0) {
          //Go back into exploreMode and act as normal. Overwrite the direction travelling from from this junction
          exploreMode = true;
          robotData.junctions[robot.getLocation().x][robot.getLocation().y][2] = convertDirectionToHeading(robot, junctionControl(robot));
          return convertHeadingToDirection(robot, robotData.junctions[robot.getLocation().x][robot.getLocation().y][2]);
        }

        //Backtrack through the junction into the direction the junction was originally found from
        return backtrack(robot);
    }
  }

  private int backtrack (IRobot robot) {
    System.out.println ("BACKTRACKING");
    robotData.junctions[robot.getLocation().x][robot.getLocation().y][2] = 0;
    return convertHeadingToDirection(robot, robotData.junctions[robot.getLocation().x][robot.getLocation().y][0]);
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

  private void printJunction (IRobot robot) {
    String heading = "";
    switch (robotData.junctions[robot.getLocation().x][robot.getLocation().y][0]){
      case IRobot.NORTH:  heading = "NORTH";
        break;
      case IRobot.EAST:   heading = "EAST";
        break;
      case IRobot.WEST:   heading = "WEST";
        break;
      case IRobot.SOUTH:  heading = "SOUTH";
        break;
    }

    System.out.println(MessageFormat.format ("New junction from {0}", heading));
  }

  private void recordJunction(IRobot robot) {
    //Set the arrived from to the heading behind robot
    robotData.junctions[robot.getLocation().x][robot.getLocation().y][0] = convertDirectionToHeading(robot, IRobot.BEHIND);
    //Set the left in to undecided yet
    robotData.junctions[robot.getLocation().x][robot.getLocation().y][2] = 0;

    //Print debug info
    //printJunction(robot);
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

  private byte beenbeforeExits (IRobot robot) {
    //Count the number of surrounding tiles are BEENBEOREs
    byte count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.BEENBEFORE)
        count++;

    //Return the count value
    return count;
  }

  public void setNewFastest () {
    for (int x = 0; x < 400; x++)
      for (int y = 0; y < 400; y++)
        robotData.junctions[x][y][1] = robotData.junctions[x][y][2];

    robotData.lastLength = pollRun;
  }

  public void reset() {
    if (pollRun < robotData.lastLength)
      setNewFastest();
    exploreMode = true;
    pollRun = 0;
  }

  private class RobotData {
    public int[][][] junctions = new int[400][400][3];
    public int lastLength = (int) Double.POSITIVE_INFINITY;

    public RobotData () {
      junctions = new int[400][400][3];
    }
  }
}
