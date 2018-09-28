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

import javax.servlet.http.HttpServletRequest;

/**
 * @author Amos Fong
 */
public class ParamUtil {

	public static boolean getBoolean(HttpServletRequest request, String param) {
		return getBoolean(request, param, false);
	}

	public static boolean getBoolean(
		HttpServletRequest request, String param, boolean defaultValue) {

		String value = request.getParameter(param);

		if (value == null) {
			return defaultValue;
		}
		else {
			return Boolean.valueOf(value);
		}
	}

	public static int getInteger(HttpServletRequest request, String param) {
		return getInteger(request, param, 0);
	}

	public static int getInteger(
		HttpServletRequest request, String param, int defaultValue) {

		String value = request.getParameter(param);

		if (value == null) {
			return defaultValue;
		}
		else {
			return Integer.valueOf(value);
		}
	}

	public static long getLong(HttpServletRequest request, String param) {
		return getLong(request, param, 0);
	}

	public static long getLong(
		HttpServletRequest request, String param, long defaultValue) {

		String value = request.getParameter(param);

		if (value == null) {
			return defaultValue;
		}
		else {
			return Long.valueOf(value);
		}
	}

	public static String getString(HttpServletRequest request, String param) {
		return getString(request, param, "");
	}

	public static String getString(
		HttpServletRequest request, String param, String defaultValue) {

		String value = request.getParameter(param);

		if (value == null) {
			return defaultValue;
		}
		else {
			return value;
		}
	}

}