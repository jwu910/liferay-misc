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
import com.liferay.filerepository.util.TokenUtil;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

/**
 * @author Amos Fong
 */
public class FileUploadServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		String token = ParamUtil.getString(request, "token");

		String dirPath = TokenUtil.getDirPath(token);

		if (dirPath == null) {
			write(
				response, ResponseMessages.INVALID_SESSION,
				HttpServletResponse.SC_BAD_REQUEST);

			return;
		}

		String fileName = ParamUtil.getString(request, "resumableFilename");

		if (!FileUtil.isValidPath(fileName)) {
			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + fileName);
			}

			write(
				response, ResponseMessages.INVALID_FILE,
				HttpServletResponse.SC_BAD_REQUEST);

			return;
		}

		int chunkNumber = ParamUtil.getInteger(request, "resumableChunkNumber");

		String resumableFilePath = FileUtil.getRelativePath(dirPath, fileName);

		ResumableFile resumableFile = ResumableFileUtil.getResumableFile(
			resumableFilePath);

		if (resumableFile == null) {
			write(
				response, ResponseMessages.NOT_FOUND,
				HttpServletResponse.SC_NO_CONTENT);
		}
		else if (resumableFile.isComplete()) {
			write(response, resumableFile, ResponseMessages.FILE_EXISTS);
		}
		else if (resumableFile.hasChunkReceived(chunkNumber)) {
			write(response, ResponseMessages.SKIP, HttpServletResponse.SC_OK);
		}
		else {
			write(
				response, ResponseMessages.NOT_FOUND,
				HttpServletResponse.SC_NO_CONTENT);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		request.setCharacterEncoding("UTF-8");

		String token = ParamUtil.getString(request, "token");

		String dirPath = TokenUtil.getDirPath(token);

		if (dirPath == null) {
			write(
				response, ResponseMessages.INVALID_SESSION,
				HttpServletResponse.SC_BAD_REQUEST);

			return;
		}

		String fileName = ParamUtil.getString(request, "resumableFilename");

		if (!FileUtil.isValidPath(fileName)) {
			write(
				response, ResponseMessages.INVALID_FILE,
				HttpServletResponse.SC_BAD_REQUEST);

			if (_log.isInfoEnabled()) {
				_log.info("Invalid path " + fileName);
			}

			return;
		}

		try {
			uploadResumableFile(request, response, dirPath, fileName);
		}
		catch (Exception e) {
			_log.error(e, e);

			write(
				response, ResponseMessages.FAIL,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	protected void uploadResumableFile(
			HttpServletRequest request, HttpServletResponse response,
			String dirPath, String fileName)
		throws Exception {

		int chunkNumber = ParamUtil.getInteger(request, "resumableChunkNumber");

		String resumableFilePath = FileUtil.getRelativePath(dirPath, fileName);

		ResumableFile resumableFile = ResumableFileUtil.getResumableFile(
			resumableFilePath);

		if (resumableFile == null) {
			long chunkSize = ParamUtil.getLong(request, "resumableChunkSize");
			int totalChunks = ParamUtil.getInteger(
				request, "resumableTotalChunks");
			long totalSize = ParamUtil.getLong(request, "resumableTotalSize");

			if (!ResumableFileUtil.isValid(
					fileName, chunkSize, totalChunks, totalSize)) {

				if (_log.isInfoEnabled()) {
					_log.info("Invalid resumable file " + fileName);
				}

				write(
					response, ResponseMessages.INVALID_FILE,
					HttpServletResponse.SC_BAD_REQUEST);

				return;
			}

			resumableFile = ResumableFileUtil.addResumableFile(
				fileName, resumableFilePath, chunkSize, totalChunks, totalSize);
		}

		resumableFile = ResumableFileUtil.uploadResumableFile(
			resumableFile, chunkNumber, request.getInputStream(),
			request.getContentLength());

		if (resumableFile.isComplete()) {
			ResumableFileUtil.finishUpload(resumableFile);

			if (_log.isInfoEnabled()) {
				_log.info("Uploaded " + resumableFile.getTempFilePath());
			}

			write(response, resumableFile, ResponseMessages.COMPLETE);
		}
		else {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Uploaded chunk " + chunkNumber + " for " +
						resumableFile.getTempFilePath());
			}

			write(
				response, ResponseMessages.SUCCESS, HttpServletResponse.SC_OK);
		}
	}

	protected void write(
		HttpServletResponse response, ResumableFile resumableFile,
		String message) {

		try {
			JSONObject jsonObject = new JSONObject();

			jsonObject.put("file", resumableFile.toJSONObject());
			jsonObject.put("message", message);

			writeJSON(response, jsonObject);
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	protected void write(
		HttpServletResponse response, String message, int status) {

		try {
			JSONObject jsonObject = new JSONObject();

			jsonObject.put("message", message);

			response.setStatus(status);

			writeJSON(response, jsonObject);
		}
		catch (Exception e) {
			_log.error(e, e);
		}
	}

	protected void writeJSON(
			HttpServletResponse response, JSONObject jsonObject)
		throws IOException {

		response.setCharacterEncoding("UTF-8");
		response.setContentType(Constants.APPLICATION_JSON);

		ServletResponseUtil.write(response, jsonObject.toString());
	}

	private static Log _log = LogFactory.getLog(FileUploadServlet.class);

}