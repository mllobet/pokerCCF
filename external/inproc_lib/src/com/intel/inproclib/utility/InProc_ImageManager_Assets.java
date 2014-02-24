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

package com.intel.inproclib.utility;

import java.lang.ref.SoftReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

public class InProc_ImageManager_Assets extends InProc_ListViewImageManager
{
	public InProc_ImageManager_Assets(Context context) {
		super(context);
	}

	@Override
	public AsyncTask<Void, Void, Void> getLoader(String path, ImageView toBeSet)
	{
		return new LoadTask(path, toBeSet);
	}

	private class LoadTask extends AsyncTask<Void, Void, Void>
	{
		final String	mPath;
		final ImageView	mView;
		Bitmap			b;

		public LoadTask(String path, ImageView view) {
			mPath = path;
			mView = view;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			final String mime = getMimeFromFile(mPath);
			if (mime == null)
				return null;

			try
			{
				if (mime.matches(".*image.*"))
					b = getThumbnail(mContext, mPath);
			}
			catch (OutOfMemoryError e)
			{
			}

			if (b != null)
				mImageMap.put(mPath, new SoftReference<Bitmap>(b));

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;

			String temp = mViewMap.get(mView);
			if (temp == null || mPath.compareTo(temp) != 0)
				return;
			else if (b != null)
				mView.setImageBitmap(b);

			for (int i = mTaskList.size() - 1; i >= 0; i--)
			{
				AsyncTask<?, ?, ?> tempTask = mTaskList.get(i);
				if (tempTask == this)
				{
					mTaskList.remove(i);
					return;
				}
			}
		}
	}
}
