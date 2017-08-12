package com.ds.peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerPlayerIntf extends Remote
{
	void startGameNotify(int pId,GameDetailsIntf intf, GameIntf backupServerStub) throws RemoteException;
	void ping() throws RemoteException;
	GameIntf informPeerB(GameImpl gameObj) throws RemoteException;
	void broadcastNewB(GameIntf st) throws RemoteException;
	void broadcastDiedStub(GameIntf st) throws RemoteException;
	void gameOverNotify(int winnerPId) throws RemoteException;
}
