/*
* File: DumboController.java
* Created: 17 September 2002, 00:34
* Author: Stephen Jarvis
*/

import uk.ac.warwick.dcs.maze.logic.IRobot;

public class DumboController
{

	public void controlRobot(IRobot robot) {

		int randno;
		int direction;

		// Select a random number

		randno = (int) Math.round(Math.random()*3);

		// Convert this to a direction

		if (randno == 0)
		direction = IRobot.LEFT;
		else if (randno == 1)
		direction = IRobot.RIGHT;
		else if (randno == 2)
		direction = IRobot.BEHIND;
		else 
		direction = IRobot.AHEAD;

		robot.face(direction); /* Face the robot in this direction */ 
	}

}