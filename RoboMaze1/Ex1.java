/*
	Ex1.java

	PREAMBLE
	This program successfully avoids any collisions with walls by generating a
	random direction then checking whether in that direction is a wall; if so,
	a different direction is chosen by looping back to choose a different random
	number until a direction is chosen that is possible to move into.

	The majority of the code is present to deal with logging the robots movements
	and displaying it in a way easy to read to humans for debugging and testing
	purposes, this has been split off into its own procedure to make the
	functional part of the code more legible. The funcional method of the code is
	concise but not very efficient as impossible directions are often chosen even
	after they have already been seen to be impossible and the robot often backtracks in the maze.
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex1 {
	public void controlRobot(IRobot robot) {
		//Deal with choosing a direction to move
		int randno;
		int direction;

		do {
			// Select a random number
			randno = (int) Math.round(Math.random()*3);

			// Convert this to a direction
			if (randno == 0) direction = IRobot.LEFT;
			else if (randno == 1) direction = IRobot.RIGHT;
			else if (randno == 2) direction = IRobot.BEHIND;
			else direction = IRobot.AHEAD;

		//Loops if the robot is facing a wall to choose another direction instead
		} while (robot.look(direction) == IRobot.WALL);

		//Turn to this direction
		robot.face(direction);
		LogTurn(robot, direction);
	}

	//Deal with logging the movements
	private void LogTurn (IRobot robot, int direction) {
		int walls = 0;
		String message = "I'm going ";

		//For each of the 4 directions, if there is a wall, increment walls
		for (int dir : new int[] {IRobot.LEFT, IRobot.RIGHT, IRobot.BEHIND, IRobot.AHEAD})
			walls += robot.look(dir)==IRobot.WALL?1:0;

		//Add the direction to the log message string
		switch (direction){
			case IRobot.LEFT: message += "left";
				break;
			case IRobot.RIGHT: 	message += "right";
				break;
			case IRobot.BEHIND: message += "backwards";
				break;
			case IRobot.AHEAD: message += "forward";
				break;
		}

		//Add the current tiles type based on how many walls surround it
		switch (walls){
				case 3: message += " at a deadend";
					break;
				case 2: message += " down a corridor";
					break;
				default: message += " at a junction";
					break;
		}

		//Print the message to the log
		System.out.println(message);
	}
}
