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
//     NoNewlineEditText.java
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * @author kmmillix
 * 
 */
public class NoNewlineEditText extends EditText
{

	/**
	 * @param context
	 */
	public NoNewlineEditText(Context context) {
		super(context);
	}
	
	public NoNewlineEditText(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
	}

	/* (non-Javadoc)
	 * @see android.widget.TextView#onCreateInputConnection(android.view.inputmethod.EditorInfo)
	 */
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs)
	{
		InputConnection connection = super.onCreateInputConnection(outAttrs);
		int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
		if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0)
		{
			// clear the existing action
			outAttrs.imeOptions ^= imeActions;
			// set the DONE action
			outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
		}
		if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0)
		{
			outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
		}
		return connection;
	}
}
