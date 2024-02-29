
import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.text.MessageFormat;

public class GrandFinaleV4 {
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
        Junction j = robotData.FindJunction(robot.getLocation().x, robot.getLocation().y);
        if (j != null)
          if (j.bestDirectionToGo[convertDirectionToHeading(robot, IRobot.BEHIND)-IRobot.NORTH] > 0 && Math.floor(Math.random() * chance) >= 0)
            return convertHeadingToDirection(robot, robotData.FindJunction(robot.getLocation().x, robot.getLocation().y).bestDirectionToGo[convertDirectionToHeading(robot, IRobot.BEHIND)-IRobot.NORTH]);


        //If can explore, then explore
        if (passageExits(robot) > 0) {
          //Record the direction travelling in from this junction
          int d = junctionControl(robot);
          if (j == null) j = recordJunction(robot, convertDirectionToHeading(robot, IRobot.BEHIND), convertDirectionToHeading(robot, d));
          j.SetDirectionToGo(convertDirectionToHeading(robot, IRobot.BEHIND), convertDirectionToHeading(robot, d));
          return d;
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
          int d = junctionControl(robot);
          robotData.FindJunction(robot.getLocation().x, robot.getLocation().y).SetDirectionToGo(convertDirectionToHeading(robot, IRobot.BEHIND), convertDirectionToHeading(robot, d));
          return d;
        }

        //Backtrack through the junction into the direction the junction was originally found from
        return backtrack(robot);
    }
  }

  private int backtrack (IRobot robot) {
    System.out.println ("BACKTRACKING");
    //Direction to go when coming from the heading equivilent to BEHIND
    return convertHeadingToDirection(robot, robotData.FindJunction(robot.getLocation().x, robot.getLocation().y).backTrackDirection);
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

  private Junction recordJunction(IRobot robot, int arrivedFrom, int leftIn) {
    Junction j = robotData.FindJunction(robot.getLocation().x, robot.getLocation().y);
    if (j == null) {
      j = new Junction(robot.getLocation().x, robot.getLocation().y, convertDirectionToHeading(robot, IRobot.BEHIND));
      robotData.junctions[robotData.counter++] = j;
    }
    //Set the arrived from to the heading behind robot
    j.SetDirectionToGo(arrivedFrom, leftIn);

    return j;
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

  public void reset() {
    if (pollRun < robotData.bestCounter) {
      robotData.bestCounter = pollRun;
      System.out.println ("SET BEST: " + pollRun);
      for (Junction j : robotData.junctions)
        if (j != null)
          j.SetBestDirections();
    }
    robotData.counter = 0;
    exploreMode = true;
    pollRun = 0;
  }

  private class RobotData {
    public int counter = 0;
    public int bestCounter = 1000000000;
    public Junction[] junctions = new Junction[50000];
    public int lastLength = (int) Double.POSITIVE_INFINITY;

    public RobotData () {
      counter = 0;
      junctions = new Junction[50000];
    }

    public Junction FindJunction (int x, int y) {
      for (Junction j : junctions)
        if (j == null)
          break;
        else if (j.position[0] == x && j.position[1] == y)
          return j;

      //SHOULD NEVER HAPPEN
      return null;
    }
  }

  private class Junction {
    public int[] position = new int[2];
    public int[] directionToGo = new int[4];
    public int[] bestDirectionToGo = new int[4];
    public int backTrackDirection = 0;

    public Junction (int x, int y, int arrivedFrom) {
      position = new int[] {x, y};
      backTrackDirection = arrivedFrom;
    }

    public void SetDirectionToGo (int arrivedFrom, int leftIn) {
      directionToGo[arrivedFrom-IRobot.NORTH] = leftIn;
    }

    public void SetBestDirections () {
      for (int x = 0; x < 4; x++)
        bestDirectionToGo[x] = directionToGo[x];
    }
  }
}
