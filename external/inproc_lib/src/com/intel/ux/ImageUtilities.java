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

package com.intel.ux;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.intel.inproclib.utility.InProcConstants;
import com.intel.stc.utility.d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtilities
{
	public static int	THUMB_IMAGE_MAX_SIZE	= 250;

	public static Bitmap getThumbnail(Context context, String filePath)
	{
		Bitmap bitmap = null;

		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		try
		{
			BitmapFactory.decodeStream(context.getAssets().openFd(filePath).createInputStream(), null, o);
		}
		catch (IOException ioe)
		{
			try
			{
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(filePath)));
				BitmapFactory.decodeStream(bis, null, o);
				bis.close();
			}
			catch (IOException ioe1)
			{
				d.error(InProcConstants.INPROC_TAG, "MAIN", "Error getting avatar path " + filePath + " open\n", ioe);
				return null;
			}
		}

		int scale = 1;
		if (o.outHeight > THUMB_IMAGE_MAX_SIZE || o.outWidth > THUMB_IMAGE_MAX_SIZE)
		{
			double tempD = Math.log((THUMB_IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)));
			tempD = tempD / Math.log(0.5);
			tempD = Math.round(tempD);

			scale = (int) Math.pow(2.0, tempD);
		}

		o = new BitmapFactory.Options();
		o.inSampleSize = scale;

		try
		{
			bitmap = BitmapFactory.decodeStream(context.getAssets().openFd(filePath).createInputStream(), null, o);
		}
		catch (IOException ioe)
		{
			try
			{
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(filePath)));
				bitmap = BitmapFactory.decodeStream(bis, null, o);
				bis.close();
			}
			catch (IOException ioe1)
			{
				d.error(InProcConstants.INPROC_TAG, "MAIN", "Error getting avatar path " + filePath + " open\n", ioe);
				return null;
			}
		}
		return bitmap;
	}
}
