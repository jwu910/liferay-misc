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

import java.util.UUID;

/**
 * @author Amos Fong
 */
public class Token {

	public Token(String dirPath, long expireTime) {
		_dirPath = dirPath;
		_expireTime = expireTime;

		_token = UUID.randomUUID().toString();
	}

	public String getDirPath() {
		return _dirPath;
	}

	public long getExpireTime() {
		return _expireTime;
	}

	public String getToken() {
		return _token;
	}

	public void setExpireTime(long expireTime) {
		_expireTime = expireTime;
	}

	private String _dirPath;
	private long _expireTime;
	private String _token;

}