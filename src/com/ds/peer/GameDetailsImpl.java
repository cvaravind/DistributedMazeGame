package com.ds.peer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameDetailsImpl implements GameDetailsIntf, Serializable
{
	Map<String,Integer> treasuresMap;
	List<PlayerIntf> playerList;
	private int numOfAvailableTreasures;
	private int N;
	private int M;
	private String message;

	public GameDetailsImpl(String msg)
	{
		message = msg;
	}
	
	public GameDetailsImpl(int n, int m, List<PlayerIntf> playerlist)
	{
		N=n;
		M=m;
		setNumOfAvailableTreasures(M);
		message = null;
		this.treasuresMap = new HashMap<String,Integer>();
		this.playerList = new ArrayList<PlayerIntf>(playerlist);
		
		int count = 0;
		int tempRand;
		int avgNumOfTreasures = M/(getN()*getN()); 
//System.out.println("Avergae no of tr: "+avgNumOfTreasures);

		Random rand = new Random();
		int x,y;
		for(int i=0; i<getN(); i++)
			for(int j=0; j<getN(); j++)
			{
				if(doesPlayerExistsAt(i,j)==false)
				{
					tempRand = rand.nextInt(avgNumOfTreasures+1);
					treasuresMap.put(i+" "+j, tempRand);
					count = count + tempRand;
				}
				else
				{
					treasuresMap.put(i+" "+j, 0);
				}
			}
		
		if(avgNumOfTreasures==0) avgNumOfTreasures = 1;
		while(count<M)
		{
			int remaining = M - count;
			int randRem = rand.nextInt(  (avgNumOfTreasures<remaining)? avgNumOfTreasures+1 : remaining+1  );
			if(randRem>0 && randRem+count<=M)
			{
				x = rand.nextInt(getN());
				y = rand.nextInt(getN());
				if(doesPlayerExistsAt(x, y))
					continue;
				
				treasuresMap.put(x+" "+y, treasuresMap.get(x+" "+y) + randRem);
				count = count + randRem;
				
			}
			
		}
	}
	
	public int findMaxTreasures()
	{
		int max=-1;
		for(PlayerIntf player : playerList)
		{
			if(player.getNumOfTreasures()>max)
			{
				max = player.getNumOfTreasures();
			}
		}
		
		return max;
	}
	
	private boolean doesPlayerExistsAt(int x, int y)
	{
		for(PlayerIntf p : playerList)
		{
			if(p.getLocationX()==x && p.getLocationY()==y)
				return true;
		}
		return false;
	}

	public Map<String, Integer> getTreasuresMap() {
		return treasuresMap;
	}

	private void setTreasuresMap(Map<String, Integer> treasuresMap) {
		this.treasuresMap = treasuresMap;
	}

	public List<PlayerIntf> getPlayerList() {
		return playerList;
	}

	private void setPlayerList(List<PlayerIntf> playerList) {
		this.playerList = playerList;
	}

	public int getM() {
		return M;
	}

	private void setM(int m) {
		M = m;
	}

	public int getN() {
		return N;
	}

	private void setN(int n) {
		N = n;
	}

	public int getNumOfAvailableTreasures() {
		return numOfAvailableTreasures;
	}

	public void setNumOfAvailableTreasures(int numOfAvailableTreasures) {
		this.numOfAvailableTreasures = numOfAvailableTreasures;
	}
	
	public String getMessage() {
		return message;
	}
}
