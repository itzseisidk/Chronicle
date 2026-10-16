package net.chronicle.ui.modern.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.chronicle.compat.InputScreen;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public abstract class MTextBase extends MWidget {
	protected String value = "";
	protected int cursor;
	protected int anchor;
	protected int maxLength = 256;
	protected Component placeholder = Component.empty();
	protected long blinkStart = System.currentTimeMillis();
	protected boolean selecting;
	private Consumer<String> onChange = s -> { };

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value.length() > maxLength ? value.substring(0, maxLength) : value;
		cursor = anchor = this.value.length();
		onTextChanged();
	}

	public MTextBase placeholder(Component placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public MTextBase maxLength(int maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	public MTextBase onChange(Consumer<String> onChange) {
		this.onChange = onChange;
		return this;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		blinkStart = System.currentTimeMillis();
	}

	protected boolean cursorVisible() {
		return focused && ((System.currentTimeMillis() - blinkStart) / 500) % 2 == 0;
	}

	protected int selStart() {
		return Math.min(cursor, anchor);
	}

	protected int selEnd() {
		return Math.max(cursor, anchor);
	}

	protected boolean hasSelection() {
		return cursor != anchor;
	}

	protected void onTextChanged() {
	}

	protected void moveTo(int pos, boolean select) {
		cursor = Math.max(0, Math.min(value.length(), pos));
		if (!select) anchor = cursor;
		blinkStart = System.currentTimeMillis();
	}

	protected void insert(String text) {
		text = filter(text);
		int start = selStart(), end = selEnd();
		int room = maxLength - (value.length() - (end - start));
		if (room <= 0 && text.length() > 0) return;
		if (text.length() > room) text = text.substring(0, room);
		value = value.substring(0, start) + text + value.substring(end);
		cursor = anchor = start + text.length();
		changed();
	}

	protected String filter(String text) {
		StringBuilder sb = new StringBuilder();
		text.codePoints().forEach(cp -> {
			if (cp == '\n' && allowNewlines()) sb.append('\n');
			else if (cp >= 32 && cp != 127 && cp != '§') sb.appendCodePoint(cp);
		});
		return sb.toString();
	}

	protected boolean allowNewlines() {
		return false;
	}

	private void deleteRange(int start, int end) {
		if (start == end) return;
		value = value.substring(0, start) + value.substring(end);
		cursor = anchor = start;
		changed();
	}

	private void changed() {
		blinkStart = System.currentTimeMillis();
		onTextChanged();
		onChange.accept(value);
	}

	protected int wordLeft(int pos) {
		while (pos > 0 && value.charAt(pos - 1) == ' ') pos--;
		while (pos > 0 && value.charAt(pos - 1) != ' ' && value.charAt(pos - 1) != '\n') pos--;
		return pos;
	}

	protected int wordRight(int pos) {
		int len = value.length();
		while (pos < len && value.charAt(pos) == ' ') pos++;
		while (pos < len && value.charAt(pos) != ' ' && value.charAt(pos) != '\n') pos++;
		return pos;
	}

	protected int lineStart(int pos) {
		return 0;
	}

	protected int lineEnd(int pos) {
		return value.length();
	}

	protected boolean vertical(int direction, boolean select) {
		return false;
	}

	@Override
	public boolean key(int key, int modifiers) {
		if (!focused) return false;
		boolean shift = (modifiers & InputScreen.SHIFT) != 0;
		boolean ctrl = (modifiers & InputScreen.CTRL) != 0;
		var keyboard = Minecraft.getInstance().keyboardHandler;
		switch (key) {
			case InputConstants.KEY_LEFT -> {
				if (hasSelection() && !shift) moveTo(selStart(), false);
				else moveTo(ctrl ? wordLeft(cursor) : cursor - 1, shift);
				return true;
			}
			case InputConstants.KEY_RIGHT -> {
				if (hasSelection() && !shift) moveTo(selEnd(), false);
				else moveTo(ctrl ? wordRight(cursor) : cursor + 1, shift);
				return true;
			}
			case InputConstants.KEY_UP -> {
				return vertical(-1, shift);
			}
			case InputConstants.KEY_DOWN -> {
				return vertical(1, shift);
			}
			case InputConstants.KEY_HOME -> {
				moveTo(ctrl ? 0 : lineStart(cursor), shift);
				return true;
			}
			case InputConstants.KEY_END -> {
				moveTo(ctrl ? value.length() : lineEnd(cursor), shift);
				return true;
			}
			case InputConstants.KEY_BACKSPACE -> {
				if (hasSelection()) deleteRange(selStart(), selEnd());
				else if (cursor > 0) deleteRange(ctrl ? wordLeft(cursor) : cursor - 1, cursor);
				return true;
			}
			case InputConstants.KEY_DELETE -> {
				if (hasSelection()) deleteRange(selStart(), selEnd());
				else if (cursor < value.length()) deleteRange(cursor, ctrl ? wordRight(cursor) : cursor + 1);
				return true;
			}
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				if (allowNewlines()) {
					insert("\n");
					return true;
				}
				return false;
			}
			default -> {
			}
		}
		if (ctrl) {
			switch (key) {
				case InputConstants.KEY_A -> {
					anchor = 0;
					cursor = value.length();
					return true;
				}
				case InputConstants.KEY_C -> {
					if (hasSelection()) keyboard.setClipboard(value.substring(selStart(), selEnd()));
					return true;
				}
				case InputConstants.KEY_X -> {
					if (hasSelection()) {
						keyboard.setClipboard(value.substring(selStart(), selEnd()));
						deleteRange(selStart(), selEnd());
					}
					return true;
				}
				case InputConstants.KEY_V -> {
					insert(keyboard.getClipboard());
					return true;
				}
				default -> {
				}
			}
		}
		// text comes in through charTyped, but still eat the key or E opens the inventory
		return key != InputConstants.KEY_ESCAPE && key != InputConstants.KEY_TAB;
	}

	@Override
	public boolean charTyped(int codepoint) {
		if (!focused) return false;
		insert(new String(Character.toChars(codepoint)));
		return true;
	}

	@Override
	public boolean mouseUp(double mx, double my, int button) {
		selecting = false;
		return false;
	}
}
