package net.bible.service.format.osistohtml.taghandler;

import net.bible.service.common.Constants.HTML;
import net.bible.service.common.Logger;
import net.bible.service.format.osistohtml.HtmlTextWriter;
import net.bible.service.format.osistohtml.OsisToHtmlParameters;
import net.bible.service.format.osistohtml.osishandlers.OsisToHtmlSaxHandler.VerseInfo;

import org.crosswire.jsword.book.OSISUtil;
import org.xml.sax.Attributes;

/** Write the verse number at the beginning of a verse
 * Also handle verse per line
 * 
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author. 
 */
public class VerseHandler implements OsisTagHandler {

	private BookmarkMarker bookmarkMarker;
	
	private MyNoteMarker myNoteMarker;
	
	private OsisToHtmlParameters parameters;
	
	private VerseInfo verseInfo; 
	
	private int writerRollbackPosition;
	
	private HtmlTextWriter writer;
	
	@SuppressWarnings("unused")
	private static final Logger log = new Logger("VerseHandler");

	public VerseHandler(OsisToHtmlParameters parameters, VerseInfo verseInfo, BookmarkMarker bookmarkMarker, MyNoteMarker myNoteMarker, HtmlTextWriter writer) {
		this.parameters = parameters;
		this.verseInfo = verseInfo;
		this.bookmarkMarker = bookmarkMarker;
		this.myNoteMarker = myNoteMarker;
		this.writer = writer;
	}
	
	
	@Override
	public String getTagName() {
        return OSISUtil.OSIS_ELEMENT_VERSE;
    }

	@Override
	public void start(Attributes attrs) {
		writerRollbackPosition = writer.getPosition();
		
		if (attrs!=null) {
			verseInfo.currentVerseNo = TagHandlerHelper.osisIdToVerseNum(attrs.getValue("", OSISUtil.OSIS_ATTR_OSISID));
		} else {
			verseInfo.currentVerseNo++;
		}

		if (parameters.isVersePerline()) {
			//close preceding verse
			if (verseInfo.currentVerseNo>1) {
				writer.write("</div>");
			}
			// start current verse
			writer.write("<div>");
		}

		writeVerse(verseInfo.currentVerseNo);

		// initialise other related handlers that write content at start of verse
		bookmarkMarker.start(attrs);
		myNoteMarker.start(attrs);
		
		// record that we are into a new verse
		verseInfo.isTextSinceVerse = false;
	}

	@Override
	public void end() {
		// these related handlers currently do nothing on end
		myNoteMarker.end();
		bookmarkMarker.end();

		if (!verseInfo.isTextSinceVerse) {
			writer.removeAfter(writerRollbackPosition);
		}
	}

	private void writeVerse(int verseNo) {
		verseInfo.positionToInsertBeforeVerse = writer.getPosition();
		
		// The id is used to 'jump to' the verse using javascript so always need the verse tag with an id
		// Do not show verse 0
		StringBuilder verseHtml = new StringBuilder();
		if (parameters.isShowVerseNumbers() && verseNo!=0) {
			verseHtml.append(" <span class='verseNo' id='").append(verseNo).append("'>").append(verseNo).append("</span>").append(HTML.NBSP);
		} else {
			// we really want an empty span but that is illegal and causes problems such as incorrect verse calculation in Psalms
			// so use something that will hopefully interfere as little as possible - a zero-width-space
			// also put a space before it to allow a separation from the last word of previous verse or to be ignored if start of line
			verseHtml.append(" <span class='verseNo' id='").append(verseNo).append("'/>&#x200b;</span>");
		}
		writer.write(verseHtml.toString());
	}
}
