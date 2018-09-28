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

import java.io.File;
import java.io.Serializable;

import java.util.BitSet;

import org.json.JSONObject;

/**
 * @author Alan Zhang
 */
public class ResumableFile implements Serializable {

	public static String EXTENSION_RESUMABLE = ".resumable";

	public static String EXTENSION_TEMP = ".tmp";

	public ResumableFile(File file, String filePath) {
		_complete = true;
		_fileName = file.getName();
		_filePath = filePath;
		_totalSize = file.length();
	}

	public ResumableFile(
		String fileName, String filePath, long chunkSize, int totalChunks,
		long totalSize) {

		_chunksReceived = new BitSet(_totalChunks);
		_chunkSize = chunkSize;
		_fileName = fileName;
		_filePath = filePath;
		_totalChunks = totalChunks;
		_totalSize = totalSize;
	}

	public long getChunkSize() {
		return _chunkSize;
	}

	public String getFileName() {
		return _fileName;
	}

	public String getFilePath() {
		return _filePath;
	}

	public String getObjectFilePath() {
		return getFilePath() + EXTENSION_RESUMABLE;
	}

	public String getTempFilePath() {
		return getFilePath() + EXTENSION_TEMP;
	}

	public long getTotalSize() {
		return _totalSize;
	}

	public synchronized boolean hasChunkReceived(int chunkNumber) {
		return _chunksReceived.get(chunkNumber);
	}

	public synchronized boolean isComplete() {
		if (_complete) {
			return true;
		}

		if (_chunksReceived.cardinality() == _totalChunks) {
			_complete = true;

			return true;
		}
		else {
			return false;
		}
	}

	public synchronized void setChunkReceived(int chunkNumber) {
		_chunksReceived.set(chunkNumber);
	}

	public synchronized JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();

		try {
			jsonObject.put("fileName", getFileName());
			jsonObject.put("filePath", getFilePath());
			jsonObject.put("fileSize", getTotalSize());
		}
		catch (Exception e) {
		}

		return jsonObject;
	}

	private static final long serialVersionUID = 1L;

	private long _chunkSize;
	private BitSet _chunksReceived;
	private volatile boolean _complete;
	private String _fileName;
	private String _filePath;
	private int _totalChunks;
	private long _totalSize;

}