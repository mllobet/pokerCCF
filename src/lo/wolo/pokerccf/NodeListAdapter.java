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
package lo.wolo.pokerccf;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
/**
 * Manages the list of created and joined nodes.
 *
 */
public class NodeListAdapter extends BaseAdapter {

	private ArrayList<NodeWrapper> discoveryNodeList = new ArrayList<NodeWrapper>();
	
	private DiscoveryNodeActivity baseActivity;
	private CCFManager serviceManager = null;
	
	//Constructor to initialize the reference of NodeListAdapter.
	public NodeListAdapter(DiscoveryNodeActivity baseActivity, CCFManager manager) {
		this.baseActivity = baseActivity;
		serviceManager = manager;
		discoveryNodeList = serviceManager.getDiscoveryNodeList();
	}
	
	//To update the discoveryNodeList.
	public void setDiscoveryNodeList(ArrayList<NodeWrapper> discoveryNodeList){
		this.discoveryNodeList = discoveryNodeList;
	}
	
	//To remove the significant node.
	private void removeDiscoveryNode(final NodeWrapper node){
		if(discoveryNodeList.contains(node))
		{
			baseActivity.runOnUiThread(new Runnable(){
	            @Override
	            public void run() {
	            	baseActivity.leaveDN(node.getNodeName());
	            	serviceManager.getDiscoveryNodeList().remove(node);
	    			discoveryNodeList.remove(node);
	            	notifyDataSetChanged();
	            }
	        });
			
		}
	}
	
	//To retrieve the count of discoveryNodeList.
	@Override
	public int getCount() {
		synchronized (discoveryNodeList) {
			return discoveryNodeList.size();
		}
	}

	//To retrieve the element at significant position on discoveryNodeList.
	@Override
	public Object getItem(int position) {
		
		synchronized(discoveryNodeList) {
			if (discoveryNodeList != null && position < discoveryNodeList.size() && position >= 0)
				return discoveryNodeList.get(position);
			else
				return null;
		}
	}

	//To retrieve the position of the object in the discoveryNodeList.
	@Override
	public long getItemId(int position) {
		return position;
	}

	//This method will be called automatically by Android to draw the discoveryNodeList UI.
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)baseActivity.getApplicationContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.node_row, null);
		} else
			convertView.setVisibility(View.VISIBLE);

		NodeWrapper curNode = null;
		synchronized (discoveryNodeList) {
			if (position >= 0 && position < discoveryNodeList.size()) 
				curNode = (NodeWrapper)getItem(position);
		}
		
		if( curNode == null ) {
			convertView.setVisibility(View.GONE);
			return convertView;
		}

		// get the name from the session and put it into the text view
		TextView nodeName = (TextView)convertView.findViewById(R.id.node_name);
		nodeName.setText(curNode.getNodeName());
		
		TextView nodeStatus = (TextView)convertView.findViewById(R.id.node_status);
		if(curNode.getNodeStatus()){
			nodeStatus.setText("Published");
		}else{
			nodeStatus.setText("Subscribing");
		}

		// setup a click handler to pass invites up to the service.
		final NodeWrapper node = curNode;
		convertView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				displayDialog(node);
			}
		});
		
		return convertView;
	}
	
	//Display confirmation dialog while leaving the node.
	private void displayDialog(final NodeWrapper node){
		final AlertDialog.Builder builder = new AlertDialog.Builder(baseActivity);
		builder.setTitle("Leave Discovery Node: "+node.getNodeName());
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				removeDiscoveryNode(node);
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(builder!=null)
					dialog.dismiss();
			}
		});
		builder.show();
	}

}
