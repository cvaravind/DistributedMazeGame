package com.ds.peer;

interface Commons 
{
	
	public enum Messages 
	{
		GAME_STARTED("Game has already started"),PLAYERS_EXCEEDED("Num of Players has exceeded")
		,GAME_ACCEPTED("Please wait for others to connect")
		,BACKUP("You are backup server")
		,MOVE_NOT_ALLOWED("Move not allowed")
		,NO_MOVE("Updated Game State sent")
		,INVALID_MOVE("Invalid Move!")
		,CELL_OCCUPIED("Cell is already occupied!")
		,VALID_MOVE("Valid move");
		private String value;
		private Messages(String val)
		{
			value = val;
		}
	}
	
	
	enum ServerType {PRIMARY,BACKUP,PLAYER}
	
	int PORT1 = 1234;
	int PORT2 = 1235;
}
