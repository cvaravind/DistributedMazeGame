package com.ds.peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameIntf extends Remote
{
	//enum Messages {GAME_ACCEPTED,GAME_STARTED,PLAYERS_EXCEEDED}
	ContainerIntf joinGame(PeerPlayerIntf obj) throws RemoteException;
	ContainerIntf move(int pid, int direction) throws RemoteException;
	void informBStubToPrimary(GameIntf stub) throws RemoteException;
	void ping() throws RemoteException;
	void UpdateGameStateToB(GameImpl gameState, boolean ft) throws RemoteException;
	void updateReadinesstoP() throws RemoteException;
}
