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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Alan Zhang
 */
public class ServletResponseUtil {

	public static void write(
			HttpServletRequest request, HttpServletResponse response,
			InputStream inputStream)
		throws IOException {

		long startTime = System.currentTimeMillis();
		long totalBytes = 0;

		OutputStream outputStream = response.getOutputStream();

		try {
			response.setHeader("Cache-Control", "public");

			if (!response.isCommitted()) {
				byte[] bytes = new byte[_BUFFER_SIZE];

				int value = -1;

				while ((value = inputStream.read(bytes)) != -1) {
					outputStream.write(bytes, 0, value);

					if (_log.isInfoEnabled()) {
						totalBytes += _BUFFER_SIZE;

						long secondsElapsed =
							(System.currentTimeMillis() - startTime) / 1000;

						_log.info(
							request.getRemoteAddr() + " downloading at " +
								(totalBytes / secondsElapsed) + " bytes/sec");
					}
				}
			}
		}
		finally {
			try {
				if (outputStream != null) {
					outputStream.flush();
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}

			try {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}

			try {
				if (inputStream != null) {
					inputStream.close();
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}
		}
	}

	public static void write(HttpServletResponse response, String message)
		throws IOException {

		OutputStream outputStream = response.getOutputStream();

		try {
			response.setHeader("Cache-Control", "public");

			if (!response.isCommitted()) {
				outputStream.write(message.getBytes("UTF-8"));
			}
		}
		finally {
			try {
				if (outputStream != null) {
					outputStream.flush();
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}

			try {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}
		}
	}

	private static final int _BUFFER_SIZE = 8192;

	private static Log _log = LogFactory.getLog(ServletResponseUtil.class);

}