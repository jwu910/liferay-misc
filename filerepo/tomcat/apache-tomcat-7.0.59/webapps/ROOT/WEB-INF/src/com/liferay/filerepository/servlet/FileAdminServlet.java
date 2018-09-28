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

import com.liferay.filerepository.util.Constants;
import com.liferay.filerepository.util.FileUtil;
import com.liferay.filerepository.util.ParamUtil;
import com.liferay.filerepository.util.ResponseMessages;
import com.liferay.filerepository.util.ResumableFile;
import com.liferay.filerepository.util.ResumableFileUtil;
import com.liferay.filerepository.util.ServletResponseUtil;

import java.io.File;
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
public class FileAdminServlet extends HttpServlet {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String remoteAddr = request.getRemoteAddr();

		if (!_isValidIP(remoteAddr)) {
			ServletResponseUtil.write(response, "Invalid IP " + remoteAddr);

			if (_log.isDebugEnabled()) {
				_log.debug("Invalid IP " + remoteAddr);
			}

			return;
		}

		String cmd = ParamUtil.getString(request, Constants.CMD);

		try {
			if (cmd.equals(Constants.ACTION_CLEAN)) {
				cleanTempFiles(request, response);
			}
			else if (cmd.equals(Constants.ACTION_DELETE)) {
				deleteFile(request, response);
			}
			else if (cmd.equals(Constants.ACTION_RENAME)) {
				renameFile(request, response);
			}
			else if (cmd.equals(Constants.ACTION_REPLICATE)) {
				replicateFile(request, response);
			}
			else if (cmd.equals(Constants.ACTION_UPDATE)) {
				updateResumableFile(request, response);
			}
		}
		catch (Exception e) {
			ServletResponseUtil.write(response, "Unexpected error");

			_log.error(e, e);
		}
	}

	@Override
	public void init(ServletConfig servletConfig) {
		String replicateIncludeFile = servletConfig.getInitParameter(
			"replicateIncludeFile");

		ResumableFileUtil.setReplicateIncludeFile(replicateIncludeFile);

		String rootDir = servletConfig.getInitParameter("rootDir");

		FileUtil.setRootDir(rootDir);

		String validIpsString = servletConfig.getInitParameter("validIps");

		_validIps = validIpsString.split(",");
	}

	protected boolean _isValidIP(String remoteAddr) {
		for (String validIp : _validIps) {
			if (remoteAddr.equals(validIp) ||
				remoteAddr.startsWith(validIp + ".")) {

				return true;
			}
		}

		return false;
	}

	protected void cleanTempFiles(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String dirPath = ParamUtil.getString(request, "dirPath");

		if (!FileUtil.isValidPath(dirPath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + dirPath);
			}

			return;
		}

		ResumableFileUtil.cleanTempFiles(dirPath);

		ServletResponseUtil.write(response, ResponseMessages.SUCCESS);

		if (_log.isInfoEnabled()) {
			_log.info("Deleted temporary directory " + dirPath);
		}
	}

	protected void deleteFile(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String filePath = ParamUtil.getString(request, "filePath");
		boolean replicate = ParamUtil.getBoolean(request, "replicate");

		if (!FileUtil.isValidPath(filePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + filePath);
			}

			return;
		}

		boolean deleted = ResumableFileUtil.deleteFile(filePath, replicate);

		if (deleted) {
			ServletResponseUtil.write(response, ResponseMessages.SUCCESS);

			if (_log.isInfoEnabled()) {
				_log.info("Deleted " + filePath);
			}
		}
		else {
			ServletResponseUtil.write(response, ResponseMessages.FAIL);

			if (_log.isInfoEnabled()) {
				_log.info("Unable to delete " + filePath);
			}
		}
	}

	protected void renameFile(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String dirPath = ParamUtil.getString(request, "dirPath");
		String fileName = ParamUtil.getString(request, "fileName");
		String filePath = ParamUtil.getString(request, "filePath");
		boolean delete = ParamUtil.getBoolean(request, "delete");
		boolean replicate = ParamUtil.getBoolean(request, "replicate");

		if (!FileUtil.isValidPath(filePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + filePath);
			}

			return;
		}

		String resumableFilePath = FileUtil.getRelativePath(dirPath, fileName);

		if (!FileUtil.isValidPath(resumableFilePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + resumableFilePath);
			}

			return;
		}

		try {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Started copying " + resumableFilePath + " to " + filePath);
			}

			FileUtil.copyFile(resumableFilePath, filePath);

			if (replicate) {
				ResumableFileUtil.replicateFile(filePath);
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					"Finished copying " + resumableFilePath + " to " +
						filePath);
			}

			if (delete) {
				File file = FileUtil.getFile(resumableFilePath);

				boolean deleted = FileUtil.delete(file);

				if (deleted) {
					if (_log.isInfoEnabled()) {
						_log.info("Deleted " + resumableFilePath);
					}
				}
				else {
					ServletResponseUtil.write(response, ResponseMessages.FAIL);

					if (_log.isInfoEnabled()) {
						_log.info("Unable to delete " + resumableFilePath);
					}
				}
			}
		}
		catch (Exception e) {
			ServletResponseUtil.write(response, ResponseMessages.FAIL);

			if (_log.isInfoEnabled()) {
				_log.info("Unable to copy " + resumableFilePath, e);
			}
		}

		ServletResponseUtil.write(response, ResponseMessages.SUCCESS);
	}

	protected void replicateFile(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String filePath = ParamUtil.getString(request, "filePath");

		if (!FileUtil.isValidPath(filePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + filePath);
			}

			return;
		}

		boolean replicated = ResumableFileUtil.replicateFile(filePath);

		if (replicated) {
			ServletResponseUtil.write(response, ResponseMessages.SUCCESS);

			if (_log.isInfoEnabled()) {
				_log.info("Replicated " + filePath);
			}
		}
		else {
			ServletResponseUtil.write(response, ResponseMessages.FAIL);

			if (_log.isInfoEnabled()) {
				_log.info("Unable to replicate " + filePath);
			}
		}
	}

	protected void updateResumableFile(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String dirPath = ParamUtil.getString(request, "dirPath");
		String fileName = ParamUtil.getString(request, "fileName");
		String filePath = ParamUtil.getString(request, "filePath");

		if (!FileUtil.isValidPath(filePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + filePath);
			}

			return;
		}

		String resumableFilePath = FileUtil.getRelativePath(dirPath, fileName);

		if (!FileUtil.isValidPath(resumableFilePath)) {
			ServletResponseUtil.write(response, ResponseMessages.INVALID_PATH);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + resumableFilePath);
			}

			return;
		}

		ResumableFile resumableFile = ResumableFileUtil.getResumableFile(
			resumableFilePath);

		if (resumableFile != null) {
			boolean replicate = ParamUtil.getBoolean(request, "replicate");

			ResumableFileUtil.updateResumableFile(
				resumableFile, filePath, replicate);

			ServletResponseUtil.write(response, ResponseMessages.SUCCESS);

			if (_log.isInfoEnabled()) {
				_log.info("Updated " + resumableFilePath);
			}
		}
		else {
			ServletResponseUtil.write(response, ResponseMessages.FAIL);

			if (_log.isInfoEnabled()) {
				_log.info("Unable to update " + resumableFilePath);
			}
		}
	}

	private static Log _log = LogFactory.getLog(FileAdminServlet.class);

	private String[] _validIps;

}