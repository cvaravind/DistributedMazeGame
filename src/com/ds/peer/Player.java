package com.ds.peer;

import java.io.Serializable;

public class Player implements Serializable,PlayerIntf
{
	private int pId;
	private int numOfTreasures;
	private int locationX;
	private int locationY;
	
	public Player(int  pid,int num,int locx,int locy)
	{
		this.pId = pid;
		this.numOfTreasures = num;
		this.locationX = locx;
		this.locationY = locy;
	}

	public int getLocationX() 
	{
		return locationX;
	}

	public void setLocationX(int locationX) {
		this.locationX = locationX;
	}

	public void setLocationY(int locationY) {
		this.locationY = locationY;
	}

	public int getNumOfTreasures() {
		return numOfTreasures;
	}

	public void setNumOfTreasures(int numOfTreasures) {
		this.numOfTreasures = numOfTreasures;
	}

	public int getLocationY() 
	{
		return locationY;
	}

	public int getPId() 
	{
		return pId;
	}
}
