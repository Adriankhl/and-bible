package net.bible.service.db.bookmark;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.bible.service.db.CommonDatabaseHelper;
import net.bible.service.db.SQLHelper;
import net.bible.service.db.bookmark.BookmarkDatabaseDefinition.BookmarkColumn;
import net.bible.service.db.bookmark.BookmarkDatabaseDefinition.BookmarkLabelColumn;
import net.bible.service.db.bookmark.BookmarkDatabaseDefinition.LabelColumn;
import net.bible.service.db.bookmark.BookmarkDatabaseDefinition.Table;

import org.apache.commons.lang.StringUtils;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.VerseFactory;
import org.crosswire.jsword.passage.VerseKey;
import org.crosswire.jsword.versification.BibleBook;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author.
 */
public class BookmarkDBAdapter {

	// Variable to hold the database instance
	private SQLiteDatabase db;

	// Database open/upgrade helper
	private SQLiteOpenHelper dbHelper;
	
	private static final String TAG = "BookmarkDBAdapter";

	public BookmarkDBAdapter() {
		dbHelper =  CommonDatabaseHelper.getInstance(); 
	}

	public BookmarkDBAdapter open() throws SQLException {
		try {
			db = dbHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			db = dbHelper.getReadableDatabase();
		}
		return this;
	}

	public void close() {
		db.close();
	}

	public BookmarkDto insertBookmark(BookmarkDto bookmark) {
		// Create a new row of values to insert.
		ContentValues newValues = new ContentValues();
		Key key = bookmark.getVerse();
		String v11nName="";
		if (key instanceof VerseKey) {
			// must save a VerseKey's versification along with the key!
			v11nName = ((VerseKey<?>) key).getVersification().getName();
		}
		
		// Gets the current system time in milliseconds
        Long now = Long.valueOf(System.currentTimeMillis());
        
		newValues.put(BookmarkColumn.KEY, key.getOsisID());
		newValues.put(BookmarkColumn.VERSIFICATION, v11nName);
		newValues.put(BookmarkColumn.CREATED_ON, now);

		long newId = db.insert(Table.BOOKMARK, null, newValues);
		BookmarkDto newBookmark = getBookmarkDto(newId);
		return newBookmark;
	}

	public boolean removeBookmark(BookmarkDto bookmark) {
		Log.d(TAG, "Removing bookmark:"+bookmark.getVerse());
		return db.delete(Table.BOOKMARK, BookmarkColumn._ID + "=" + bookmark.getId(), null) > 0;
	}

	public boolean removeLabel(LabelDto label) {
		Log.d(TAG, "Removing label:"+label.getName());
		return db.delete(Table.LABEL, LabelColumn._ID + "=" + label.getId(), null) > 0;
	}

	public LabelDto insertLabel(LabelDto label) {
		// Create a new row of values to insert.
		ContentValues newValues = new ContentValues();
		newValues.put(LabelColumn.NAME, label.getName());

		long newId = db.insert(Table.LABEL, null, newValues);
		LabelDto newLabel = getLabelDto(newId);
		return newLabel;
	}

	public LabelDto updateLabel(LabelDto label) {
		// Create a new row of values to insert.
		ContentValues newValues = new ContentValues();
		newValues.put(LabelColumn.NAME, label.getName());

		long newId = db.update(Table.LABEL, newValues, "_id=?", new String []{String.valueOf(label.getId())});
		LabelDto newLabel = getLabelDto(newId);
		return newLabel;
	}

	public boolean removeBookmarkLabelJoin(BookmarkDto bookmark, LabelDto label) {
		return db.delete(Table.BOOKMARK_LABEL, BookmarkLabelColumn.BOOKMARK_ID + "=" + bookmark.getId()+" AND "+BookmarkLabelColumn.LABEL_ID + "=" + label.getId(), null) > 0;
	}

	public List<BookmarkDto> getAllBookmarks() {
		List<BookmarkDto> allBookmarks = new ArrayList<BookmarkDto>();
		Cursor c = db.query(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS, null, null, null, null, null);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		    		BookmarkDto bookmark = getBookmarkDto(c);
		    		allBookmarks.add(bookmark);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
        return allBookmarks;
	}

	public List<BookmarkDto> getBookmarksInBook(BibleBook book) {
		Log.d(TAG, "about to getBookmarksInPassage:"+book.getOSIS());
		List<BookmarkDto> bookmarkList = new ArrayList<BookmarkDto>();
		//av11n TODO may need to map between different versifications here
		Cursor c = db.query(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS, BookmarkColumn.KEY+" LIKE ?", new String []{String.valueOf(book.getOSIS()+".%")}, null, null, null);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		        	BookmarkDto bookmark = getBookmarkDto(c);
		    		bookmarkList.add(bookmark);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
		Log.d(TAG, "bookmarksInPassage set to " + bookmarkList.size() + " item long list");
        return bookmarkList;
	}

	public List<BookmarkDto> getBookmarksWithLabel(LabelDto label) {
		String sql = "SELECT "+SQLHelper.getColumnsForQuery(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS)+
					 " FROM bookmark "+
					 "JOIN bookmark_label ON (bookmark._id = bookmark_label.bookmark_id) "+
					 "JOIN label ON (bookmark_label.label_id = label._id) "+
					 "WHERE label._id = ? ";
		
		List<BookmarkDto> allBookmarks = new ArrayList<BookmarkDto>();
		String[] args = new String[] {label.getId().toString()};
		Cursor c = db.rawQuery(sql, args);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		    		BookmarkDto bookmark = getBookmarkDto(c);
		    		allBookmarks.add(bookmark);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
        return allBookmarks;
	}

