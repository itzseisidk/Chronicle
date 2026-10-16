package net.chronicle.ui.modern;

import com.mojang.blaze3d.platform.InputConstants;
import net.chronicle.compat.InputScreen;
import java.util.ArrayList;
import java.util.List;
import net.chronicle.compat.CScreen;
import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.Category;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.Filter;
import net.chronicle.journal.ItemIcons;
import net.chronicle.journal.Journal;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.journal.Nav;
import net.chronicle.journal.Notes;
import net.chronicle.ui.JournalScreens;
import net.chronicle.ui.modern.widget.MButton;
import net.chronicle.ui.modern.widget.MCardList;
import net.chronicle.ui.modern.widget.MChip;
import net.chronicle.ui.modern.widget.MModal;
import net.chronicle.ui.modern.widget.MTextField;
import net.chronicle.ui.modern.widget.MWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ModernJournalScreen extends CScreen implements MCardList.Actions {
	private final Screen parent;
	private final Anim.Clock clock = new Anim.Clock();
	private final Anim intro = new Anim(0, 12);
	private final List<MWidget> widgets = new ArrayList<>();
	private final List<MChip> chips = new ArrayList<>();

	private Filter filter = Filter.ALL;
	private String search = "";
	private int seenRevision = -1;
	private MModal modal;
	private boolean openNoteOnInit;

	private MTextField searchField;
	private MCardList list;
	private int px, py, pw, ph, sidebarW, segX;

	public ModernJournalScreen(Screen parent) {
		super(Component.translatable("chronicle.title"));
		this.parent = parent;
	}

	public static ModernJournalScreen withNewNote(Screen parent) {
		ModernJournalScreen screen = new ModernJournalScreen(parent);
		screen.openNoteOnInit = true;
		return screen;
	}

	@Override
	protected void init() {
		widgets.clear();
		chips.clear();
		pw = Math.min(width - 16, 700);
		ph = Math.min(height - 16, 420);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		sidebarW = pw >= 440 ? 124 : 28;
		boolean compact = sidebarW < 100;

		// header, right to left
		int hx = px + pw - 8;
		MButton close = add(MButton.icon("✕", Component.translatable("gui.done"), this::onClose).bounds(hx - 18, py + 7, 18, 18));
		MButton settings = add(MButton.icon("⚙", Component.translatable("chronicle.settings.title"), () -> openModal(new SettingsModal()))
				.bounds(close.x - 22, py + 7, 18, 18));
		int segW = 56;
		// already here, does nothing
		MButton modern = add(new MButton(Component.translatable("chronicle.style.modern"), null, MButton.Style.GHOST, () -> { }));
		modern.bounds(settings.x - 8 - segW, py + 7, segW, 18);
		modern.active = true;
		MButton vanilla = add(new MButton(Component.translatable("chronicle.style.vanilla"), null, MButton.Style.GHOST,
				() -> JournalScreens.switchStyle(ChronicleConfig.UiStyle.VANILLA, parent)));
		vanilla.bounds(modern.x - segW, py + 7, segW, 18);
		vanilla.tooltip = Component.translatable("chronicle.style.switch_tooltip");
		segX = vanilla.x;

		// filters on the left
		int sx = px + 8;
		int sy = py + 36;
		for (Filter f : Filter.values()) {
			MChip chip = new MChip(f, () -> {
				Journal j = JournalManager.get();
				return j == null ? 0 : f.count(j);
			}, () -> setFilter(f));
			chip.compact = compact;
			chip.selected = f == filter;
			chip.bounds(sx, sy, sidebarW, 18);
			chips.add(chip);
			widgets.add(chip);
			sy += f == Filter.PINNED ? 26 : 20; // gap for the divider
		}

		int mx0 = px + 8 + sidebarW + 10;
		int mw = px + pw - 8 - mx0;
		int noteW = mw >= 260 ? 84 : 22; // just the plus when there's not much room
		searchField = add(new MTextField("⌕").bounds(mx0, py + 36, mw - noteW - 6, 20));
		searchField.placeholder(Component.translatable("chronicle.search_hint")).maxLength(64).onChange(s -> {
			search = s;
			refresh(true);
		});
		searchField.setValue(search);
		MButton note = add(new MButton(noteW > 30 ? Component.translatable("chronicle.button.new_note") : null, "✚", MButton.Style.PRIMARY,
				() -> openModal(new NoteModal(null))).bounds(mx0 + mw - noteW, py + 36, noteW, 20));
		note.tooltip = noteW > 30 ? null : Component.translatable("chronicle.button.new_note");

		list = add(new MCardList(this).bounds(mx0, py + 62, mw, ph - 62 - 8));
		list.emptyTitle = Component.translatable("chronicle.empty.title");
		list.emptyHint = Component.translatable("chronicle.empty.hint");
		refresh(false);

		if (modal != null) modal.layout(width, height);
		if (openNoteOnInit) {
			openNoteOnInit = false;
			openModal(new NoteModal(null));
		}
	}

	private <T extends MWidget> T add(T widget) {
		widgets.add(widget);
		return widget;
	}

	private void setFilter(Filter f) {
		filter = f;
		for (MChip chip : chips) chip.selected = chip.filter == f;
		refresh(true);
	}

	private void refresh(boolean resetScroll) {
		seenRevision = JournalManager.revision();
		list.setEntries(Filter.query(JournalManager.get(), filter, search));
		if (resetScroll) list.scrollToTop();
	}

	private void openModal(MModal m) {
		for (MWidget w : widgets) w.setFocused(false);
		modal = m;
		modal.layout(width, height);
	}

	@Override
	public void tick() {
		if (seenRevision != JournalManager.revision()) refresh(false);
	}

	@Override
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
		vanillaBackground(g, mouseX, mouseY, partialTick);
		g.gradient(0, 0, width, height, Draw.alpha(Theme.BACKDROP_TOP, intro.get()), Draw.alpha(Theme.BACKDROP_BOTTOM, intro.get()));
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		float dt = clock.tick();
		intro.set(1);
		float in = intro.update(dt);
		int offset = Math.round((1 - in) * 12);
		boolean blocked = modal != null;
		int mx = blocked ? -1 : mouseX;
		int my = blocked ? -1 : mouseY - offset;

		g.push();
		g.translate(0, offset);
		Draw.shadow(g, px, py, pw, ph, 10, 10, in);
		Draw.panel(g, px, py, pw, ph, 10, Theme.PANEL, Theme.PANEL_BORDER);
		drawHeader(g);
		drawSidebar(g);
		for (MWidget w : widgets) w.render(g, mx, my, dt);
		g.pop();

		MWidget hovered = null;
		for (MWidget w : widgets) {
			if (w.tooltip != null && w.contains(mx, my)) hovered = w;
		}
		MButton cardButton = list.hoveredButton(mx, my);
		if (cardButton != null && cardButton.tooltip != null) hovered = cardButton;
		if (hovered != null && !(hovered instanceof MChip chip && !chip.compact)) {
			g.tooltip(font, hovered.tooltip, mouseX, mouseY);
		}

		if (modal != null) {
			g.layer();
			modal.render(g, mouseX, mouseY, dt, width, height);
			if (modal.isClosed()) {
				modal = null;
				refresh(false);
			}
		}
	}

	private void drawHeader(Gfx g) {
		Draw.circle(g, px + 20, py + 16, 10, Theme.ACCENT_SOFT);
		g.item(ItemIcons.stack("minecraft:writable_book"), px + 12, py + 8);
		Journal j = JournalManager.get();
		g.text(font, title.copy().withStyle(ChatFormatting.BOLD), px + 36, py + 7, Theme.TEXT, false);
		String world = j == null ? "" : j.worldName;
		int maxW = pw - 36 - 176; // room up to the buttons on the right
		String worldText = Draw.ellipsize(font, world, maxW);
		g.text(font, worldText, px + 36, py + 18, Theme.TEXT_MUTED, false);
		g.fill(px + 1, py + 30, px + pw - 1, py + 31, Theme.DIVIDER);
		Draw.round(g, segX - 2, py + 5, 56 * 2 + 4, 22, 6, Theme.SURFACE);
		int textRight = px + 36 + Math.max(font.width(title.copy().withStyle(ChatFormatting.BOLD)), font.width(worldText));
		drawTargetPill(g, textRight + 12, segX - 10);
	}

	private void drawSidebar(Gfx g) {
		int sx = px + 8;
		int dividerY = py + 36 + 2 * 20 + 2;
		g.fill(sx + 4, dividerY, sx + sidebarW - 4, dividerY + 1, Theme.DIVIDER);
		g.fill(px + 8 + sidebarW + 4, py + 36, px + 8 + sidebarW + 5, py + ph - 8, Theme.DIVIDER);

		Journal j = JournalManager.get();
		if (j == null || sidebarW < 100) return;
		int statsY = py + 36 + Filter.values().length * 20 + 14;
		if (statsY + 34 > py + ph - 8) return;
		stat(g, sx + 6, statsY, "⚑", Component.translatable("chronicle.stats.biomes", j.biomes.size()), Category.BIOME.color);
		stat(g, sx + 6, statsY + 12, "✦", Component.translatable("chronicle.stats.items", j.items.size()), Category.ITEM.color);
		stat(g, sx + 6, statsY + 24, "☠", Component.translatable("chronicle.stats.deaths", j.deaths), Category.DEATH.color);
	}

	private void stat(Gfx g, int x, int y, String icon, Component text, int color) {
		g.text(font, icon, x, y, color, false);
		g.text(font, Draw.ellipsize(font, text, sidebarW - 20), x + 12, y, Theme.TEXT_MUTED, false);
	}

	// current target in the header, only if there's enough room
	private void drawTargetPill(Gfx g, int left, int right) {
		Journal j = JournalManager.get();
		JournalEntry target = j == null ? null : j.target();
		if (target == null || right - left < 80) return;
		var player = minecraft.player;
		Nav nav = player == null ? null : Nav.of(target, player);
		String text = "◎ " + target.title().getString() + (nav == null ? "" : " · " + EntryText.distance(nav.distance()));
		text = Draw.ellipsize(font, text, right - left - 16);
		int w = font.width(text) + 16;
		int x = right - w;
		Draw.round(g, x, py + 8, w, 16, 8, Draw.alpha(Theme.CYAN, 0.14f));
		g.text(font, text, x + 8, py + 12, Theme.CYAN, false);
	}

	@Override
	protected boolean onMouseDown(double mx, double my, int button, boolean doubleClick) {
		if (modal != null) return modal.mouseDown(mx, my, button);
		boolean hit = false;
		for (MWidget w : widgets) {
			if (!hit && w.contains(mx, my)) {
				w.setFocused(w.focusable());
				w.mouseDown(mx, my, button);
				hit = true;
			} else {
				w.setFocused(false);
			}
		}
		return true;
	}

	@Override
	protected boolean onMouseUp(double mx, double my, int button) {
		if (modal != null) return modal.mouseUp(mx, my, button);
		for (MWidget w : widgets) w.mouseUp(mx, my, button);
		return true;
	}

	@Override
	protected boolean onMouseDrag(double mx, double my, int button, double dx, double dy) {
		if (modal != null) return modal.mouseDrag(mx, my, button, dx, dy);
		for (MWidget w : widgets) {
			if (w.mouseDrag(mx, my, button, dx, dy)) return true;
		}
		return true;
	}

	@Override
	protected boolean onScroll(double mx, double my, double horizontal, double vertical) {
		if (modal != null) return modal.scroll(mx, my, vertical);
		for (MWidget w : widgets) {
			if (w.scroll(mx, my, vertical)) return true;
		}
		return true;
	}

	@Override
	protected boolean onKey(int key, int modifiers) {
		if (modal != null) return modal.key(key, modifiers);
		boolean ctrl = (modifiers & InputScreen.CTRL) != 0;
		if (searchField.isFocused()) {
			if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN) {
				searchField.setFocused(false);
				return true;
			}
			return searchField.key(key, modifiers);
		}
		if (ctrl && key == InputConstants.KEY_F) {
			searchField.setFocused(true);
			return true;
		}
		if (ctrl && key == InputConstants.KEY_N) {
			openModal(new NoteModal(null));
			return true;
		}
		return false;
	}

	@Override
	protected boolean onChar(int codepoint) {
		if (modal != null) return modal.charTyped(codepoint);
		if (searchField.isFocused()) return searchField.charTyped(codepoint);
		return false;
	}

	@Override
	public void onClose() {
		Platform.setScreen(parent);
	}

	@Override
	public void target(JournalEntry entry) {
		Notes.toggleTarget(entry);
	}

	@Override
	public void pin(JournalEntry entry) {
		Notes.togglePin(entry);
	}

	@Override
	public void copy(JournalEntry entry) {
		Notes.copyCoords(entry);
	}

	@Override
	public void edit(JournalEntry entry) {
		openModal(new NoteModal(entry));
	}

	@Override
	public void delete(JournalEntry entry) {
		openModal(new ConfirmModal(Component.translatable("chronicle.delete.title"),
				Component.translatable("chronicle.delete.message", entry.title()),
				Component.translatable("chronicle.button.delete"),
				() -> JournalManager.remove(entry)));
	}
}
