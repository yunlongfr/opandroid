package com.openpeer.sample.conversation;

import java.text.DateFormat;
import com.openpeer.sample.BaseActivity;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.openpeer.app.OPChatWindow;
import com.openpeer.app.OPDataManager;
import com.openpeer.app.OPSession;
import com.openpeer.app.OPUser;
import com.openpeer.datastore.DatabaseContracts;
import com.openpeer.datastore.DatabaseContracts.ContactsViewEntry;
import com.openpeer.datastore.DatabaseContracts.MessageEntry;
import com.openpeer.javaapi.OPConversationThread;
import com.openpeer.javaapi.OPIdentityContact;
import com.openpeer.javaapi.OPMessage;
import com.openpeer.javaapi.OPMessage.OPMessageType;
import com.openpeer.sample.BaseFragment;
import com.openpeer.sample.BuildConfig;
import com.openpeer.sample.IntentData;
import com.openpeer.sample.OPSessionManager;
import com.openpeer.sample.R;
import com.openpeer.sample.util.DateFormatUtils;
import com.squareup.picasso.Picasso;

public class ChatFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		ProfilePickerFragment.ProfilePickerListener {
	private final static int VIEWTYPE_SELF_MESSAGE_VIEW = 0;
	private final static int VIEWTYPE_RECIEVED_MESSAGE_VIEW = 1;
	private static final int DEFAULT_NUM_MESSAGES_TO_LOAD = 30;
	private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	private ListView mMessagesList;
	private TextView mComposeBox;
	private View mSendButton;
	private MessagesAdaptor mAdapter;
	private List<OPMessage> mMessages;
	private LinearLayout usersContainer;

	private OPIdentityContact mSelfContact;
	private OPConversationThread mConvThread;
	private long mWindowId;
	private OPSession mSession;

	public static ChatFragment newInstance(long[] userIdList) {
		ChatFragment fragment = new ChatFragment();
		Bundle args = new Bundle();
		args.putLongArray(IntentData.ARG_PEER_USER_IDS, userIdList);
		fragment.setArguments(args);
		return fragment;
	}

	public static ChatFragment newInstance(String peerContactId) {
		ChatFragment fragment = new ChatFragment();
		Bundle args = new Bundle();
		args.putString(IntentData.ARG_PEER_CONTACT_ID, peerContactId);
		fragment.setArguments(args);
		return fragment;
	}

	public static ChatFragment newTestInstance() {
		ChatFragment fragment = new ChatFragment();
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		long[] userIDs = args.getLongArray(IntentData.ARG_PEER_USER_IDS);
		mWindowId = OPChatWindow.getWindowId(userIDs);
		mSession = OPSessionManager.getInstance().getSessionForUsers(userIDs);
		if (mSession == null) {
			// this is user intiiated session
			List<OPUser> users = OPDataManager.getDatastoreDelegate().getUsers(userIDs);
			mSession = new OPSession(users);
			OPSessionManager.getInstance().addSession(mSession);
		}
		// mPeerContact =
		// OPDataManager.getDatastoreDelegate().getIdentityContact(
		// args.getString(IntentData.ARG_PEER_CONTACT_ID));
		mSelfContact = OPDataManager.getInstance().getSelfContacts().get(0);
		this.setHasOptionsMenu(true);
		// mSelfContact =
		// OPDataManager.getDatastoreDelegate().getIdentityContact(
		// mSelfContactId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = inflater.inflate(R.layout.fragment_chat, null);
		return setupView(view);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSession.setWindowAttached(true);
		OPDataManager.getDatastoreDelegate().markMessagesRead(mWindowId);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSession.setWindowAttached(false);
	}