	public List<BookmarkDto> getUnlabelledBookmarks() {
		String sql = "SELECT "+SQLHelper.getColumnsForQuery(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS)+
					 " FROM bookmark "+
					 " WHERE NOT EXISTS (SELECT * FROM bookmark_label WHERE bookmark._id = bookmark_label.bookmark_id)";
		
		List<BookmarkDto> bookmarks = new ArrayList<BookmarkDto>();
		Cursor c = db.rawQuery(sql, null);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		    		BookmarkDto bookmark = getBookmarkDto(c);
		    		bookmarks.add(bookmark);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
        return bookmarks;
	}


	public List<LabelDto> getAllLabels() {
		List<LabelDto> allLabels = new ArrayList<LabelDto>();
		Cursor c = db.query(LabelQuery.TABLE, LabelQuery.COLUMNS, null, null, null, null, LabelColumn.NAME);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		    		LabelDto bookmark = getLabelDto(c);
		    		allLabels.add(bookmark);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
        return allLabels;
	}

	public List<LabelDto> getBookmarkLabels(BookmarkDto bookmark) {
		String sql = "SELECT label._id, label.name "+
					 "FROM label "+
					 "JOIN bookmark_label ON (label._id = bookmark_label.label_id) "+
					 "JOIN bookmark ON (bookmark_label.bookmark_id = bookmark._id) "+
					 "WHERE bookmark._id = ?";
		
		List<LabelDto> labels = new ArrayList<LabelDto>();
		String[] args = new String[] {bookmark.getId().toString()};
		Cursor c = db.rawQuery(sql, args);
		try {
			if (c.moveToFirst()) {
		        while (!c.isAfterLast()) {
		    		LabelDto label = getLabelDto(c);
		    		labels.add(label);
		       	    c.moveToNext();
		        }
			}
		} finally {
	        c.close();
		}
        
        return labels;
	}

	public void insertBookmarkLabelJoin(BookmarkDto bookmark, LabelDto label) {
		// Create a new row of values to insert.
		ContentValues newValues = new ContentValues();
		newValues.put(BookmarkLabelColumn.BOOKMARK_ID, bookmark.getId());
		newValues.put(BookmarkLabelColumn.LABEL_ID, label.getId());

		//long newId = 
		db.insert(Table.BOOKMARK_LABEL, null, newValues);
	}

	public BookmarkDto getBookmarkDto(long id) {
		BookmarkDto bookmark = null;
		
		Cursor c = db.query(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS, BookmarkColumn._ID+"=?", new String[] {String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				bookmark = getBookmarkDto(c);
			}
		} finally {
			c.close();
		}
		
		return bookmark;
	}

	public BookmarkDto getBookmarkByKey(String key) {
		BookmarkDto bookmark = null;
		
		Cursor c = db.query(BookmarkQuery.TABLE, BookmarkQuery.COLUMNS, BookmarkColumn.KEY+"=?", new String[] {key}, null, null, null);
		try {
			if (c.moveToFirst()) {
				bookmark = getBookmarkDto(c);
			}
		} finally {
			c.close();
		}
		
		return bookmark;
	}
	
	/** return Dto from current cursor position or null
	 * @param c
	 * @return
	 * @throws NoSuchKeyException
	 */
	private BookmarkDto getBookmarkDto(Cursor c) {
		BookmarkDto dto = new BookmarkDto();
		try {
			//Id
			Long id = c.getLong(BookmarkQuery.ID);
			dto.setId(id);
			
			//Verse
			String key = c.getString(BookmarkQuery.KEY);
			Versification v11n=null;
			if (!c.isNull(BookmarkQuery.VERSIFICATION)) {
				String v11nString = c.getString(BookmarkQuery.VERSIFICATION);
				if (!StringUtils.isEmpty(v11nString)) {
					v11n = Versifications.instance().getVersification(v11nString);
				}
			}
			if (v11n==null) {
				// use default v11n
				v11n = Versifications.instance().getVersification(Versifications.DEFAULT_V11N);
			}
			dto.setVerse(VerseFactory.fromString(v11n, key));

			//Created date
			long created = c.getLong(BookmarkQuery.CREATED_ON);
			dto.setCreatedOn(new Date(created));
		
		} catch (NoSuchKeyException nke) {
			Log.e(TAG, "Key error", nke);
		}
		
		return dto;
	}

	public LabelDto getLabelDto(long id) {
		LabelDto label = null;
		
		Cursor c = db.query(LabelQuery.TABLE, LabelQuery.COLUMNS, LabelColumn._ID+"=?", new String[] {String.valueOf(id)}, null, null, null);
		try {
			if (c.moveToFirst()) {
				label = getLabelDto(c);
			}
		} finally {
			c.close();
		}
		
		return label;
	}

	/** return Dto from current cursor position or null
	 * @param c
	 * @return
	 * @throws NoSuchKeyException
	 */
	private LabelDto getLabelDto(Cursor c) {
		LabelDto dto = new LabelDto();

		Long id = c.getLong(LabelQuery.ID);
		dto.setId(id);
		
		String name = c.getString(LabelQuery.NAME);
		dto.setName(name);
		
		return dto;
	}
	
	private interface BookmarkQuery {
        final String TABLE = Table.BOOKMARK;

		final String[] COLUMNS = new String[] {BookmarkColumn._ID, BookmarkColumn.KEY, BookmarkColumn.VERSIFICATION, BookmarkColumn.CREATED_ON};

        final int ID = 0;
        final int KEY = 1;
        final int VERSIFICATION = 2;
        final int CREATED_ON = 3;
    }
	private interface LabelQuery {
        final String TABLE = Table.LABEL;

		final String[] COLUMNS = new String[] {LabelColumn._ID, LabelColumn.NAME};

        final int ID = 0;
        final int NAME = 1;
    }
}
