import java.text.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.spec.*;
import java.lang.reflect.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class testKey {

	public static void main(String[] args) throws Exception {
		HttpURLConnection  connection = (HttpURLConnection )new URL("http://localhost:7080/download/osb/support/41213/log4j.dtd").openConnection();

		connection.setDoOutput(true);
		connection.setDoInput ( true );
		connection.setRequestMethod( "POST" );
		connection.connect();

		BufferedReader in = new BufferedReader(
			new InputStreamReader(connection.getInputStream()));

		String buffer = in.readLine();

		System.out.println(buffer);
	}

}