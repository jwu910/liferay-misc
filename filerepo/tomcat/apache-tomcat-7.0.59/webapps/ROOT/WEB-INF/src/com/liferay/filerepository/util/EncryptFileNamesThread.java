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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

/**
 * @author Alexander Chow
 * @author Brian Wing Shun Chan
 */
public class EncryptFileNamesThread extends Thread {

	public EncryptFileNamesThread(ServletConfig servletConfig)
		throws Exception {

		String sleepInterval = servletConfig.getInitParameter("sleepInterval");

		if (sleepInterval != null) {
			_sleepInterval = Long.parseLong(sleepInterval);
		}
		else {
			_sleepInterval = 1000 * 60 * 5;
		}

		String rootDir = servletConfig.getInitParameter("rootDir");

		if (rootDir != null) {
			_rootDir = new File(rootDir);
		}
		else {
			_s3AccessKey = servletConfig.getInitParameter("s3AccessKey");
			_s3SecretKey = servletConfig.getInitParameter("s3SecretKey");
			_s3BucketName = servletConfig.getInitParameter("s3BucketName");
			_s3Prefix = servletConfig.getInitParameter("s3Prefix");

			AWSCredentials awsCredentials = new AWSCredentials(
				_s3AccessKey, _s3SecretKey);

			_s3Service = new RestS3Service(awsCredentials);

			_s3Bucket = _s3Service.createBucket(_s3BucketName);
		}
	}

	@Override
	public void destroy() {
		_destroyed = true;
	}

	public void encryptFileName(File file) {
		String key = generateKey();

		String fileName = file.toString();

		fileName = fileName.substring(_rootDir.toString().length());
		fileName = fileName.replace('\\', '/');

		activeFileNamesMap.put(key, fileName);
		activeKeysMap.put(fileName, key);
	}

	public File getFile(String key) {
		String fileName = getFileName(key);

		if (fileName != null) {
			return new File(_rootDir + "/" + fileName);
		}
		else {
			return null;
		}
	}

	public String getFileName(String key) {
		String fileName = null;

		if (activeFileNamesMap != null) {
			fileName = activeFileNamesMap.get(key);
		}

		if ((fileName == null) && (staleFileNamesMap != null)) {
			fileName = staleFileNamesMap.get(key);
		}

		return fileName;
	}

	public String getKey(String fileName) {
		return activeKeysMap.get(fileName);
	}

	public File getRootDir() {
		return _rootDir;
	}

	public S3Object getS3Object(String key) throws Exception {
		String fileName = getFileName(key);

		if (fileName != null) {
			return _s3Service.getObject(_s3Bucket, _s3Prefix + fileName);
		}
		else {
			return null;
		}
	}

	public long getSleepInterval() {
		return _sleepInterval;
	}

	@Override
	public void run() {
		while (true) {
			if (_destroyed) {
				return;
			}

			try {
				encryptFileNames();
			}
			catch (Exception e) {
				_log.error(e, e);
			}

			if (_destroyed) {
				return;
			}

			try {
				sleep(getSleepInterval());
			}
			catch (InterruptedException ie) {
				if (_log.isWarnEnabled()) {
					_log.warn(ie, ie);
				}
			}
		}
	}

	protected void encryptFileNames() throws Exception {
		Map<String, String> fileNamesMap = new HashMap<String, String>();
		Map<String, String> keysMap = new HashMap<String, String>();

		if (_rootDir != null) {
			encryptFileNames(_rootDir, fileNamesMap, keysMap);
		}
		else {
			S3Object[] s3Objects = _s3Service.listObjects(
				_s3Bucket, _s3Prefix, null);

			for (S3Object s3Object : s3Objects) {
				if (_destroyed) {
					return;
				}

				String fileName = s3Object.getKey();

				if (!fileName.endsWith("_$folder$")) {
					fileName = fileName.substring(_s3Prefix.length());

					String key = generateKey();

					fileNamesMap.put(key, fileName);
					keysMap.put(fileName, key);
				}
			}
		}

		staleFileNamesMap = activeFileNamesMap;

		activeFileNamesMap = fileNamesMap;
		activeKeysMap = keysMap;

		if (_log.isInfoEnabled()) {
			_log.info("Finished encrypting " + fileNamesMap.size() + " files");
		}
	}

	protected void encryptFileNames(
		File dir, Map<String, String> fileNamesMap,
		Map<String, String> keysMap) {

		if (!dir.exists()) {
			_log.error("Directory " + dir + " does not exist");
		}

		for (File file : dir.listFiles()) {
			if (_destroyed) {
				return;
			}

			if (file.isDirectory()) {
				File tempUploadDir = FileUtil.getFile(
					Constants.DIR_TEMP_UPLOAD);

				if (!file.equals(tempUploadDir)) {
					encryptFileNames(file, fileNamesMap, keysMap);
				}
			}
			else {
				String fileName = file.toString();

				fileName = fileName.substring(_rootDir.toString().length());
				fileName = fileName.replace('\\', '/');

				String key = generateKey();

				fileNamesMap.put(key, fileName);
				keysMap.put(fileName, key);
			}
		}
	}

	protected String generateKey() {
		long number = Math.abs(_random.nextLong());

		return Long.toString(number, Character.MAX_RADIX - 3).toUpperCase();
	}

	protected Map<String, String> activeFileNamesMap;
	protected Map<String, String> activeKeysMap;
	protected Map<String, String> staleFileNamesMap;

	private static Log _log = LogFactory.getLog(EncryptFileNamesThread.class);

	private boolean _destroyed;
	private Random _random = new Random(System.currentTimeMillis());
	private File _rootDir;
	private String _s3AccessKey;
	private S3Bucket _s3Bucket;
	private String _s3BucketName;
	private String _s3Prefix;
	private String _s3SecretKey;
	private S3Service _s3Service;
	private long _sleepInterval;

}