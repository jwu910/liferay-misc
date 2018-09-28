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

import com.liferay.filerepository.util.FileUtil;
import com.liferay.filerepository.util.ParamUtil;
import com.liferay.filerepository.util.ServletResponseUtil;
import com.liferay.filerepository.util.TokenUtil;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Amos Fong
 */
public class TokenServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String remoteAddr = request.getRemoteAddr();

		if (!_isValidIP(remoteAddr)) {
			ServletResponseUtil.write(response, "Invalid IP " + remoteAddr);

			return;
		}

		String dirPath = ParamUtil.getString(request, "dirPath");

		if (!FileUtil.isValidPath(dirPath)) {
			ServletResponseUtil.write(response, "Invalid path " + dirPath);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + dirPath);
			}

			return;
		}

		String uploadToken = TokenUtil.createToken(dirPath);

		ServletResponseUtil.write(response, uploadToken);
	}

	@Override
	public void init(ServletConfig servletConfig) {
		String validIpsString = servletConfig.getInitParameter("validIps");

		_validIps = validIpsString.split(",");
	}

	private boolean _isValidIP(String remoteAddr) {
		for (String validIp : _validIps) {
			if (remoteAddr.equals(validIp) ||
				remoteAddr.startsWith(validIp + ".")) {

				return true;
			}
		}

		return false;
	}

	private static Log _log = LogFactory.getLog(TokenServlet.class);

	private String[] _validIps;

}