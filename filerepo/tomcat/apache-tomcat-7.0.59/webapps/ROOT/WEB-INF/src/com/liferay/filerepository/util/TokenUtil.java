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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Amos Fong
 */
public class TokenUtil {

	public static final long _LIFETIME = 1000 * 60 * 5;

	public static String createToken(String dirPath) {
		long currentTime = System.currentTimeMillis();

		_expungeExpiredTokens(currentTime);

		Token token = new Token(dirPath, currentTime + _LIFETIME);

		_tokens.put(token.getToken(), token);

		return token.getToken();
	}

	public static String getDirPath(String token) {
		long currentTime = System.currentTimeMillis();

		_expungeExpiredTokens(currentTime);

		String dirPath = null;

		synchronized (_tokens) {
			Token tokenObj = _tokens.get(token);

			if (tokenObj != null) {
				tokenObj.setExpireTime(currentTime + (_LIFETIME * 10));

				dirPath = tokenObj.getDirPath();
			}
		}

		return dirPath;
	}

	private static void _expungeExpiredTokens(long currentTime) {
		synchronized (_tokens) {
			Set<Map.Entry<String, Token>> tokens = _tokens.entrySet();

			Iterator<Map.Entry<String, Token>> itr = tokens.iterator();

			while (itr.hasNext()) {
				Map.Entry<String, Token> entry = itr.next();

				Token token = entry.getValue();

				if (token.getExpireTime() < currentTime) {
					itr.remove();
				}
				else {
					break;
				}
			}
		}
	}

	private static final Map <String, Token> _tokens =
		Collections.synchronizedMap(
			new LinkedHashMap<String, Token>(16, .75f, true));

}