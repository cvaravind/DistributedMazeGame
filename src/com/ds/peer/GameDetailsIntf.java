package com.ds.peer;

import java.util.List;
import java.util.Map;

public interface GameDetailsIntf 
{

	int getN();

	int getM();

	Map<String, Integer> getTreasuresMap();

	List<PlayerIntf> getPlayerList();

	int getNumOfAvailableTreasures();
	
	String getMessage();


	
	
}
