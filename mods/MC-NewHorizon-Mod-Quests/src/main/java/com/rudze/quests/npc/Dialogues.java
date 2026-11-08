package com.rudze.quests.npc;

import com.cobblemon.mod.common.api.dialogue.ActiveDialogue;
import com.cobblemon.mod.common.api.dialogue.Dialogue;
import com.cobblemon.mod.common.api.dialogue.DialogueManager;
import com.cobblemon.mod.common.api.dialogue.DialoguePage;
import com.cobblemon.mod.common.api.dialogue.DialogueSpeaker;
import com.cobblemon.mod.common.api.dialogue.DialogueText;
import com.cobblemon.mod.common.api.dialogue.FunctionDialogueAction;
import com.cobblemon.mod.common.api.dialogue.FunctionDialoguePredicate;
import com.cobblemon.mod.common.api.dialogue.ReferenceDialogueFaceProvider;
import com.cobblemon.mod.common.api.dialogue.WrappedDialogueText;
import com.cobblemon.mod.common.api.dialogue.input.DialogueOption;
import com.cobblemon.mod.common.api.dialogue.input.DialogueOptionSetInput;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Builds Cobblemon dialogues in Java (rather than as dialogue JSON) so that each option can run
 * server code directly. Every page is spoken by the NPC the dialogue is opened with.
 */
public final class Dialogues {

    private static final String SPEAKER = "npc";

    private Dialogues() {
    }

    public static void start(ServerPlayer player, NPCEntity npc, String speakerName, List<DialoguePage> pages) {
        DialogueSpeaker speaker = new DialogueSpeaker(text(speakerName), new ReferenceDialogueFaceProvider(npc.getId(), true), null);
        FunctionDialogueAction close = new FunctionDialogueAction((d, value) -> {
            d.close();
            return Unit.INSTANCE;
        });
        FunctionDialogueAction noop = new FunctionDialogueAction((d, value) -> Unit.INSTANCE);
        DialogueManager.startDialogue(player, npc,
                new Dialogue(pages, new Dialogue().getBackground(), close, Map.of(SPEAKER, speaker), noop));
    }

    public static DialoguePage page(String id, String... lines) {
        return page(id, List.of(lines));
    }

    public static DialoguePage page(String id, List<String> lines) {
        DialoguePage page = new DialoguePage();
        page.setId(id);
        page.setSpeaker(SPEAKER);
        List<DialogueText> texts = new ArrayList<>();
        for (String line : lines) {
            texts.add(text(line));
        }
        page.setLines(texts);
        return page;
    }

    public static DialoguePage options(DialoguePage page, DialogueOption... options) {
        page.setInput(new DialogueOptionSetInput(List.of(options), null, false));
        return page;
    }

    public static DialoguePage verticalOptions(DialoguePage page, DialogueOption... options) {
        page.setInput(new DialogueOptionSetInput(List.of(options), null, true));
        return page;
    }

    public static DialogueOption option(String label, Consumer<ActiveDialogue> action) {
        return option(label, p -> true, action);
    }

    /**
     * Options close the dialogue unless their action moved it to another page, so actions that
     * open a screen don't have to remember to.
     */
    public static DialogueOption option(String label, Predicate<ServerPlayer> visible, Consumer<ActiveDialogue> action) {
        FunctionDialogueAction run = new FunctionDialogueAction((d, value) -> {
            DialoguePage before = d.getCurrentPage();
            action.accept(d);
            if (d.getCurrentPage() == before) {
                d.close();
            }
            return Unit.INSTANCE;
        });
        FunctionDialoguePredicate isVisible = new FunctionDialoguePredicate(d -> visible.test(d.getPlayerEntity()));
        FunctionDialoguePredicate always = new FunctionDialoguePredicate(d -> true);
        return new DialogueOption(text(label), label, run, isVisible, always);
    }

    /** Lets the dialogue screen close on the client before another screen replaces it. */
    public static void nextTick(ServerPlayer player, Runnable task) {
        player.server.tell(new TickTask(player.server.getTickCount() + 1, task));
    }

    private static DialogueText text(String line) {
        return new WrappedDialogueText(Component.literal(line));
    }
}
