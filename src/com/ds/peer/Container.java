package com.ds.peer;

import java.io.Serializable;

public class Container implements ContainerIntf, Serializable
{
	String message;
	GameImpl gameObj=null;
	GameDetailsIntf gameDetailsObj = null;
	
	public GameDetailsIntf getGameDetailsObj() {
		return gameDetailsObj;
	}
	public void setGameDetailsObj(GameDetailsIntf gameDetailsObj) {
		this.gameDetailsObj = gameDetailsObj;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public GameImpl getGameObj() {
		return gameObj;
	}
	public void setGameObj(GameImpl gameObj) {
		this.gameObj = gameObj;
	}
	
	
}
