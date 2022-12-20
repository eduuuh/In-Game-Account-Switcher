package the_fireplace.ias.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.gui.screen.AlertScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Session;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;
import ru.vidtu.ias.Config;
import ru.vidtu.ias.account.Account;
import the_fireplace.ias.IAS;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The GUI where you can log in to, add, edit, and remove accounts.
 *
 * @author The_Fireplace
 * @author VidTu
 */
public class AccountListScreen extends Screen {
    private static long nextSkinUpdate = System.currentTimeMillis();
    private final Screen prev;
    private AccountList list;
    private Button add;
    private Button login;
    private Button loginOffline;
    private Button delete;
    private Button edit;
    private Button reloadSkins;
    private TextFieldWidget search;
    private String state;

    public AccountListScreen(Screen prev) {
        super(new StringTextComponent("In-Game Account Switcher"));
        this.prev = prev;
    }

    @Override
    public void init() {
        list = new AccountList(minecraft, width, height);
        addButton(reloadSkins = new Button(2, 2, 120, 20, I18n.get("ias.listGui.reloadSkins"), btn -> reloadSkins()));
        addButton(search = new TextFieldWidget(this.font, this.width / 2 - 80, 14, 160, 16, search, I18n.get("ias.listGui.search")));
        addButton(add = new Button(this.width / 2 + 4 + 40, this.height - 52, 120, 20, I18n.get("ias.listGui.add"), btn -> add()));
        addButton(login = new Button(this.width / 2 - 154 - 10, this.height - 52, 120, 20, I18n.get("ias.listGui.login"), btn -> login()));
        addButton(loginOffline = new Button(this.width / 2 - 154 - 10, this.height - 28, 110, 20, I18n.get("ias.listGui.loginOffline"), btn -> loginOffline()));
        addButton(edit = new Button(this.width / 2 - 40, this.height - 52, 80, 20, I18n.get("ias.listGui.edit"), btn -> edit()));
        addButton(delete = new Button(this.width / 2 - 50, this.height - 28, 100, 20, I18n.get("ias.listGui.delete"), btn -> delete()));
        addButton(new Button(this.width / 2 + 4 + 50, this.height - 28, 110, 20, I18n.get("gui.cancel"), btn -> minecraft.setScreen(prev)));
        updateButtons();
        search.setSuggestion(I18n.get("ias.listGui.search"));
        search.setResponder(s -> {
            list.updateAccounts(s);
            search.setSuggestion(s.isEmpty() ? I18n.get("ias.listGui.search") : "");
        });
        list.updateAccounts(search.getValue());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        return list.mouseClicked(mx, my, btn) || super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scroll) {
        return list.mouseScrolled(mx, my, scroll) || super.mouseScrolled(mx, my, scroll);
    }

    @Override
    public void tick() {
        search.tick();
        updateButtons();
    }

    @Override
    public void removed() {
        Config.save(minecraft.gameDirectory.toPath());
    }

    @Override
    public void render(int mx, int my, float delta) {
        renderBackground();
        list.render(mx, my, delta);
        super.render(mx, my, delta);
        drawCenteredString(font, this.title.getColoredString(), this.width / 2, 4, -1);
        if (list.getSelected() != null) {
            minecraft.getTextureManager().bind(list.getSelected().skin());
            RenderSystem.color4f(1F, 1F, 1F, 1F);
            boolean slim = list.getSelected().slimSkin();
            RenderSystem.pushMatrix();
            RenderSystem.scaled(4, 4, 4);
            RenderSystem.translated(1, height / 8D - 16D - 4D, 0);
            Screen.blit(4, 0, 8, 8, 8, 8, 64, 64); // Head
            Screen.blit(4, 8, 20, 20, 8, 12, 64, 64); // Body
            Screen.blit(slim ? 1 : 0, 8, 44, 20, slim ? 3 : 4, 12, 64, 64); // Right Arm (Left from our perspective)
            Screen.blit(12, 8, 36, 52, slim ? 3 : 4, 12, 64, 64); // Left Arm (Right from our perspective)
            Screen.blit(4, 20, 4, 20, 4, 12, 64, 64); // Right Leg (Left from our perspective)
            Screen.blit(8, 20, 20, 52, 4, 12, 64, 64); // Left Leg (Right from our perspective)
            if (minecraft.options.getModelParts().contains(PlayerModelPart.HAT))
                Screen.blit(4, 0, 40, 8, 8, 8, 64, 64); // Head (Overlay)
            if (minecraft.options.getModelParts().contains(PlayerModelPart.RIGHT_SLEEVE))
                Screen.blit(slim ? 1 : 0, 8, 44, 36, slim ? 3 : 4, 12, 64, 64); // Right Arm (Overlay)
            if (minecraft.options.getModelParts().contains(PlayerModelPart.LEFT_SLEEVE))
                Screen.blit(12, 8, 52, 52, slim ? 3 : 4, 12, 64, 64); // Left Arm (Overlay)
            if (minecraft.options.getModelParts().contains(PlayerModelPart.RIGHT_PANTS_LEG))
                Screen.blit(4, 20, 4, 36, 4, 12, 64, 64); // Right Leg (Overlay)
            if (minecraft.options.getModelParts().contains(PlayerModelPart.LEFT_PANTS_LEG))
                Screen.blit(8, 20, 4, 52, 4, 12, 64, 64); // Left Leg (Overlay)
            RenderSystem.popMatrix();
        }
        if (state != null) {
            drawCenteredString(font, state, this.width / 2, this.height - 62, 0xFFFF9900);
        }
    }

