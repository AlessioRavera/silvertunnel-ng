/*
 * silvertunnel.org Netlib - Java library to easily access anonymity networks
 * Copyright (c) 2009-2012 silvertunnel.org
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */
/*
 * silvertunnel-ng.org Netlib - Java library to easily access anonymity networks
 * Copyright (c) 2013 silvertunnel-ng.org
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.silvertunnel_ng.netlib.tool;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.silvertunnel_ng.netlib.adapter.url.NetlibURLStreamHandlerFactory;
import org.silvertunnel_ng.netlib.api.NetLayer;
import org.silvertunnel_ng.netlib.api.util.TcpipNetAddress;
import org.silvertunnel_ng.netlib.layer.tor.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for easy HTTP GET and HTTP POST requests.
 * 
 * All methods assume UTF-8 encoding. All methods do internally use
 * java.net.URL.
 * 
 * @author Tobias Boese
 */
public final class SimpleHttpClientCompressed
{
	/** */
	private static final Logger LOG = LoggerFactory.getLogger(SimpleHttpClientCompressed.class);

	private static SimpleHttpClientCompressed instance = new SimpleHttpClientCompressed();

	/**
	 * @return singleton instance
	 */
	public static SimpleHttpClientCompressed getInstance()
	{
		return instance;
	}

	/** protocol to be used. */
	private static final String PROTOCOL_HTTP = "http";

	/**
	 * Execute HTTP GET request.
	 * 
	 * If you want to define timeouts than you should wrap the lowerNetLayer by
	 * a ControlNetLayer.
	 * 
	 * @param netLayer
	 * @param hostAndPort
	 * @param path
	 * @return response as String, not null
	 * @throws IOException
	 *             in the case of any error
	 * @throws DataFormatException
	 */
	public String get(final NetLayer netLayer, TcpipNetAddress hostAndPort, String path) throws IOException, DataFormatException
	{
		String urlStr = null;
		DataInputStream in = null;
		final long startTime = System.currentTimeMillis();
		try
		{

			if (LOG.isDebugEnabled())
			{
				LOG.debug("start download with hostAndPort=" + hostAndPort + " and path=" + path);
			}

			// prepare URL handling on top of the lowerNetLayer
			final NetlibURLStreamHandlerFactory factory = new NetlibURLStreamHandlerFactory(false);
			factory.setNetLayerForHttpHttpsFtp(netLayer);

			// create the suitable URL object
			if (path != null && !path.startsWith("/"))
			{
				path = "/" + path;
			}
			path += ".z";
			urlStr = PROTOCOL_HTTP + "://" + hostAndPort.getHostnameOrIpaddress() + ":" + hostAndPort.getPort() + path;
			final URLStreamHandler handler = factory.createURLStreamHandler("http");
			final URL context = null;
			final URL url = new URL(context, urlStr, handler);

			// open connection and read response
			final URLConnection conn = url.openConnection();
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.connect();
			// read response code
			if (conn instanceof HttpURLConnection)
			{
				final HttpURLConnection httpConnection = (HttpURLConnection) conn;
				final int code = httpConnection.getResponseCode();

				// is it a "successful" code?
				if (!(code >= 200 && code < 300))
				{
					// no: not successful
					throw new IOException(PROTOCOL_HTTP + " transfer was not successful for url=" + urlStr);
				}
			}
			else
			{
				// wrong protocol (handler)
				throw new IOException(PROTOCOL_HTTP + " response code could not be determined for url=" + urlStr);
			}

			in = new DataInputStream(conn.getInputStream());
			final DynByteBuffer byteBuffer = new DynByteBuffer(conn.getContentLengthLong());
			final byte[] buffer = new byte[512000];
			int count;
			while ((count = in.read(buffer)) > 0)
			{
				byteBuffer.append(buffer, 0, count);
			}
			long timeReceived = System.currentTimeMillis();

			final Inflater inflater = new Inflater();
			inflater.setInput(byteBuffer.toArray());
			byteBuffer.init();
			while (!inflater.finished())
			{
				final int len = inflater.inflate(buffer);
				byteBuffer.append(buffer, 0, len);
			}
			final String response = new String(byteBuffer.toArray(), Util.UTF8);
			// result
			if (LOG.isDebugEnabled())
			{
				LOG.debug("end download with hostAndPort=" + hostAndPort + " and path=" + path + " finished with result of length="
						+ response.length() + " timeReceived : " + (timeReceived - startTime) + " ms timeDecompress : "
						+ (System.currentTimeMillis() - timeReceived) + " ms");
			}
			return response;

		}
		catch (final IOException e)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("end download with hostAndPort=" + hostAndPort + " and path=" + path + " with " + e, e);
			}
			throw e;
		}
		catch (final DataFormatException e)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("end download with hostAndPort=" + hostAndPort + " and path=" + path + " with " + e, e);
			}
			throw e;
		}
		finally
		{
			// close stream(s)
			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (final IOException e)
				{
					LOG.warn("Exception while closing InputStream from url=" + urlStr);
				}
			}
		}
	}
}