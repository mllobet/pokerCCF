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

package com.intel.multiconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/***
 * Handles reading from the input stream and parsing into strings that are stored
 * in an ArrayList.
 * <p>
 * There is nothing c3 specific here.
 */
public class ReadEngine implements Runnable 
{	
	static final String LOGC = ReadEngine.class.getCanonicalName();
	
	InputStream stream;
	IServiceIOListener listener;
	Thread thread;
	volatile boolean running = true;
	ArrayList<String>lines = new ArrayList<String>();
	
	public ReadEngine( InputStream stream, IServiceIOListener listener )
	{
		this.stream = stream;
		this.listener = listener;
		thread = new Thread(this);
		thread.setDaemon(true);
		thread.setName("Read Engine Thread");
		thread.start();
	}
	
	private void addLine(StringBuffer sb)
	{
		String line = sb.toString();
//		int count;
		synchronized( lines )
		{
			lines.add(line);
//			count = lines.size();
		}
		listener.lineReceived(line);
	}
	
	public String getLine(int number)
	{
		if(number< 0)
			return "";
		synchronized(lines)
		{
			if(number >= lines.size())
				return "";
			
			return lines.get(number);
		}
	}
	
	public List<String> getLines()
	{
		synchronized(lines) {
			return new ArrayList<String>(lines);
		}
	}
	
	public void run() 
	{
		try {
			char buffer[] = new char[1024];
			InputStreamReader reader = new InputStreamReader(stream, "US-ASCII");
			StringBuffer sb = new StringBuffer();
			while( running )
			{
				int numBytes = reader.read(buffer);
				if( numBytes < 0 )
				{
					running = false;
					listener.remoteDisconnect();
				}
				else 
				{
					sb.append(buffer, 0, numBytes);
					addLine(sb);
					sb = new StringBuffer();
					// for( int i = 0 ; i < numBytes ; i++ )
					// {
					// if( buffer[i] == '\r' )
					// {
					// addLine(sb);
					// sb = new StringBuffer();
					// }
					// else
					// sb.append(buffer[i]);
					// }
				}
			}
		} catch(UnsupportedEncodingException e) {
			Log.e(LOGC, "unsupported encoding?");
		} catch(IOException e) {	
			listener.remoteDisconnect();
			running = false;
		}
	}
	
	public void stop()
	{
		running = false;
	}
}
