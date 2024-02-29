/*
	Ex2.java

	PREAMBLE
	This program now has a tendancy to continue down straight corridors if possible
	by checking if the tile ahead is a wall or not using the robot.look() function and changing its
	behaviour based on this. However it does not always change its behaviour to only moving forward
	as there is still a random integer generated in the conditional statement which 1/8 of
	the time is equal to 0 at which point a random direction is generated anyway.

	This system does reach the end of the maze if given enough time but is still
	not very efficient, although from some very limited testing appears to be faster than
	the previous iteration. This is because the robot still wastes a lot of time turning around
	and making turns that it has taken before and only found deadends in, but it does spend less
	time jigging forwards-backwards-forwards-backwards-etc in straight corridors due to the 7/8 chance
	to keep going straight if possible. A better way to do this could be to decide when to go
	straight forward based on the numbers of walls surrounding the robot as used in the logging system;
	as junctions in the maze are the only times not just following the corridor is required.

	The initial part of this exercise which ALWAYS went straight if possible did not always reach the
	end of the maze as it got stuck in situations where there were two deadends in-line with each other
	and to escape the robot had to take a turn at a 3/4-way junction, which it never did as it could always
	just keep going straight on. Excluding 3-way junctions as such:

																	|		|
													________|		|
													________	>	|
																	|		|
																	|		|

	Bias of "randno = (int) Math.round(Math.random()*3);" due to uneven probabilities:
	Math.random() returns number between 0.0 and 1.0 with even probability
	Math.round() rounds a value to the nearest integer
		==> 1.2 -> 1
		==> 1.7 -> 2
		==> 0.4 -> 0
	There are 4 possible outcomes from the line as a whole
		0, 1, 2, 3
	To reach each there is a range of values that could be chosen by 3*random()
		0 <- 0.0-0.5
		1 <- 0.5-1.5
		2 <- 1.5-2.5
		3 <- 2.5-3.0
	The ranges for 0 and 3 are half the size of the ranges for 1 and 2 and so
	appear half as frequently:
		P(0) = 16.7%
		P(1) = 33.3%
		P(2) = 33.3%
		P(3) = 16.7%

	Using "randno = (int) Math.floor(Math.random()*4);" to choose directions with equal probabilities:
		Math.random() returns number between 0.0 and 1.0 with even probability
		Math.floor() rounds a value down to an integer
			==> 1.2 -> 1
			==> 1.7 -> 1
			==> 0.4 -> 0
		There are 3 possible outcomes from the line as a whole
			0, 1, 2, 3
		To reach each there is a range of values that could be chosen by 4*random()
			0 <- 0.0-1.0
			1 <- 1.0-2.0
			2 <- 2.0-3.0
			3 <- 3.0-4.0
		The ranges for all outcomes are equally large so they all appear with equal probabilities:
			P(0) = 25%
			P(1) = 25%
			P(2) = 25%
			P(3) = 25%
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class Ex2
{

	public void controlRobot(IRobot robot) {
		//Deal with choosing a direction to move
		int randno;
		int direction;

		//Check if the tile ahead is a wall OR if a new random number between [0-7] is 0
		if (robot.look(IRobot.AHEAD) == IRobot.WALL || (int) Math.floor(Math.random()*8) == 0) {
			//Randomise the direction of the robot
			do {
				// Select a random number
				randno = (int) Math.floor(Math.random()*4);

				// Convert this to a direction
				if (randno == 0) direction = IRobot.LEFT;
				else if (randno == 1) direction = IRobot.RIGHT;
				else if (randno == 2) direction = IRobot.BEHIND;
				else direction = IRobot.AHEAD;

			//Loops if the robot is facing a wall to choose another direction instead
			} while (robot.look(direction) == IRobot.WALL);
		} else
			//Keep moving forwards
			direction = IRobot.AHEAD;

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
