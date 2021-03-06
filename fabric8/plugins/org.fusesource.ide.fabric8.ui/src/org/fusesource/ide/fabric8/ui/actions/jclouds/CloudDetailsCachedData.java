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

package org.fusesource.ide.fabric8.ui.actions.jclouds;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.internal.expressions.util.LRUCache;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.fusesource.ide.commons.jobs.Jobs;
import org.fusesource.ide.commons.jobs.LoadListJobSupport;
import org.fusesource.ide.fabric8.ui.FabricPlugin;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.domain.Location;

public class CloudDetailsCachedData {
	protected static LRUCache cache = new LRUCache(5);

	private final CloudDetails details;
	private final WritableList hardwareList = WritableList.withElementType(Hardware.class);
	private final WritableList locationList = WritableList.withElementType(Location.class);
	private final WritableList nodePropertyList = WritableList.withElementType(ComputeMetadata.class);
	
	private LinkedList<OsFamily> familiesList = new LinkedList<OsFamily>();
	private LinkedList<String>   versionsList = new LinkedList<String>();

	private ComputeService computeClient;

	private CloudConnectJob createClientJob;
	private LoadListJobSupport locationsJob;
	private LoadListJobSupport hardwareJob;
	private CloudConnectJob reloadJob;
	
	public static synchronized CloudDetailsCachedData getInstance(CloudDetails selectedCloud) {
		CloudDetailsCachedData answer = null;
		if (selectedCloud == null) {
			answer = new CloudDetailsCachedData(selectedCloud);
		} else {
			Object key = selectedCloud.getCacheKey();
			answer = (CloudDetailsCachedData) cache.get(key);
			if (answer == null) {
				answer = new CloudDetailsCachedData(selectedCloud);
				cache.put(key, answer);
			}
		}
		return answer;
	}

	public CloudDetailsCachedData(CloudDetails details) {
		this.details = details;
	}

	public CloudDetails getDetails() {
		return details;
	}

	public WritableList getHardwareList() {
		return hardwareList;
	}

	public WritableList getLocationList() {
		return locationList;
	}

	public synchronized List getOsFamilyList() {
		if (familiesList.isEmpty()) {
			for (OsFamily fam : OsFamily.values()) {
				familiesList.add(fam);
			}
		}
		return familiesList;
	}

	public synchronized List getOsVersionList() {
		if (versionsList.isEmpty()) {
			versionsList.add("");
			for (int i = 1; i<20; i++) {
				versionsList.add("" + i);
			}	
		}
		return versionsList;
	}

	public WritableList getNodePropertyList() {
		return nodePropertyList;
	}

	public void startLoadingDataJobs() {
		if (details == null) { // maybe we have cached data we can reuse
			return;
		}
		cancelLoadingJobs();

		locationsJob = new LoadListJobSupport("Loading cloud locations", locationList) {

			@Override
			protected List<?> loadList() {
				if (computeClient == null) return Collections.EMPTY_LIST;
				return JClouds.sortedLocationList(computeClient.listAssignableLocations());
			}

		};
		
		hardwareJob = new LoadListJobSupport("Loading cloud hardware", hardwareList) {

			@Override
			protected List<?> loadList() {
				if (computeClient == null) return Collections.EMPTY_LIST;
				return JClouds.sortedList(computeClient.listHardwareProfiles());
			}

		};

		createClientJob = new CloudConnectJob(this, locationsJob, hardwareJob);
		Jobs.schedule(createClientJob);
	}

	public void cancelLoadingJobs() {
		Jobs.cancel(createClientJob, locationsJob, hardwareJob);
		createClientJob = null;
		locationsJob = null;
		hardwareJob = null;
	}
	
	public ComputeService getComputeClient() {
		return computeClient;
	}

	public void setComputeClient(ComputeService computeClient) {
		this.computeClient = computeClient;
	}

//	public ComputeService createComputeClient(IZKClient izkClient) {
//		details.setZooKeeper(izkClient);
//		return CloudDetails.createComputeService(details);
//	}


	public void reloadNodes() {
		getReloadJob().schedule();
		// not a user job on startup?
		// Jobs.schedule(getReloadJob());
	}

	protected CloudConnectJob getReloadJob() {
		if (reloadJob != null){
			reloadJob.cancel();
		}
		reloadJob = new CloudConnectJob(this) {

			@Override
			protected void onConnected(ComputeService client, IProgressMonitor monitor) {
				super.onConnected(client, monitor);

				setName("Loading nodes");
				monitor.setTaskName("Loading nodes");
				final Set<? extends ComputeMetadata> listNodes = client.listNodes();
				FabricPlugin.getLogger().debug("==============  Loaded " + listNodes.size() + " nodes!");
				Display.getDefault().asyncExec(new Runnable(){

					@Override
					public void run() {
						nodePropertyList.clear();
						nodePropertyList.addAll(listNodes);
						FabricPlugin.getLogger().debug("==============  Added " + listNodes.size() + " nodes!");
					}});
			}
		};
		return reloadJob;
	}

}
