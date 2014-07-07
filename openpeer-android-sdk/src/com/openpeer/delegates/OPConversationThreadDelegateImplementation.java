package com.openpeer.delegates;

import android.util.Log;

import com.openpeer.javaapi.ContactStates;
import com.openpeer.javaapi.MessageDeliveryStates;
import com.openpeer.javaapi.OPContact;
import com.openpeer.javaapi.OPConversationThread;
import com.openpeer.javaapi.OPConversationThreadDelegate;
import com.openpeer.javaapi.OPMessage;

public class OPConversationThreadDelegateImplementation extends
		OPConversationThreadDelegate {

	@Override
	public void onConversationThreadNew(OPConversationThread conversationThread) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConversationThreadContactsChanged(
			OPConversationThread conversationThread) {
		Log.d("output", "onConversationThreadContactsChanged  thread = "
				+ conversationThread);

	}

	@Override
	public void onConversationThreadContactStateChanged(
			OPConversationThread conversationThread, OPContact contact,
			ContactStates state) {
		// TODO Auto-generated method stub
		Log.d("output", "onConversationThreadContactStateChanged  state = "
				+ state.toString());
	}

	@Override
	public void onConversationThreadMessage(
			OPConversationThread conversationThread, String messageID) {
//		?OPMessage message = conversationThread.getMessage(messageID);
		Log.d("output", "onConversationThreadMessage = " + conversationThread
				+ " messageId " + messageID );

	}

	@Override
	public void onConversationThreadMessageDeliveryStateChanged(
			OPConversationThread conversationThread, String messageID,
			MessageDeliveryStates state) {
		// TODO Auto-generated method stub
		Log.d("output", "onConversationThreadMessageDeliveryStateChanged = "
				+ conversationThread + " messageId " + messageID + "state = "
				+ state.toString());

	}

	@Override
	public void onConversationThreadPushMessage(
			OPConversationThread conversationThread, String messageID,
			OPContact contact) {
		// OPMessage message = conversationThread.getMessage(messageID);
		Log.d("output",
				"onConversationThreadPushMessage = " + messageID + " thread "
						+ conversationThread);
	}

}