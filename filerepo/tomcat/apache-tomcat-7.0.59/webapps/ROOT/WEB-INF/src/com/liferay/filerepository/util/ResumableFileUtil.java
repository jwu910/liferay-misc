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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Alan Zhang
 */
public class ResumableFileUtil {

	public static ResumableFile addResumableFile(
		String fileName, String filePath, long chunkSize, int totalChunks,
		long totalSize) {

		ResumableFile resumableFile = new ResumableFile(
			fileName, filePath, chunkSize, totalChunks, totalSize);

		_resumableMap.put(filePath, resumableFile);

		return resumableFile;
	}

	public static void cleanTempFiles(String dirPath) {
		File tempDirectory = _getTempUploadFile(dirPath);

		FileUtil.deltree(tempDirectory);

		List<String> ancestorDirPaths = FileUtil.getAncestorDirPaths(
			tempDirectory);

		ancestorDirPaths.add(tempDirectory.getPath() + "/*");

		FileUtil.append(_replicateIncludeFile, ancestorDirPaths);
	}

	public static boolean deleteFile(String filePath, boolean replicate) {
		File file = FileUtil.getFile(filePath);

		boolean deleted = FileUtil.delete(file);

		if (deleted && replicate) {
			replicateFile(filePath);
		}

		return deleted;
	}

	public static void deleteResumableFile(ResumableFile resumableFile) {
		File file = _getTempUploadFile(resumableFile.getFilePath());

		FileUtil.delete(file);

		File tempFile = _getTempUploadFile(resumableFile.getTempFilePath());

		FileUtil.delete(tempFile);

		File objectFile = _getTempUploadFile(resumableFile.getObjectFilePath());

		FileUtil.delete(objectFile);

		_resumableMap.remove(resumableFile.getFilePath());
	}

	public static boolean finishUpload(ResumableFile resumableFile) {
		File tempFile = _getTempUploadFile(resumableFile.getTempFilePath());

		File file = _getTempUploadFile(resumableFile.getFilePath());

		boolean renamed = FileUtil.renameTo(tempFile, file);

		if (!renamed) {
			return renamed;
		}

		File resumableObjectFile = _getTempUploadFile(
			resumableFile.getObjectFilePath());

		FileUtil.delete(resumableObjectFile);

		return renamed;
	}

	public static synchronized ResumableFile getResumableFile(String filePath) {
		ResumableFile resumableFile = _resumableMap.get(filePath);

		if (resumableFile == null) {
			resumableFile = _getCompletedResumableFile(filePath);

			_resumableMap.put(filePath, resumableFile);
		}

		if (resumableFile == null) {
			resumableFile = _readResumableFileObject(filePath);

			_resumableMap.put(filePath, resumableFile);
		}

		return resumableFile;
	}

	public static boolean isValid(
		String fileName, long chunkSize, int totalChunks, long totalSize) {

		if (fileName.equals("") || (totalChunks <= 0) || (chunkSize <= 0) ||
			(totalSize <= 0)) {

			return false;
		}

		return true;
	}

	public static boolean replicateFile(String filePath) {
		List<String> ancestorDirPaths = FileUtil.getAncestorDirPaths(filePath);

		if (ancestorDirPaths.isEmpty()) {
			return false;
		}

		FileUtil.append(_replicateIncludeFile, ancestorDirPaths);

		return true;
	}

	public static void setEncryptFileNamesThread(
		EncryptFileNamesThread encryptFileNamesThread) {

		_encryptFileNamesThread = encryptFileNamesThread;
	}

	public static void setReplicateIncludeFile(String replicateIncludeFile) {
		_replicateIncludeFile = new File(replicateIncludeFile);
	}

	public static void updateResumableFile(
		ResumableFile resumableFile, String filePath, boolean replicate) {

		String resumableFilePath = resumableFile.getFilePath();

		File source = _getTempUploadFile(resumableFilePath);
		File destination = FileUtil.getFile(filePath);

		FileUtil.renameTo(source, destination);

		if (replicate) {
			replicateFile(filePath);
		}

		_resumableMap.remove(resumableFilePath);

		_encryptFileNamesThread.encryptFileName(destination);
	}

	public static ResumableFile uploadResumableFile(
			ResumableFile resumableFile, int chunkNumber,
			InputStream inputStream, long contentLength)
		throws Exception {

		long pos = (chunkNumber - 1) * resumableFile.getChunkSize();

		File tempFile = _getTempUploadFile(resumableFile.getTempFilePath());

		FileUtil.write(tempFile, pos, inputStream, contentLength);

		synchronized (resumableFile) {
			resumableFile.setChunkReceived(chunkNumber);

			_writeResumableFileObject(resumableFile);
		}

		return resumableFile;
	}

	private static ResumableFile _getCompletedResumableFile(String filePath) {
		ResumableFile resumableFile = null;

		try {
			File file = _getTempUploadFile(filePath);

			if (file.exists()) {
				resumableFile = new ResumableFile(file, filePath);
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e.getMessage());
			}
		}

		return resumableFile;
	}

	private static File _getTempUploadFile(String filePath) {
		return FileUtil.getFile(Constants.DIR_TEMP_UPLOAD + "/" + filePath);
	}

	private static ResumableFile _readResumableFileObject(String filePath) {
		ResumableFile resumableFile = null;

		String objectFilePath = filePath + ResumableFile.EXTENSION_RESUMABLE;

		try {
			File objectFile = _getTempUploadFile(objectFilePath);

			if (objectFile.exists()) {
				ObjectInputStream objectInputStream = new ObjectInputStream(
					new FileInputStream(objectFile));

				resumableFile = (ResumableFile)objectInputStream.readObject();

				objectInputStream.close();
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return resumableFile;
	}

	private static void _writeResumableFileObject(ResumableFile resumableFile)
		throws Exception {

		File resumableObjectFile = _getTempUploadFile(
			resumableFile.getObjectFilePath());

		ObjectOutputStream objectOutputStream = new ObjectOutputStream(
			new FileOutputStream(resumableObjectFile));

		objectOutputStream.writeObject(resumableFile);

		objectOutputStream.flush();
		objectOutputStream.close();
	}

	private static Log _log = LogFactory.getLog(ResumableFileUtil.class);

	private static EncryptFileNamesThread _encryptFileNamesThread;
	private static File _replicateIncludeFile;
	private static Map<String, ResumableFile> _resumableMap =
		Collections.synchronizedMap(new LRUMap<String, ResumableFile>(100));

}