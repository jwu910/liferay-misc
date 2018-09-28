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

import java.io.Serializable;

import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ClusteredEncryptFileNamesDTO implements Serializable {

	public ClusteredEncryptFileNamesDTO(
		Map<String, String> activeFileNamesMap,
		Map<String, String> activeKeysMap, long lastModified,
		Map<String, String> staleFileNamesMap) {

		_activeKeysMap = activeKeysMap;
		_activeFileNamesMap = activeFileNamesMap;
		_lastUpdateTimeMillis = lastModified;
		_staleFileNamesMap = activeKeysMap;
	}

	public Map<String, String> getActiveFileNamesMap() {
		return _activeFileNamesMap;
	}

	public Map<String, String> getActiveKeysMap() {
		return _activeKeysMap;
	}

	public long getLastUpdateTimeMillis() {
		return _lastUpdateTimeMillis;
	}

	public Map<String, String> getStaleFileNamesMap() {
		return _staleFileNamesMap;
	}

	private Map<String, String> _activeFileNamesMap;
	private Map<String, String> _activeKeysMap;
	private long _lastUpdateTimeMillis;
	private Map<String, String> _staleFileNamesMap;

}