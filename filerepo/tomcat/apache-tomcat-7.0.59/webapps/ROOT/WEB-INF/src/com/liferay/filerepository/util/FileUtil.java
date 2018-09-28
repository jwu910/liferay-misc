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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Amos Fong
 */
public class FileUtil {

	public static synchronized void append(File file, List<String> contents) {
		try {
			FileWriter fileWriter = new FileWriter(file, true);

			for (String content : contents) {
				content += _RETURN_NEW_LINE;

				fileWriter.write(content);
			}

			fileWriter.flush();

			fileWriter.close();
		}
		catch (IOException ioe) {
		}
	}

	public static void copyFile(String source, String destination)
		throws IOException {

		Path sourcePath = Paths.get(_rootDir, source);

		Path destinationPath = Paths.get(_rootDir, destination);

		File destinationFile = destinationPath.toFile();

		File parentFile = destinationFile.getParentFile();

		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}

		Files.copy(sourcePath, destinationPath, LinkOption.NOFOLLOW_LINKS);
	}

	public static boolean delete(File file) {
		if (file.exists()) {
			return file.delete();
		}

		return true;
	}

	public static void deltree(File directory) {
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		File[] files = directory.listFiles();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				deltree(files[i]);
			}
			else {
				files[i].delete();
			}
		}

		directory.delete();
	}

	public static List<String> getAncestorDirPaths(File file) {
		List<String> ancestorDirPaths = new ArrayList<String>();

		if ((file == null) || file.equals(new File(_rootDir))) {
			return ancestorDirPaths;
		}

		ancestorDirPaths.addAll(getAncestorDirPaths(file.getParentFile()));

		String filePath = file.getPath();

		String relativePath = filePath.replaceFirst(_rootDir, "");

		ancestorDirPaths.add(relativePath);

		return ancestorDirPaths;
	}

	public static List<String> getAncestorDirPaths(String filePath) {
		return getAncestorDirPaths(getFile(filePath));
	}

	public static File getFile(String filePath) {
		return new File(_rootDir + "/" + filePath);
	}

	public static String getRelativePath(String dirPath, String fileName) {
		if ((dirPath == null) || fileName.equals("")) {
			return null;
		}

		StringBuilder sb = new StringBuilder(3);

		sb.append(dirPath);
		sb.append("/");
		sb.append(fileName);

		return sb.toString();
	}

	public static boolean isValidPath(String dirPath) {
		if ((dirPath == null) ||
			dirPath.contains("\\") ||
			dirPath.contains("\\\\") ||
			dirPath.contains("//") ||
			dirPath.contains(":") ||
			dirPath.contains("*") ||
			dirPath.contains("?") ||
			dirPath.contains("\"") ||
			dirPath.contains("<") ||
			dirPath.contains(">") ||
			dirPath.contains("|") ||
			dirPath.contains("[") ||
			dirPath.contains("]") ||
			dirPath.contains("../") ||
			dirPath.contains("/..")) {

			return false;
		}

		return true;
	}

	public static boolean renameTo(File source, File destination) {
		if (!source.exists() || destination.exists()) {
			return false;
		}

		File parentFile = destination.getParentFile();

		parentFile.mkdirs();

		boolean renamed = source.renameTo(destination);

		if (renamed) {
			delete(source);
		}

		return renamed;
	}

	public static void setRootDir(String rootDir) {
		_rootDir = rootDir;
	}

	public static void write(
			File file, long pos, InputStream inputStream, long contentLength)
		throws IOException {

		if (!file.exists()) {
			File parentFile = file.getParentFile();

			parentFile.mkdirs();
		}

		file.setWritable(true, false);

		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

		try {
			randomAccessFile.seek(pos);

			byte[] bytes = new byte[1024 * 100];

			while (contentLength > 0) {
				int readBytes = inputStream.read(bytes);

				if (readBytes < 0) {
					break;
				}

				randomAccessFile.write(bytes, 0, readBytes);

				contentLength -= readBytes;
			}
		}
		finally {
			randomAccessFile.close();
		}
	}

	private static String _RETURN_NEW_LINE = System.getProperty(
		"line.separator");

	private static String _rootDir;

}