    private void reloadSkins() {
        if (list.children().isEmpty() || System.currentTimeMillis() <= nextSkinUpdate || state != null) return;
        IAS.SKIN_CACHE.clear();
        list.updateAccounts(search.getValue());
        nextSkinUpdate = System.currentTimeMillis() + 15000L;
    }

    private void login() {
        if (list.getSelected() == null || state != null) return;
        Account acc = list.getSelected().account();
        updateButtons();
        state = "";
        acc.login((s, o) -> state = I18n.get(s, o)).whenComplete((d, t) -> {
            state = null;
            if (t != null) {
                minecraft.execute(() -> minecraft.setScreen(new AlertScreen(() -> minecraft.setScreen(this),
                        new TranslationTextComponent("ias.error").withStyle(TextFormatting.RED),
                        new StringTextComponent(String.valueOf(t)))));
                return;
            }
            minecraft.execute(() -> {
                minecraft.user = new Session(d.name(), UUIDTypeAdapter.fromUUID(d.uuid()), d.accessToken(), d.userType());
            });
        });
    }

    private void loginOffline() {
        if (list.getSelected() == null || state != null) return;
        Account acc = list.getSelected().account();
        minecraft.user = new Session(acc.name(), UUIDTypeAdapter.fromUUID(UUID
                .nameUUIDFromBytes("OfflinePlayer".concat(acc.name()).getBytes(StandardCharsets.UTF_8))),
                "0", "legacy");
    }

    private void add() {
        if (state != null) return;
        minecraft.setScreen(new LoginScreen(this, new TranslationTextComponent("ias.loginGui.add"),
                I18n.get("ias.loginGui.add.button"),
                I18n.get("ias.loginGui.add.button.tooltip"), acc -> {
            Config.accounts.add(acc);
            Config.save(minecraft.gameDirectory.toPath());
            list.updateAccounts(search.getValue());
        }));
    }

    public void edit() {
        if (list.getSelected() == null || state != null) return;
        Account acc = list.getSelected().account();
        minecraft.setScreen(new LoginScreen(this, new TranslationTextComponent("ias.loginGui.edit"),
                I18n.get("ias.loginGui.edit.button"),
                I18n.get("ias.loginGui.edit.button.tooltip"), newAcc -> {
            Config.accounts.set(Config.accounts.indexOf(acc), newAcc);
            Config.save(minecraft.gameDirectory.toPath());
        }));
    }

    public void delete() {
        if (list.getSelected() == null || state != null) return;
        Account acc = list.getSelected().account();
        if (hasShiftDown()) {
            Config.accounts.remove(acc);
            Config.save(minecraft.gameDirectory.toPath());
            updateButtons();
            list.updateAccounts(search.getValue());
            return;
        }
        minecraft.setScreen(new ConfirmScreen(b -> {
            if (b) {
                Config.accounts.remove(acc);
                updateButtons();
                list.updateAccounts(search.getValue());
            }
            minecraft.setScreen(this);
        }, new TranslationTextComponent("ias.deleteGui.title"), new TranslationTextComponent("ias.deleteGui.text", acc.name())));
    }

    private void updateButtons() {
        login.active = list.getSelected() != null && state == null;
        loginOffline.active = list.getSelected() != null;
        add.active = state == null;
        edit.active = list.getSelected() != null && state == null;
        delete.active = list.getSelected() != null && state == null;
        reloadSkins.active = list.getSelected() != null && state == null && System.currentTimeMillis() > nextSkinUpdate;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search.isFocused()) return super.keyPressed(key, scan, mods);
        if (key == GLFW.GLFW_KEY_F5 || key == GLFW.GLFW_KEY_R) {
            reloadSkins();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (Screen.hasShiftDown()) loginOffline();
            else login();
            return true;
        }
        if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) {
            add();
            return true;
        }
        if (key == GLFW.GLFW_KEY_PERIOD || key == GLFW.GLFW_KEY_KP_DIVIDE) {
            edit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) {
            delete();
            return true;
        }
        return list.keyPressed(key, scan, mods) || super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(prev);
    }
}