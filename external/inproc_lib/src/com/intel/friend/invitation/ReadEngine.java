/*
Copyright (c) 2011-2013, Intel Corporation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

 * Neither the name of Intel Corporation nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.intel.friend.invitation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ReadEngine implements Runnable {
	static final String LOGC = ReadEngine.class.getCanonicalName();

	InputStream mStream;
	IDataStreamEventListener mListener;
	Thread mReadEngineThread;
	volatile boolean mRunning = true;

	private final int MSG_STATUS = 1;
	private final int MSG_DATA = 2;
	private static final int HEADER_SIZE_STATUS = 6;
	private static String TAG_STATUS = "status";
	private static String ATTR_VALUE = "value";

	public ReadEngine(InputStream stream, IDataStreamEventListener listener) {
		this.mStream = stream;
		this.mReadChannel = Channels.newChannel(stream);
		this.mListener = listener;
		mReadEngineThread = new Thread(this);
		mReadEngineThread.setDaemon(true);
		mReadEngineThread.setName("Read Engine Thread");
		mReadEngineThread.start();
	}

	private void statusAvailable(String statusCode) {
		int value = Integer.parseInt(statusCode);
		mListener.statusAvailable(value);
	}

	ReadableByteChannel mReadChannel;

	public static byte[] readChannel(ReadableByteChannel rc, int numBytes) {

		if (rc == null || numBytes < 0) {
			return null;
		}

		ByteBuffer b = null;
		final int maxRetry = 10;
		int retry = 0;
		while (b == null) {
			try {
				b = ByteBuffer.allocate(numBytes);
			} catch (OutOfMemoryError oome) {
				b = null;
				System.gc();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

				retry++;
				if (retry > maxRetry) {
					return null;
				}
			}
		}

		int totalRead = 0;
		int read = 0;

		while (totalRead < numBytes) {
			try {
				read = 0;
				read = rc.read(b);
				if (read == -1) {
					return null;
				}
				totalRead += read;
			} catch (IOException e) {
				return null;
			}
		}

		return b.array();
	}

	/**
	 * Parses 4 bytes from a byte[]
	 * 
	 * @param buffer
	 * @param offset
	 * @return
	 */
	private int parseInt(byte[] buffer, int offset) {
		int ret = ((buffer[offset + 0] & 0xff) << 24)
				| ((buffer[offset + 1] & 0xff) << 16)
				| ((buffer[offset + 2] & 0xff) << 8)
				| (buffer[offset + 3] & 0xff);
		return ret;
	}

	/**
	 * Parses 2 bytes from a byte[]
	 * 
	 * @param buffer
	 * @param offset
	 * @return
	 */
	private short parseShort(byte[] buffer, int offset) {
		short ret = (short) ((buffer[offset + 0] << 8) + (buffer[offset + 1] & 0xFF));
		return ret;
	}

	public void run() {

		byte[] buffer = null;
		mRunning = true;
		while (mRunning) {
			try {
				buffer = readChannel(mReadChannel, 2);
				int type = 0;
				if (buffer == null) {
					mRunning = false;
				}

				type = parseShort(buffer, 0);
				buffer = readChannel(mReadChannel, 4);
				if (buffer == null) {
					mRunning = false;
					continue;
				}

				int size = parseInt(buffer, 0);

				if (size < 0) {
					mRunning = false;
					break;
				}

				if (type == MSG_STATUS) {
					buffer = readChannel(mReadChannel, size
							- HEADER_SIZE_STATUS);

					String bufferString = null;
					try {
						if (buffer != null)
							bufferString = new String(buffer);
						buffer = null;
					} catch (OutOfMemoryError e) {
						bufferString = null;
						buffer = null;
					}

					if (bufferString == null) {
						mRunning = false;
						continue;
					}

					parseStatus(bufferString);
				}
			} catch (Exception ie) {

			}
		} // End running loop

	}

	private void parseStatus(String dataString) {

		if (dataString == null || dataString.compareTo("") == 0)
			return;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Reader reader = new StringReader(dataString);
			Document dom = builder.parse(new InputSource(reader));

			// REQUEST
			NodeList items = dom.getElementsByTagName(TAG_STATUS);
			String statusCode = null;
			for (int i = 0; i < items.getLength(); i++) {
				Node item = items.item(i);
				NamedNodeMap attrs = item.getAttributes();

				for (int j = 0; j < attrs.getLength(); j++) {
					Node attr = attrs.item(j);
					if (attr.getNodeName().compareToIgnoreCase(ATTR_VALUE) == 0) {
						statusCode = attr.getNodeValue();
						statusAvailable(statusCode);
						break;
					}
				}
			}

		} catch (Exception e) {
		}

	}

	public void stop() {
		mRunning = false;
	}
}
