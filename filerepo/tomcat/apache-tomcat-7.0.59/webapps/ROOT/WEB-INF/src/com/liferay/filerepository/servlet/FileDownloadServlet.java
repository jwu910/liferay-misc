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

package com.liferay.filerepository.servlet;

import com.liferay.filerepository.util.ClusteredEncryptFileNamesThread;
import com.liferay.filerepository.util.EncryptFileNamesThread;
import com.liferay.filerepository.util.ResumableFileUtil;
import com.liferay.filerepository.util.ServletResponseUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.SocketException;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jets3t.service.model.S3Object;

/**
 * @author Alexander Chow
 * @author Brian Wing Shun Chan
 */
public class FileDownloadServlet extends HttpServlet {

	@Override
	public void destroy() {
		_encryptFileNamesThread.destroy();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		try {
			String key = request.getPathInfo();

			if (key.startsWith("/")) {
				key = key.substring(1);
			}

			File rootDir = _encryptFileNamesThread.getRootDir();

			if (rootDir != null) {
				File file = _encryptFileNamesThread.getFile(key);

				if (file != null) {
					sendFile(request, response, file);

					return;
				}
			}
			else {
				S3Object s3Object = _encryptFileNamesThread.getS3Object(key);

				InputStream is = null;
				int contentLength = 0;

				if (s3Object != null) {
					is = s3Object.getDataInputStream();
					contentLength = (int)s3Object.getContentLength();
				}

				if ((is != null) && (contentLength > 0)) {
					sendFile(request, response, s3Object, is, contentLength);

					return;
				}
			}
		}
		catch (Exception e) {
			if (e instanceof SocketException ||
				e.getClass().getName().equals(_CLIENT_ABORT_EXCEPTION)) {
			}
			else {
				_log.error(e, e);
			}
		}

		sendError(response, "Session has expired. Please try again.");
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String remoteAddr = request.getRemoteAddr();

		if (!isValidIP(remoteAddr)) {
			sendError(response, "Invalid IP " + remoteAddr);

			return;
		}

		String fileName = decodePath(request.getPathInfo());

		String key = _encryptFileNamesThread.getKey(fileName);

		if (key != null) {
			ServletResponseUtil.write(response, key);
		}
		else {
			sendError(response, "No key exists for " + fileName + ".");
		}
	}

	@Override
	public void init(ServletConfig servletConfig) {
		String validIpsString = servletConfig.getInitParameter("validIps");

		_validIps = validIpsString.split(",");

		try {
			String clusterName = servletConfig.getInitParameter("clusterName");

			if (clusterName != null) {
				_encryptFileNamesThread = new ClusteredEncryptFileNamesThread(
					servletConfig, clusterName);
			}
			else {
				_encryptFileNamesThread = new EncryptFileNamesThread(
					servletConfig);

				ResumableFileUtil.setEncryptFileNamesThread(
					_encryptFileNamesThread);
			}

			_encryptFileNamesThread.start();
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	protected String decodePath(String path) {
		if ((path == null) || (path.length() == 0)) {
			return path;
		}

		path = path.replace("/", "_TEMP_SLASH_");

		try {
			path = URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException uee) {
		}

		path = path.replace("_TEMP_SLASH_", "/");

		return path;
	}

	protected boolean isValidIP(String remoteAddr) {
		for (String validIp : _validIps) {
			if (remoteAddr.equals(validIp) ||
				remoteAddr.startsWith(validIp + ".")) {

				return true;
			}
		}

		return false;
	}

	protected void sendError(HttpServletResponse response, String message)
		throws IOException {

		if (_log.isInfoEnabled()) {
			_log.info(message);
		}

		ServletResponseUtil.write(response, message);
	}

	protected void sendFile(
			HttpServletRequest request, HttpServletResponse response, File file)
		throws IOException {

		long rangeStart = 0;
		long rangeEnd = file.length() - 1;

		String range = request.getHeader("Range");

		if ((range != null) && range.startsWith("bytes=")) {
			try {
				range = range.substring(6);

				String[] ranges = range.split("-");

				rangeStart = Long.parseLong(ranges[0]);

				if (ranges.length == 2) {
					rangeEnd = Long.parseLong(ranges[1]);
				}
			}
			catch (Exception e) {
			}
		}

		response.setContentLength((int)(rangeEnd - rangeStart + 1));
		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader(
			"Content-Disposition",
			"attachment; filename=\"" + file.getName() + "\"");

		if (rangeStart > 0) {
			StringBuilder sb = new StringBuilder();

			sb.append("bytes ");
			sb.append(rangeStart);
			sb.append("-");
			sb.append(rangeEnd);
			sb.append("/");
			sb.append(file.length());

			response.setHeader("Content-Range", sb.toString());
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		}

		FileInputStream fileInputStream = new FileInputStream(file);

		if (rangeStart > 0) {
			fileInputStream.skip(rangeStart);
		}

		if (_log.isInfoEnabled()) {
			_log.info("Sending file " + file.getName());
		}

		ServletResponseUtil.write(request, response, fileInputStream);
	}

	protected void sendFile(
			HttpServletRequest request, HttpServletResponse response,
			S3Object s3Object, InputStream is, int contentLength)
		throws IOException {

		String fileName = s3Object.getKey();

		int pos = fileName.lastIndexOf("/");

		fileName = fileName.substring(pos + 1);

		response.setContentLength(contentLength);
		response.setHeader(
			"Content-Disposition", "attachment; filename=\"" + fileName + "\"");

		if (_log.isInfoEnabled()) {
			_log.info("Sending file " + fileName);
		}

		ServletResponseUtil.write(request, response, is);
	}

	private static final String _CLIENT_ABORT_EXCEPTION =
		"org.apache.catalina.connector.ClientAbortException";

	private static Log _log = LogFactory.getLog(FileDownloadServlet.class);

	private EncryptFileNamesThread _encryptFileNamesThread;
	private String[] _validIps;

}