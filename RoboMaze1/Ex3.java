/*
	Ex3.java

	PREAMBLE
  This program dooes follow the specification for a robot that homes in on the target
  location when the tiles in the direction of the target are free, but chooses a
  different direction randomly when that is not possible. The robot does not always
  reach the end of the maze as it can get stuck in deadends and corner points if
  they are pointing towards the target tile.
  For example, where X is the target, # is the robot:

                  |   |
    ______________|   |
    |    __________#__|
    |    |__________________
    |____________________X_|
------------------------------------------------------------------------------------
    __________________________
    _____    ____________#___|
        |    |_________________
        |___________________X_|
------------------------------------------------------------------------------------
    ______
    |  X |     ______
    |    |    |     |
    |    |    |  #  |
    |    |____|     |_____
    |_____________________

  The method used here for headingController() uses a list of possible options
  for the robot to choose to move in. The list is initally filled with all directions
  that would not lead the robot into a wall. Then the NORTH-SOUTH case is checked
  using the latitude value returned by the isTargetNorth() function; here there are 4 cases:
    Target is north AND robot can go north
    Target is south AND robot can go south
    Either of (Target is east and can go east) or (Target is west and can go west) and (None of the above)
    None of the above
  Each leading to a different code path:
    Remove south as an option as can and want to go north
    Remove north as an option as can and want to go south
    Remove both north and south as either cant or dont mind about
      going north or south AND there is a priority direction in {east, west}
    Change nothing and leave both north and south as possible directions
      for random choosing
  A similar thing is then done with the longitude value from isTargetEast() affecting
  the options to go east and west. Finally out of all the remaining options in the list
  one is randomly chosen using the even probability function from the previous iteration
  using the options.size() as the maximum value [exclusive].

  To improve this iteration of the robot, it needs some way to check when it has become
  stuck in a deadend or corner of the maze, to do this we could use the IRobot.BEENBEFORE tiles
  as markers for locations the robot has been but didnt succeed in finding a route from last time.
  We could also using larger scope variables in the class to store information between ticks
  of the maze and remember which moves didnt work out from each point in the maze so that they
  can be avoided in the future.
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;

public class Ex3 {
  public void controlRobot(IRobot robot) {
    int heading = headingController(robot);
    ControlTest.test(heading, robot);
    robot.setHeading(heading);  /* Face the direction */
  }

  int headingController (IRobot robot) {
    //Get the relative position of the target
    byte latitude = isTargetNorth(robot);
    byte longitude = isTargetEast(robot);

    //List all the options possible to move in;
    //ignoring all the headings that would lead to a collision
    List<Integer> options = new ArrayList<> ();
    for (int heading : new int[] {IRobot.NORTH, IRobot.SOUTH, IRobot.EAST, IRobot.WEST})
		  if (lookHeading(robot, heading) != IRobot.WALL)
        options.add(heading);

    //Consider NORTH-SOUTH cases
    if (latitude == 1 && options.contains(IRobot.NORTH)) {
      //If north is possible and towards target, never go south
      options.remove((Object)IRobot.SOUTH);
    } else if (latitude == -1 && options.contains(IRobot.SOUTH)) {
      //If south is possible and towards target, never go north
      options.remove((Object)IRobot.NORTH);
    } else if ( (longitude ==  1 && options.contains(IRobot.EAST)) ||
                (longitude == -1 && options.contains(IRobot.WEST))) {
      //If the longitude target option is possible, remove these options as there is higher priority
      options.remove((Object)IRobot.NORTH);
      options.remove((Object)IRobot.SOUTH);
    }

    //Consider EAST-WEST cases
    if (longitude == 1 && options.contains(IRobot.EAST))
      //If east is possible and towards target, never go west
      options.remove((Object)IRobot.WEST);
    else if (longitude == -1 && options.contains(IRobot.WEST))
      //If west is possible and towards target, never go east
      options.remove((Object)IRobot.EAST);
    else if ( (latitude ==  1 && options.contains(IRobot.NORTH)) ||
              (latitude == -1 && options.contains(IRobot.SOUTH))) {
      //If the latitude target option is possible, remove these options as there is higher priority
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
    //the +4 just ensures it is positive so the %4 always returns [0, 4), not (-4, 4)
    int direction = (heading - offset + 4) % 4;

    //Convert direction back to the required constants standard
    //eg if direction is 2: AHEAD+2 => BEHIND
    direction += IRobot.AHEAD;

    //Return the tile type in the given heading direction
    return robot.look(direction);
  }

  public void reset () {
    ControlTest.printResults();
  }
}
