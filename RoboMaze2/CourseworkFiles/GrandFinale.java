/*
  GrandFinale.java

  V1 - Stacks (Explore and Backtrack design)
  I wrote a previous version of the GrandFinale task which was very similar to the
  more guided exercises. It effectively was a very minor tweak which just stored
  the stack of headings to leave junctions in in the order that the robot finds the junctions.
  So then after the first run, rather than using explore and backtrack modes etc, it
  just follows the stack of headings. In the normal maze this works perfectly and after 1
  run of the maze, the robot will always take the optimal route to the target. The issue
  with this design came when dealing with loopy mazes where there are mutiple possible
  routes to the target, the robot will find a route and then stick to it no matter how
  long it is.

  V2 - A* Algorithm (New design)
  This controller uses a completely different approach to V1 and to the previous
  excercises. It is an adaptation of the A* pathing algorithm and works by using
  the same logic in this algorithm by calculating a heuristic disance of each tile
  to the target tile and using stored g values for each tile to keep track of how
  many steps it takes for the robot to reach the tile from the start tile.

  Excluding the basic components of the A* algorithm, the adjustments in order to
  allow the robot to use the algorithm are as follows:
    - On the first run, the algorithm will be run.
    - On subsequent runs, the robot will just follow the path created on the first.
    - When the next node in the open set is chosen and the robot is not currently on
    it, the robot will backtrack two paths from the next node and the current node
    back to the start and find their intersection. Then travel backwards towards
    the start until reaching that intersection, at which point switching to follow
    the path out to the next node. Once there, the usual A* logic is used to extend
    the open set and give more options to choose from.
    - The selection of the next node is slightly weighted towards the heuristic as
    from some light testing it appears to give a better balance of thoroughness and
    efficiency in the first run of each maze.

  In comparison to GrandFinaleV1.java, it is slower at finding the path in the basic mazes
  do to the amount of backtracking it does however will always find it eventually and then
  in subsequent runs of the same maze, take the most direct route to the target; the exact
  same route V1 would take.
  In the loopy mazes however, this controller will find a much more efficient path to the
  target and stick to that in subsequent runs of the same maze which was the downfall of V1;
  which very frequently found a much longer route than was possible, and then just stuck with
  it no matter how inefficient in was in subsequent runs of the maze.

  Run == 1 --> V1 more efficient for standard mazes
               V1 more efficient for loopy mazes
  Run >= 2 --> V1 and V2 equally efficient for standard mazes (Both optimal)
               V2 more efficient for loopy mazes (V2 Optimal)
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GrandFinale {
  int currentNodeInPath = 0;
  RobotData rd;
  int pollRun = 0;

  public void controlRobot (IRobot robot) {
    int heading = 0;

    //If this is the first turn on the first run, create a new RobotData object
    //with new default values, set the current tile to be the first node with a
    //g ("distance from start") value of 0 and add it to the openNodes list to be
    //chosen in teh first iteration of aStarSearch()
    if (pollRun == 0 && robot.getRuns() == 0) {
      rd = new RobotData(robot);
      rd.allNodes[robot.getLocation().x][robot.getLocation().y].g = 0;
      rd.openNodes.add (rd.allNodes[robot.getLocation().x][robot.getLocation().y]);
    }

    //If a path exists, then just follow it as it is the best route
    //If not, then this is the first run so run the aStarSearch algorithm to find
    //the best route for all runs after this.
    if (rd.path.size() > 0)
      heading = rd.path.get(currentNodeInPath++);
    else
      heading = aStarSearch(robot);

    pollRun++;
    robot.setHeading(heading);
  }

  public void reset () {
    pollRun = 0;
    currentNodeInPath = 0;
  }

  private Node lowestFNode () {
    //Slightly weighted such that getting close to the target is more important
    //than being closer to the start. Should make the first run a little faster
    //while only making the chance of producing a less efficient path only a little
    //more likely.

    //From testing found that values less than 1.3 take a long time on teh first run for 200*200 mazes
    //and that values greater than 1.5 are very direct and often miss quicker route to the target.
    //1.4 and in fact the value rt2 seem to work better with a good balance of directness and
    //thouroughness to ensure 200*200 loopy mazes are solved in an optimal/nearly optimal way
    //every time.
    double weight = 1.4;

    if (rd.openNodes.size() == 0)
     return null;

    Node node = rd.openNodes.get(0);
    for (Node n : rd.openNodes)
      if (n.g + weight*n.h < node.g + weight*node.h)
        node = n;

    return node;
  }

  private int aStarSearch (IRobot robot) {
    //Get the next node in openNodes with the lowest F value to be the next
    //node to search.
    Node target = lowestFNode();

    //If already at target node, then perform aStarSearch calculations to add the neighbouring nodes
    //and assign g values and parents nodes where appropriate eg. when the neighbour is faster reached
    //from this node rather than the node it was found from before or if this is the first time
    //finding it.
    if (target.Equals(robot.getLocation().x, robot.getLocation().y)) {
      //Make sure this node isnt chosen again when it shouldnt be
      rd.closedNodes.add (target);
      rd.openNodes.remove(target);

      //List all possible nodes from this tile
      Node[] options = new Node[] { rd.allNodes[robot.getLocation().x][robot.getLocation().y-1],
                                    rd.allNodes[robot.getLocation().x+1][robot.getLocation().y],
                                    rd.allNodes[robot.getLocation().x][robot.getLocation().y+1],
                                    rd.allNodes[robot.getLocation().x-1][robot.getLocation().y]
                                  };

      //Remove nodes which are walls
      for (int x = 0; x < 4; x++)
        if (lookHeading (robot, IRobot.NORTH + x) == IRobot.WALL || rd.closedNodes.contains (options[x]))
          options[x] = null;

      //For each node neighbouring this one, that isnt a wall or has been reached before:
      //  Set g values if this is a better route to them
      //  Add to the openNodes list if they are newly discovered
      //  If the node is the target tile, use the linked list of parents to recreate the fastest path then end run
      for(int x = 0; x < 4; x++) {
        if (options[x] != null) {
          int tempG = target.g + 1;
          if (tempG < options[x].g) {
            options[x].parent = target;
            options[x].g = tempG;
            if (rd.openNodes.contains(options[x]) == false)
              rd.openNodes.add (options[x]);
          }
          if (options[x].Equals(robot.getTargetLocation().x, robot.getTargetLocation().y)) {
            rd.createPath(options[x]);
            return getHeadingToNeighbour(robot, options[x]);
          }
        }
      }
    }
    //If the current node is not the next lowest, the robot uses the findPathToNext() method
    //to travel to the next node using tiles it has already explored. Not the optimal route but
    //it will always find its way there.


    //Start making way to next lowest node
    return findPathToNext (robot, lowestFNode(), null);
  }

  private int findPathToNext (IRobot robot, Node target, List<Node> path) {
    //Recursively create a temporary path from the target node back until either:
    //  The path reaches the robots current position, then travel in the direction of the target.
    //  Or the path reaches the start node, then:
    //    If robot is currently one tile away from the path, move onto it
    //    Else, move to the current tiles parent until eventually 1 tile away from the path

    //The two paths between the target and the current position will ALWAYS converge as they both
    //end at the start node. So worst case scenario, the robot travels all the way back to the start,
    //and then follows the path it originally took to get to the target node the first time.

    //If first iteration, define an empty list
    if (path == null)
      path = new ArrayList<> ();

    //Add the current target node to the list as is the next part of the path back to the start
    path.add(target);

    //If target is the first node then start backtracking from current until on the path
    if (target.parent == null) {
      Node current = rd.allNodes[robot.getLocation().x][robot.getLocation().y];

      //If can get onto the path from current, do that
      for (Node n : path) {
        int heading = getHeadingToNeighbour(robot, n);
        if (heading != 0)
          return heading;
      }

      //Else, find a path to the path, not necessarily the shortest
      //  By backtracking to the start until the path is 1 tile away.
      return getHeadingToNeighbour(robot, current.parent);
    }

    //If target tile is next to current tile, move onto target ==> DONE
    if (target.parent.Equals(robot.getLocation().x, robot.getLocation().y))
      return getHeadingToNeighbour(robot, target);

    //Recursively move onto the next parent node back to the start
    return findPathToNext (robot, target.parent, path);
  }

  private int getHeadingToNeighbour (IRobot robot, Node next) {
    //Find the heading which leads to the neighbour node given in params.
    Node[] options = new Node[] { rd.allNodes[robot.getLocation().x][robot.getLocation().y-1],
                                  rd.allNodes[robot.getLocation().x+1][robot.getLocation().y],
                                  rd.allNodes[robot.getLocation().x][robot.getLocation().y+1],
                                  rd.allNodes[robot.getLocation().x-1][robot.getLocation().y]
                                };
    for (int x = 0; x < 4; x++)
      if (options[x].Equals(next))
        return x + IRobot.NORTH;

    //Next node is not a neighbour
    return 0;
  }

  private int getHeadingToNeighbour (Node current, Node next) {
    //Find the heading which leads to the neighbour node given in params
    //from the current node given in params.
    Node[] options = new Node[] { rd.allNodes[current.x][current.y-1],
                                  rd.allNodes[current.x+1][current.y],
                                  rd.allNodes[current.x][current.y+1],
                                  rd.allNodes[current.x-1][current.y]
                                };
    for (int x = 0; x < 4; x++)
      if (options[x].Equals(next))
        return x + IRobot.NORTH;

    //Next node is not a neighbour
    return 0;
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

  class RobotData {
    List<Integer> path;
    Node[][] allNodes;
    List<Node> closedNodes;
    List<Node> openNodes;

    public RobotData (IRobot robot) {
      //Set default values and create empty lists.
      allNodes = new Node[401][401];
      path = new ArrayList<> ();
      openNodes = new ArrayList<> ();
      closedNodes = new ArrayList<> ();

      //Create the grid of nodes, and assign the heuristic values h as the distance between
      //the node and the target as the crow flies, ignoring any walls between the two.
      //Distance just calculated by pythagorus theorem.
      for (int x = 0; x < 401; x++)
        for (int y = 0; y < 401; y++)
          allNodes [x][y] = new Node (x, y, Math.sqrt(Math.pow(robot.getTargetLocation().x - x, 2) + Math.pow(robot.getTargetLocation().y - y, 2)));
    }

    public void createPath (Node finalNode) {
      //Go through the list of nodes on the path from target to start and create a list of
      //headings needed in order to follow that path.

      path.clear();

      do {
        path.add(getHeadingToNeighbour(finalNode.parent, finalNode));
        finalNode = finalNode.parent;
      } while (finalNode.parent != null);

      //Reverse the list as they are backwards.
      Collections.reverse(path);
    }
  }

  class Node {
    int x, y;
    Node parent;
    int g;
    double h;

    public Node (int posX, int posY, double nH) {
      x = posX;
      y = posY;
      g = (int) Double.POSITIVE_INFINITY;
      h = nH;
      parent = null;
    }

    //Used when checking the robots position in the maze corresponds to this node.
    public boolean Equals (int X, int Y) {
      return (X == x && Y == y);
    }

    //In this program, if this return true, the actual objects will also be identical
    //with the same memory address, however in the interest of extendibility and
    //good coding practices, I have used this method anyway which checks the position
    //values of the nodes are the same, not just whether or not the reference addresses are.
    public boolean Equals (Node n) {
      return (n.x == x && n.y == y);
    }
  }
}
