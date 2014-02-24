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
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

/***
 * Handles writing to the output stream.
 * <p>
 * There is nothing c3 specific here.
 */
public class WriteEngine implements Runnable 
{
	static final String LOGC = "PokerCCF"
	
	IServiceIOListener listener;
	OutputStream stream;
	BlockingQueue<String> queue;
	Thread thread;
	volatile boolean running = true;
	
	public WriteEngine(OutputStream stream, IServiceIOListener listener)
	{
		this.listener = listener;
		this.stream = stream;
		queue = new LinkedBlockingQueue<String>();
		thread = new Thread(this);
		thread.setDaemon(true);
		thread.setName("Write Engine Thread");
		thread.start();
	}
	
	public void writeString(String s)
	{
		queue.add(s);
	}
	
	public void stop()
	{
		running = false;
		queue.add(" ");
	}

	public void run() 
	{
		
		while( running )
		{
			try {
				String s = queue.take();
				if( running ) {
					stream.write(s.getBytes());
				}
			} catch(InterruptedException e) {
				Log.e(LOGC, e.toString() );
				listener.remoteDisconnect();
			} catch(IOException e) {
				Log.i(LOGC, e.toString() );
				listener.remoteDisconnect();
			}
		}
	}
}
