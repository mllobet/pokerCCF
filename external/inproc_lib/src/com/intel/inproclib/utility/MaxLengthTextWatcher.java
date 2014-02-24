//******************************************************************
//
// Copyright (c) 2011-2013, Intel Corporation
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//    * Redistributions of source code must retain the above copyright notice,
//      this list of conditions and the following disclaimer.
//
//    * Redistributions in binary form must reproduce the above copyright notice,
//      this list of conditions and the following disclaimer in the documentation
//      and/or other materials provided with the distribution.
//
//    * Neither the name of Intel Corporation nor the names of its contributors
//      may be used to endorse or promote products derived from this software
//      without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
// OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.
//
//
//******************************************************************
// File name:       
//     MaxLengthTextWatcher.java
//
// Description:     
//     
//
// Author:          
//     kmmillix
//
// Review History:  
//     
//
//*********************************************************************
package com.intel.inproclib.utility;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * @author kmmillix
 * 
 */
public class MaxLengthTextWatcher implements TextWatcher
{
	private final int		maxLength;
	private final EditText	editBox;
	private final Handler	handler	= new Handler();

	private int				newStart, newCount;

	public MaxLengthTextWatcher(int maxLength, EditText editBox) {
		this.maxLength = maxLength;
		this.editBox = editBox;
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
		newStart = start;
		newCount = count;
	}

	@Override
	public void afterTextChanged(final Editable s)
	{
		String l = s.toString();

		l = l.replace('\n', ' ');
		if (l.compareTo(s.toString()) != 0)
		{
			s.clear();
			s.append(l);
		}

		if (l.length() > maxLength)
		{
			if (editBox != null)
			{
				final int diff = l.length() - maxLength;
				handler.post(new Runnable() {
					@Override
					public void run()
					{
						int sel = editBox.getSelectionStart() - 1;
						if (sel < 0)
							sel = 0;

						String newString = new String();
						if (newStart != 0)
						{
							newString += s.subSequence(0, newStart);
						}
						newString += s.subSequence(newStart, (newStart + newCount) - diff);
						newString += s.subSequence(newStart + newCount, s.length());

						editBox.setText(newString);
						editBox.setSelection(Math.min(sel, editBox.getText().length()));
					}
				});
			}
		}
	}
}