	void updateUsersView(List<OPUser> users) {
		int width = (int) (getActivity().getResources().getDisplayMetrics().density * 50);
		this.usersContainer.removeAllViews();
		for (OPUser user : users) {
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);
			ImageView view = (ImageView) LayoutInflater.from(getActivity()).inflate(R.layout.imageview_profile, null);
			usersContainer.addView(view, lp);
			if (user.getAvatarUri() != null) {
				Picasso.with(getActivity())
						.load(user.getAvatarUri())
						.into(view);
			}
		}
	}

	View setupView(View view) {
		View emptyView = view.findViewById(R.id.empty_view);
		mMessagesList = (ListView) view.findViewById(R.id.listview);
		mMessagesList.setEmptyView(emptyView);
		mAdapter = new MessagesAdaptor(getActivity(), null);
		mMessagesList.setAdapter(mAdapter);
		View layout = view.findViewById(R.id.layout_compose);
		mComposeBox = (TextView) layout.findViewById(R.id.text);
		mSendButton = layout.findViewById(R.id.send);
		usersContainer = (LinearLayout) view.findViewById(R.id.users_container);

		updateUsersView(mSession.getParticipants());
		mSendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.d("TODO", "call actual send function");
				if (mComposeBox.getText() == null
						|| mComposeBox.getText().length() == 0) {
					return;
				}

				String messageId = java.util.UUID.randomUUID().toString();
				// we use 0 for home user
				OPMessage msg = new OPMessage(0,
						OPMessageType.TYPE_TEXT, mComposeBox.getText()
								.toString(), System.currentTimeMillis(), messageId);
				msg.setRead(true);

				mComposeBox.setText("");

				mSession.sendMessage(msg, false);
			}
		});
		getLoaderManager().initLoader(URL_LOADER, null, this);

		return view;
	}

	private List<OPMessage> getMessages() {
		if (mMessages == null) {
			mMessages = new ArrayList<OPMessage>();
			// TODO: do some setup
		}
		return mMessages;
	}

	class MessagesAdaptor extends CursorAdapter {

		public MessagesAdaptor(Context context, Cursor c) {
			super(context, c);
			// TODO Auto-generated constructor stub
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return super.getItem(position);
		}

		@Override
		public int getItemViewType(int position) {
			Cursor cursor = (Cursor) getItem(position);
			long sender_id = cursor.getLong(cursor.getColumnIndex(MessageEntry.COLUMN_NAME_SENDER_ID));
			if (sender_id == 0) {
				return 0;
			}
			return 1;

		}

		public int getItemViewType(Cursor cursor) {
			long sender_id = cursor.getLong(cursor.getColumnIndex(MessageEntry.COLUMN_NAME_SENDER_ID));
			if (sender_id == 0) {
				return 0;
			}
			return 1;

		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 2;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			OPMessage message = OPMessage.fromCursor(cursor);
			((ViewHolder) view.getTag()).update(message);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup arg2) {
			int viewType = getItemViewType(cursor);
			View view = null;
			switch (viewType) {
			case VIEWTYPE_SELF_MESSAGE_VIEW:
				view = View.inflate(getActivity(),
						R.layout.item_message_self, null);
				break;
			case VIEWTYPE_RECIEVED_MESSAGE_VIEW:
				view = View.inflate(getActivity(),
						R.layout.item_message_peer, null);

				break;
			}
			ViewHolder viewHolder = new ViewHolder(view, viewType);
			view.setTag(viewHolder);
			return view;
		}
	}

	class ViewHolder {
		ImageView avatarView;
		TextView title;

		TextView time;
		TextView text;
		int viewType;

		public ViewHolder(View view, int viewType) {
			title = (TextView) view.findViewById(R.id.user);
			avatarView = (ImageView) view.findViewById(R.id.avatar);
			text = (TextView) view.findViewById(R.id.message);
			time = (TextView) view.findViewById(R.id.time);

			this.viewType = viewType;
		}

		void update(OPMessage data) {
			switch (viewType) {
			case VIEWTYPE_SELF_MESSAGE_VIEW:
				Picasso.with(getActivity())
						.load(mSelfContact.getDefaultAvatarUrl())
						.into(avatarView);
				break;
			case VIEWTYPE_RECIEVED_MESSAGE_VIEW:
				OPUser sender = mSession.getUserBySenderId(data.getSenderId());
				Picasso.with(getActivity())
						.load(sender.getAvatarUri())
						.into(avatarView);

				break;
			}

			String avatar = null;
			if (data.getSenderId() == 0) {
				// self
				// avatar =
				// OPDataManager.getInstance().getSelfContacts().get(0).getDefaultAvatarUrl();
			} else {
				OPUser user = mSession.getUserBySenderId(data.getSenderId());
				if (user != null) {
					avatar = user.getAvatarUri();
					if (user.getName() != null) {
						title.setText(user.getName());
					}
				}
			}
			if (avatar != null) {
				Picasso.with(getActivity())
						.load(avatar)
						.into(avatarView);
			}

			time.setText(DateFormatUtils.getSameDayTime(data.getTime().toMillis(true)));
			text.setText(data.getMessage());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_chat, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_call:
			// makeCall(mPeerContact);
			return true;
		case R.id.menu_add:
			addParticipant();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDone(List<Long> userIds) {

		long ids[] = new long[userIds.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = userIds.get(i);
		}
		List<OPUser> users = OPDataManager.getDatastoreDelegate().getUsers(ids);
		Log.d("test", "ChatFragment user counts " + users.size());
		if (users != null) {
			mSession.addParticipant(users);
			updateUsersView(mSession.getParticipants());
			mWindowId = mSession.getCurrentWindowId();
			LoaderManager.enableDebugLogging(true);
			getLoaderManager().restartLoader(URL_LOADER, null, this);
		}
	}

	// After adding a new participant we'll have to switch chat window
	private void addParticipant() {
		ProfilePickerFragment fragment = new ProfilePickerFragment();
		fragment.setTargetFragment(this, 0);
		((BaseActivity) this.getActivity()).switchFragment(fragment);

		//		if (BuildConfig.DEBUG) {
		//			Toast.makeText(getActivity(), "TODO: group chat is not supported yet", Toast.LENGTH_LONG);
		//			//			return;
		//		}
		//		Cursor cursor = getActivity().getContentResolver().query(DatabaseContracts.ContactsViewEntry.CONTENT_URI, null,
		//				ContactsViewEntry.COLUMN_NAME_CONTACT_NAME + "=?", new String[] { "David Gotwo" }, null);
		//		if (cursor != null) {
		//			cursor.moveToFirst();
		//			OPUser user = OPUser.fromDetailCursor(cursor);
		//			Log.d("test", "loaded user " + user.getName());
		//			List<OPUser> users = new ArrayList<OPUser>();
		//			users.add(user);
		//			mSession.addParticipant(users);
		//			mWindowId = mSession.getCurrentWindowId();
		//			LoaderManager.enableDebugLogging(true);
		//			getLoaderManager().restartLoader(URL_LOADER, null, this);
		//		}
	}

	private void makeCall(OPIdentityContact peerContact) {
		Toast.makeText(getActivity(),
				"makeCall to be implmented" + peerContact, Toast.LENGTH_LONG);
	}

	// Begin: CursorCallback implementation
	private static final int URL_LOADER = 0;

	// static final String LIST_PROJECTION[] = { BaseColumns._ID,
	// MessageEntry.COLUMN_NAME_MESSAGE_ID,
	// MessageEntry.COLUMN_NAME_MESSAGE_TEXT,
	// MessageEntry.COLUMN_NAME_SENDER_ID,
	// MessageEntry.COLUMN_NAME_WINDOW_ID };

	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle arg1) {
		switch (loaderID) {
		case URL_LOADER:
			// Returns a new CursorLoader
			return new CursorLoader(
					getActivity(), // Parent activity context
					Uri.parse(DatabaseContracts.MessageEntry.CONTENT_ID_URI_BASE + "window/" + mWindowId),
					null, // Projection to return
					null, // No selection clause
					null, // No selection arguments
					null // Default sort order
			);
		default:
			// An invalid id was passed in
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d("test", "ChatFragment onLoadFinished" + cursor);
		mAdapter.changeCursor(cursor);
		mMessagesList.setSelection(mMessagesList.getCount() - 1);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}
	// End: CursorCallback implementation

}