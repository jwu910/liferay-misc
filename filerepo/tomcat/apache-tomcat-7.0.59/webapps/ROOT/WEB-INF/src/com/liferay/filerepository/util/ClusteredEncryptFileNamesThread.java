/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.filerepository.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.servlet.ServletConfig;

import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.locking.LockService;

/**
 * @author Igor Beslic
 */
public class ClusteredEncryptFileNamesThread extends EncryptFileNamesThread {

	public ClusteredEncryptFileNamesThread(
			ServletConfig servletConfig, String clusterName)
		throws Exception {

		super(servletConfig);

		_channel = new JChannel("META-INF/jgroups-config.xml");

		_channel.setDiscardOwnMessages(true);
		_channel.setReceiver(
			new ClusteredEncryptFileNamesReceiverAdapter(this));

		_lockService = new LockService((JChannel)_channel);

		_lock = _lockService.getLock(clusterName + "-lock");

		_channel.connect(clusterName);
	}

	@Override
	public void destroy() {
		_channel.disconnect();

		_channel.close();

		super.destroy();
	}

	@Override
	protected void encryptFileNames() throws Exception {
		if ((System.currentTimeMillis() - _lastModified) < getSleepInterval()) {
			return;
		}

		if (_lock.tryLock(2000, TimeUnit.MILLISECONDS)) {
			super.encryptFileNames();

			_lastModified = System.currentTimeMillis();

			ClusteredEncryptFileNamesDTO clusteredEncryptFileNamesDTO =
				new ClusteredEncryptFileNamesDTO(
					activeFileNamesMap, activeKeysMap, _lastModified,
					staleFileNamesMap);

			Message message = new Message(
				null, null, clusteredEncryptFileNamesDTO);

			_channel.send(message);

			_lock.unlock();
		}
	}

	protected void setActiveFileNamesMap(
		Map<String, String> activeFileNamesMap) {

		this.activeFileNamesMap = activeFileNamesMap;
	}

	protected void setActiveKeysMap(Map<String, String> activeKeysMap) {
		this.activeKeysMap = activeKeysMap;
	}

	protected void setLastModified(long lastModified) {
		_lastModified = lastModified;
	}

	protected void setStaleFileNamesMap(Map<String, String> staleFileNamesMap) {
		this.staleFileNamesMap = staleFileNamesMap;
	}

	private Channel _channel;
	private long _lastModified;
	private Lock _lock;
	private LockService _lockService;

}