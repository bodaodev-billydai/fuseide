/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.fusesource.ide.server.karaf.ui;

import java.util.HashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IViewPart;
import org.eclipse.wst.server.core.IServer;
import org.fusesource.ide.server.karaf.core.Messages;
import org.fusesource.ide.server.karaf.core.server.IKarafServerDelegate;
import org.fusesource.ide.server.view.ITerminalConnectionListener;
import org.fusesource.ide.server.view.SshView;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IControllableServerBehavior;


/**
 * Thread used to ping server to test when it is started.
 */
public class SshConnector implements ITerminalConnectionListener {

	private static HashMap<IServer, SshConnector> connectors = new HashMap<IServer, SshConnector>();
	
	private int port;
	private String host;
	private String userName;
	private String passwd;
	private IServer server;
	private IControllableServerBehavior behaviorDelegate;
	private static final String TERMINAL_VIEW_LABEL = Messages.shellViewLabel;
	
	public SshConnector(IServer server) {
		super();
		this.server = server;
		this.behaviorDelegate = (IControllableServerBehavior)server.loadAdapter(IControllableServerBehavior.class, new NullProgressMonitor());
		IKarafServerDelegate config = (IKarafServerDelegate)server.loadAdapter(IKarafServerDelegate.class, new NullProgressMonitor());
		this.host = server.getHost();
		this.port = config.getPortNumber();
		this.userName = config.getUserName();
		this.passwd = config.getPassword();
	}
	/**
	 * creates/establishes a SSH connection with the provided connection data
	 * 
	 * @param server				the server object
	 * @param behaviorDelegate		the behaviour delegate
	 * @param host					the host name
	 * @param port					the port number
	 * @param user					the user name
	 * @param pass					the user password
	 */
	public SshConnector(IServer server, IControllableServerBehavior behaviorDelegate, String host, int port, String user, String pass) {
		super();
		this.server = server;
		this.behaviorDelegate = behaviorDelegate;
		this.host = host;
		this.port = port;
		this.userName = user;
		this.passwd = pass;
	}
	
	/**
	 * starts the ssh connection
	 */
	public void start() {
		// open the terminal view
		IViewPart vp = KarafUIPlugin.openTerminalView();
		if (vp == null || vp instanceof SshView == false) {
			KarafUIPlugin.getLogger().error("Unable to open the terminal view!");
			return;
		}
		
		// get the view
		final SshView connectorView = (SshView)vp;
		
		connectorView.setPartName(server.getName());
		
		// add a connection listener
		connectorView.addConnectionListener(this);
		
		// create the connection
		try {
			connectorView.createConnectionIfNotExists(host, port, userName, passwd);
		} catch (Exception ex) {
			KarafUIPlugin.getLogger().error("Unable to connect via SSH", ex);
		}
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.server.view.ITerminalConnectionListener#onConnect()
	 */
	public void onConnect() {
		// store the active connector
		connectors.put(server, this);
		// and open the terminal view
		KarafUIPlugin.openTerminalView().setFocus();
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.server.view.ITerminalConnectionListener#onDisconnect()
	 */
	public void onDisconnect() {
		// open the terminal view
		IViewPart vp = KarafUIPlugin.openTerminalView();
		if (vp == null || vp instanceof SshView == false) {
			KarafUIPlugin.getLogger().error("Unable to open the terminal view!");
			return;
		}
		
		// get the view
		final SshView connectorView = (SshView)vp;
		
		// add a connection listener
		connectorView.addConnectionListener(this);
		connectorView.setPartName(TERMINAL_VIEW_LABEL);

		connectorView.onTerminalDisconnect();
		
		// remove from the active connectors
		connectors.remove(server);
	}
	
	public static SshConnector getConnectorForServer(IServer server) {
		return connectors.get(server);
	}
}
