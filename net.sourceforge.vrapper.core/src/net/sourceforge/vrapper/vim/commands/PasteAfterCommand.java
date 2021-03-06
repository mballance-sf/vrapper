package net.sourceforge.vrapper.vim.commands;

import net.sourceforge.vrapper.platform.TextContent;
import net.sourceforge.vrapper.utils.ContentType;
import net.sourceforge.vrapper.utils.LineInformation;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.StringUtils;
import net.sourceforge.vrapper.utils.VimUtils;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.register.RegisterContent;

public class PasteAfterCommand extends CountAwareCommand {

    public static final PasteAfterCommand CURSOR_ON_TEXT = new PasteAfterCommand(false);
    public static final PasteAfterCommand CURSOR_AFTER_TEXT = new PasteAfterCommand(true);

    private boolean placeCursorAfter;
    
    private PasteAfterCommand(boolean placeCursorAfter) {
    	this.placeCursorAfter = placeCursorAfter;
    }

    @Override
    public void execute(EditorAdaptor editorAdaptor, int count) {
        if (!editorAdaptor.getFileService().isEditable()) {
            return;
        }
        if (count == NO_COUNT_GIVEN) {
            count = 1;
        }
        RegisterContent registerContent = editorAdaptor.getRegisterManager().getActiveRegister().getContent();
        String text = registerContent.getText();
        TextContent content = editorAdaptor.getModelContent();
        int offset = editorAdaptor.getPosition().getModelOffset();
        LineInformation line = content.getLineInformationOfOffset(offset);
        int lineNo = line.getNumber() + 1;
        int position;
        boolean linewise = registerContent.getPayloadType() == ContentType.LINES;
		if (linewise) {
            // FIXME: position calculation for count > 1
            if (lineNo < content.getNumberOfLines()) {
                offset = content.getLineInformation(lineNo).getBeginOffset();
                position = offset;
            } else {
                offset = content.getTextLength();
                String newLine = editorAdaptor.getConfiguration().getNewLine();
                text = newLine + VimUtils.stripLastNewline(text);
                position = offset + newLine.length();
            }
        } else {
            offset = Math.min(line.getEndOffset(), offset + 1);
            position = offset + text.length() * count;
            if (!placeCursorAfter || text.length() == 0)
            	position -= 1;
        }
        try {
            editorAdaptor.getHistory().beginCompoundChange();
            content.replace(offset, 0, StringUtils.multiply(text, count));
            int followingLine = lineNo + count;
            if (linewise && placeCursorAfter
				&& followingLine < content.getNumberOfLines()) {
	                position = content.getLineInformation(followingLine).getBeginOffset();
            }
            Position destination = editorAdaptor.getCursorService().newPositionForModelOffset(position);
            editorAdaptor.setPosition(destination, true);
        } finally {
            editorAdaptor.getHistory().endCompoundChange();
        }
    }

    @Override
    public CountAwareCommand repetition() {
        return this;
    }


}