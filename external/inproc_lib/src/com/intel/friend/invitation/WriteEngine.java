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
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import android.util.Xml;

public class WriteEngine implements Runnable {
	static final String LOGC = WriteEngine.class.getCanonicalName();
	static String ATTR_VALUE = "value";
	static String ATTR_ID = "ID";
	static final int HEADER_SIZE_DATA = 26;
	static final int HEADER_SIZE_STATUS = 6;
	static final int MESSAGE_DATA = 2;
	static final int MESSAGE_STATUS = 1;
	static String TAG_STATUS = "status";

	IDataStreamEventListener mListener;
	OutputStream mStream;
	BlockingQueue<byte[]> mQueueByte;
	Thread mWriteEngineThread;
	volatile boolean mRunning = true;

	public static byte[] getStatusPacket(int dataLength) {

		byte[] pckt = new byte[dataLength + HEADER_SIZE_STATUS];

		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.putShort((short) MESSAGE_STATUS);
		System.arraycopy(bb.array(), 0, pckt, 0, bb.capacity());

		bb = ByteBuffer.allocate(4);
		bb.putInt(pckt.length);
		System.arraycopy(bb.array(), 0, pckt, 2, bb.capacity());

		return pckt;
	}

	public byte[] constructStatusPacket(int statusCode) {

		byte[] packet = null;
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF8", null);
			serializer.startTag("", TAG_STATUS);
			serializer.attribute("", ATTR_VALUE, String.valueOf(statusCode));
			serializer.endTag("", TAG_STATUS);
			serializer.endDocument();
			byte[] data = writer.toString().trim().getBytes("UTF8");

			packet = getStatusPacket(data.length);
			System.arraycopy(data, 0, packet, HEADER_SIZE_STATUS, data.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return packet;

	}

	public WriteEngine(OutputStream stream, IDataStreamEventListener listener) {
		this.mListener = listener;
		this.mStream = stream;
		mQueueByte = new LinkedBlockingQueue<byte[]>();
		mWriteEngineThread = new Thread(this);
		mWriteEngineThread.setDaemon(true);
		mWriteEngineThread.setName("Write Engine Thread");
		mWriteEngineThread.start();
	}

	public void writeBuffer(byte[] data) {
		mQueueByte.add(data);
	}

	public void stop() {
		mRunning = false;
		mQueueByte.clear();
	}

	public void run() {

		while (mRunning) {
			try {
				if (mQueueByte != null && !mQueueByte.isEmpty()) {

					byte[] buffer = mQueueByte.take();
					if (mRunning) {
						mStream.write(buffer);
					}
				}
			} catch (InterruptedException e) {
				Log.e(LOGC, e.toString());
				mListener.remoteDisconnect();
			} catch (IOException e) {
				Log.i(LOGC, e.toString());
				mListener.remoteDisconnect();
			}
		}
	}
}
