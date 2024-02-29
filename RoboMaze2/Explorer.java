import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Explorer {
  private int[] allDirections = new int[] {IRobot.AHEAD, IRobot.LEFT, IRobot.RIGHT, IRobot.BEHIND};
  private int pollRun = 0;
  private RobotData robotData;

  public void controlRobot(IRobot robot) {
    int exits = nonwallExits(robot);
    int direction = 0;

    if (robot.getRuns() == 0 && pollRun == 0)
      robotData = new RobotData();

    switch (exits) {
      case 1: direction = deadendControl(robot);
        break;
      case 2: direction = corridorControl(robot);
        break;
      default: direction = junctionControl(robot);
        break;
    }

    robot.face(direction);
    pollRun++;
  }

  private int deadendControl (IRobot robot) {
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
    //Check all tiles and find the list of PASSAGEs and BEENBEFOREs
    List<Integer> passages = new ArrayList<>();
    List<Integer> beenbefores = new ArrayList<>();
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.PASSAGE)
        passages.add(dir);
      else if (robot.look(dir) == IRobot.BEENBEFORE)
        beenbefores.add(dir);

    //Check if this is a new junction before traversing it
    if (beenbefores.size() == 1)
      //Record new junction
      recordJunction(robot);
    else if (passages.size() == 0) {
      //Backtrack through the junction into the direction the junction was originally found from
      return convertHeadingToDirection(robot, robotData.arrivedFroms[findIndexOfJunction(new int[] {robot.getLocation().x, robot.getLocation().y})]);



    //If there are non-beenbefore passages, randomly choose between those.
    //If not randomly choose between the beenbefores.
    if (passages.size() > 0)
      return passages.get ((int) Math.floor(Math.random() * passages.size()));
    else
      return beenbefores.get ((int) Math.floor(Math.random() * beenbefores.size()));
  }

  private int findIndexOfJunction (int[] coords) {
    for (int i = 0; i < robotData.junctions.length; i++) {
      if (robotData.junctions[i][0] == coords[0] && robotData.junctions[i][1] == coords[1])
        return i;
    }

    //Should never be reached
    return -1;
  }

  private int convertHeadingToDirection (IRobot robot, int heading) {
    //Reduce the numbers all down to 0, 1, 2, 3 by using the fact AHEAD and NORTH are identities
    //and that the 4 directions and 4 headings loop round in %4

    //The offset is the heading equivilent to IRobot.AHEAD
    int offset = robot.getHeading() - IRobot.NORTH;
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
    int offset = robot.getHeading() - IRobot.NORTH;
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

    System.out.println(
      "Junction " + robotData.junctionCounter + ": (" +
      robotData.junctions[robotData.junctionCounter][0] +
      ", " + robotData.junctions[robotData.junctionCounter][1] +
      ") from " + heading
    );
  }

  private void recordJunction(IRobot robot) {
    //Add x,y vector to the list of junction positions
    robotData.junctions[robotData.junctionCounter] = new int[] {robot.getLocation().x, robot.getLocation().y};

    //Get the heading equivilent to BEHIND as that is where robot came from
    int heading = convertDirectionToHeading(robot, IRobot.BEHIND);
    robotData.arrivedFroms[robotData.junctionCounter] = heading;

    if (convertHeadingToDirection (robot, heading) != IRobot.BEHIND)
      System.out.println ("BROKEN");

    //Print debug info and increment counter
    printJunction();
    robotData.junctionCounter++;
  }

  private int nonwallExits (IRobot robot) {
    //Count the number of surrounding tiles are not WALLs
    int count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) != IRobot.WALL)
        count++;

    //Return the count value
    return count;
  }

  private int passageExits (IRobot robot) {
    //Count the number of surrounding tiles are PASSAGEs
    int count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.PASSAGE)
        count++;

    //Return the count value
    return count;
  }

  private int beenbeforeExits (IRobot robot) {
    //Count the number of surrounding tiles are BEENBEFOREs
    int count = 0;
    for (int dir : allDirections)
      if (robot.look(dir) == IRobot.BEENBEFORE)
        count++;

    //Return the count value
    return count;
  }

  public void reset() {
    robotData.resetJunctionCounter ();
  }

  private class RobotData {
    public int junctionCounter = 0;
    public int[][] junctions = new int[200*200][2];
    public int[] arrivedFroms = new int[200*200];

    public void resetJunctionCounter() {
      junctionCounter = 0;
    }
  }
}
