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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intel.stc.utility.d;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

/* This is for use with listviews that take advantage for convertview, 
 * and therefore an image could be ready to display, but be pointing 
 * to the wrong view if you blindly set the image.*/
public abstract class InProc_ListViewImageManager
{
	/********************** Constants ********************************************/
	private final static String tag 						= "InProc";
	private static String className 						= InProc_ListViewImageManager.class.getSimpleName();	
	private final static String	jpegImageMime				= "image/jpg";
	public static int THUMB_IMAGE_MAX_SIZE					= 250;

	/********************** Local Variables ********************************************/
	final Map<String, SoftReference<Bitmap>>	mImageMap				= Collections
																				.synchronizedMap(new HashMap<String, SoftReference<Bitmap>>());
	final Map<ImageView, String>				mViewMap				= Collections
																				.synchronizedMap(new HashMap<ImageView, String>());
	final Context								mContext;

	final List<AsyncTask<Void, Void, Void>>		mTaskList				= Collections
																				.synchronizedList(new ArrayList<AsyncTask<Void, Void, Void>>());
	final Handler								mHandler				= new Handler();
	
	final static MimeTypeMap					mMimeTypeMap			= MimeTypeMap.getSingleton();

	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	// ONLY ON MAIN THREAD!!
	public InProc_ListViewImageManager(Context context) {
		mContext = context;
	}

	/*********************************************************************/
	/**** Android Asynk Tasks ********************************************/
	/*********************************************************************/
	public abstract AsyncTask<Void, Void, Void> getLoader(final String path, final ImageView toBeSet);

	/*********************************************************************/
	/**** Public Helpers  ************************************************/
	/*********************************************************************/

	public void destroyManager()
	{
		mHandler.post(new Runnable() {
			@Override
			public void run()
			{
				mImageMap.clear();
				mViewMap.clear();
				for (AsyncTask<?, ?, ?> task : mTaskList)
					task.cancel(true);
			}
		});
	}
	
	public static String getMimeFromFile(String file)
	{
		String mime = null;

		String[] delName = file.split("\\.");

		String type = "";
		if (delName.length > 0)
		{
			type = delName[delName.length - 1];
			
			// Fix: Show custom avatar picture in the edit profile page.
			// Originally mime for jpeg or jpg file would be null. 
			// This would show default SD card image instead of selected image from gallery.
			if(type.equalsIgnoreCase("JPG") || type.equalsIgnoreCase("JPEG"))
			{
				mime = jpegImageMime;
			}
			else
			{
				mime = mMimeTypeMap.getMimeTypeFromExtension(type);
			}
		}

		return mime;
	}

	public static Bitmap getImageFromFile(Context context, String file, String mime)
	{
		if (file == null || mime == null)
			return null;

		Bitmap b = null;

		// @formatter:off
		if (mime.matches(".*video.*"))
		{
			Cursor c = null;
			try
			{
				c = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Video.Media._ID }, MediaStore.Video.Media.DATA + " like ? ",
						new String[] { file }, null);
			}
			catch (Exception e)
			{
				d.error(InProcConstants.INPROC_TAG, tag, "getImageFromFile", e);
				return null;
			}

			if(c == null)
				return null;
						
			if (!c.moveToFirst())
			{
				if(!c.isClosed())
					c.close();
				return null;
			}

			int columnIndex = c.getColumnIndex(MediaStore.Video.Media._ID);
			long origId = c.getLong(columnIndex);
			c.close();

			b = MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(), origId,
					MediaStore.Video.Thumbnails.MICRO_KIND, null);
		}
		else if (mime.matches(".*image.*"))
		{
			Cursor c = null;
			try
			{
				c = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + " like ? ",
						new String[] { file }, null);
			}
			catch (Exception e)
			{
				d.error(InProcConstants.INPROC_TAG, tag, "getImageFromFile", e);
				return null;
			}

			if(c == null)
				return null;
						
			if (!c.moveToFirst())
			{
				if(!c.isClosed())
					c.close();
				//We don't have a thumbnail, request it be added to the gallery.
				scanFile(context, file);
				return null;
			}

			int columnIndex = c.getColumnIndex(MediaStore.Images.Media._ID);
			long origId = c.getLong(columnIndex);
			c.close();

			b = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), origId,
					MediaStore.Images.Thumbnails.MICRO_KIND, null);
		}
		// @formatter:on

		return b;
	}

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

	// ONLY ON MAIN THREAD!!
	public void resetImageView(ImageView toBeReset)
	{
		mViewMap.remove(toBeReset);
	}
		
	// ONLY ON MAIN THREAD!!
	public void requestImage(String path, ImageView toBeSet)
	{
		mViewMap.remove(toBeSet);
		mViewMap.put(toBeSet, path);
		SoftReference<Bitmap> b = mImageMap.get(path);
		if (b != null && b.get() != null)
		{
			toBeSet.setImageBitmap(b.get());
			return;
		}
		else
		{
			toBeSet.setImageResource(com.intel.inproclib.R.drawable.generic_avatar);

			mTaskList.add(getLoader(path, toBeSet).execute());
		}
	}
		
	public static void scanFile(Context context, String filePath)
	{
		if (context != null && filePath != null)
		{
			File f = new File(filePath);
			if (f.exists())
				scanFile(context, f);
		}
	}

	public static void scanFile(Context context, File downloadedFile)
	{
		Uri contentUri = Uri.fromFile(downloadedFile);
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		mediaScanIntent.setData(contentUri);
		context.sendBroadcast(mediaScanIntent);
	}

}
