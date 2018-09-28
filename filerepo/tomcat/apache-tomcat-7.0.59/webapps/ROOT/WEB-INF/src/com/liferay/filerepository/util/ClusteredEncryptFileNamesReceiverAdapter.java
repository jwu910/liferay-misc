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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

/**
 * @author Igor Beslic
 */
public class ClusteredEncryptFileNamesReceiverAdapter extends ReceiverAdapter {

	public ClusteredEncryptFileNamesReceiverAdapter(
			ClusteredEncryptFileNamesThread clusteredEncryptFileNamesThread)
		throws Exception {

		_clusteredEncryptFileNamesThread = clusteredEncryptFileNamesThread;
	}

	@Override
	public void receive(Message message) {
		ClusteredEncryptFileNamesDTO clusteredEncryptFileNamesDTO =
			(ClusteredEncryptFileNamesDTO)message.getObject();

		_clusteredEncryptFileNamesThread.setActiveFileNamesMap(
			clusteredEncryptFileNamesDTO.getActiveFileNamesMap());
		_clusteredEncryptFileNamesThread.setActiveKeysMap(
			clusteredEncryptFileNamesDTO.getActiveKeysMap());
		_clusteredEncryptFileNamesThread.setLastModified(
			clusteredEncryptFileNamesDTO.getLastUpdateTimeMillis());
		_clusteredEncryptFileNamesThread.setStaleFileNamesMap(
			clusteredEncryptFileNamesDTO.getStaleFileNamesMap());

		if (_log.isInfoEnabled()) {
			_log.info("File names updated by cluster");
		}
	}

	private static Log _log = LogFactory.getLog(
		ClusteredEncryptFileNamesReceiverAdapter.class);

	private ClusteredEncryptFileNamesThread _clusteredEncryptFileNamesThread;

}