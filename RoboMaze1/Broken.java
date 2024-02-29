import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;

public class Broken {
  public void controlRobot(IRobot robot) {
    int heading = headingController(robot);
    ControlTest.test(heading, robot);
    robot.setHeading(heading);  /* Face the direction */
  }

  int headingController (IRobot robot) {
    //Get the relative positive of the target
    byte latitude = isTargetNorth(robot);
    byte longitude = isTargetEast(robot);

    //List all the options possible to move in
    List<Integer> options = new ArrayList<> ();

    //System.out.println("-----");
    //Remove all the headings that would lead to a collision
    for (int heading : new int[] {IRobot.NORTH, IRobot.SOUTH, IRobot.EAST, IRobot.WEST})
		  if (lookHeading(robot, heading) != IRobot.WALL) {
        /*switch (heading){
          case IRobot.NORTH: System.out.println("NORTH free");
            break;
          case IRobot.SOUTH: System.out.println("SOUTH free");
            break;
          case IRobot.EAST: System.out.println("EAST free");
            break;
          case IRobot.WEST: System.out.println("WEST free");
            break;
        }*/
        options.add(heading);
      }

    //If north is possible and towards target, never go south
    //If south is possible and towards target, never go north
    //If the target is the same latitude as the robot check the longitude
    //  If the longitude target option is possible, remove these options
    if (latitude == 1 && options.contains(IRobot.NORTH)) {
      options.remove((Object)IRobot.SOUTH);
    } else if (latitude == -1 && options.contains(IRobot.SOUTH)) {
      options.remove((Object)IRobot.NORTH);
    } else if (((longitude == 1 && options.contains(IRobot.EAST)) ||
              (longitude == -1 && options.contains(IRobot.WEST)))) {
      options.remove((Object)IRobot.NORTH);
      options.remove((Object)IRobot.SOUTH);
    }

    //If east is possible and towards target, never go west
    //If west is possible and towards target, never go east
    //If the target is the same longitude as the robot check the latitude
    //  If the latitude target option is possible, remove these options
    if (longitude == 1 && options.contains(IRobot.EAST)) {
      options.remove((Object)IRobot.WEST);
    } else if (longitude == -1 && options.contains(IRobot.WEST)) {
      options.remove((Object)IRobot.EAST);
    } else if (((latitude == 1 && options.contains(IRobot.NORTH)) ||
              (latitude == -1 && options.contains(IRobot.SOUTH)))) {
      options.remove((Object)IRobot.EAST);
      options.remove((Object)IRobot.WEST);
    }
    //Choose a random direction from remaining list
    return options.get((int) Math.floor(Math.random()*options.size()));
  }

  private byte isTargetNorth (IRobot robot) {
    //Set the default value to neither north or south,
    //if it dooesnt get changed before returning then they must be the same latitude
    byte value = 0;

    //Set the value based on comparing the latitude
    if (robot.getLocation().y > robot.getTargetLocation().y)
      value = 1;
    else if (robot.getLocation().y < robot.getTargetLocation().y)
      value = -1;

    //Return the value to the original caller
    return value;
  }

  private byte isTargetEast (IRobot robot) {
    //Set the default value to neither east or west,
    //if it dooesnt get changed before returning then they must be the same longitude
    byte value = 0;

    //Set the value based on comparing the longitude
    if (robot.getLocation().x > robot.getTargetLocation().x)
      value = -1;
    else if (robot.getLocation().x < robot.getTargetLocation().x)
      value = 1;

    //Return the value to the original caller
    return value;
  }

  private int lookHeading (IRobot robot, int heading) {
    //The offset is the heading equivilent to IRobot.AHEAD
    int offset = robot.getHeading();

    //The direction equivilent to the given heading is heading - offset but wrapped
    //arround back to 0 after 3 as there is no direction 4, 5, 6, etc
    int direction = (heading - offset + 4) % 4;

    //Convert direction back to the required type
    switch (direction){
      case 0: direction = IRobot.AHEAD;
        break;
      case 1: direction = IRobot.RIGHT;
        break;
      case 2: direction = IRobot.BEHIND;
        break;
      case 3: direction = IRobot.LEFT;
        break;
    }

    return robot.look(direction);
  }

  public void reset () {
    ControlTest.printResults();
  }
}
