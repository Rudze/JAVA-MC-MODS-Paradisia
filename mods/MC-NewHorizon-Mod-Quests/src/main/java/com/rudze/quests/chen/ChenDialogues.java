package com.rudze.quests.chen;

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
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.starter.StarterLockHandler;
import com.rudze.quests.tax.TaxOffice;
import com.rudze.worldbuild.Mayor;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Professor Chen's conversations, built in Java (rather than as Cobblemon dialogue JSON) so that
 * each option can run server code directly: unlocking the starter screen, opening the tax chest...
 *
 * <p>The first conversation is the welcome speech, ending with the starter choice and the
 * Pokédex; it replays until the player reaches its last page. After that, Chen offers a menu.
 */
public final class ChenDialogues {

    private static final String SPEAKER = "chen";
    private static final ResourceLocation POKEDEX = ResourceLocation.fromNamespaceAndPath("cobblemon", "pokedex_red");

    private ChenDialogues() {
    }

    public static void open(ServerPlayer player, NPCEntity npc) {
        List<DialoguePage> pages = PlayerProgress.chenIntroDone(player) ? menu(player) : intro(player);
        DialogueManager.startDialogue(player, npc, dialogue(npc, pages));
    }

    private static List<DialoguePage> intro(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        List<DialoguePage> pages = new ArrayList<>(List.of(
                page("welcome", "Ah ! Te voilà enfin, " + name + " ! Bienvenue sur l'île de Paradisia !"),
                page("who", "Je me présente : je suis le Professeur Chen. Les gens d'ici m'appellent le Professeur Pokémon !"),
                page("world", "Ce monde est peuplé de créatures fascinantes appelées Pokémon.",
                        "Certains les adoptent comme compagnons, d'autres les entraînent pour combattre..."),
                page("research", "Moi, j'étudie les Pokémon : c'est le travail de toute une vie !",
                        "Et Paradisia abrite des espèces que l'on ne voit nulle part ailleurs. Il reste tant à découvrir..."),
                page("mayor", "L'île est dirigée par notre maire, Rudzee.",
                        "Si tu veux un terrain pour construire en ville, c'est auprès du maire qu'il faut le demander."),
                page("taxes", "Je m'occupe aussi de collecter les impôts fonciers pour la mairie.",
                        "Viens me voir quand tu posséderas un terrain !")));

        if (StarterLockHandler.hasStarter(player)) {
            pages.add(options(page("partner", "Mais je vois que tu as déjà un partenaire Pokémon !",
                            "Tiens, prends quand même ce Pokédex : il enregistrera chaque Pokémon que tu rencontreras."),
                    option("Merci, Professeur !", d -> finishIntro(player))));
        } else {
            pages.add(options(page("starter", "Mais assez parlé ! Ton aventure Pokémon commence maintenant.",
                            "Tiens, voici ton Pokédex. Et maintenant... choisis le Pokémon qui t'accompagnera !"),
                    option("Choisir mon starter !", d -> {
                        finishIntro(player);
                        nextTick(player, () -> StarterLockHandler.unlockAndOpenStarterScreen(player));
                    })));
        }
        return pages;
    }

    private static void finishIntro(ServerPlayer player) {
        PlayerProgress.setChenIntroDone(player);
        player.getInventory().placeItemBackInInventory(new ItemStack(BuiltInRegistries.ITEM.get(POKEDEX)));
        player.sendSystemMessage(Component.literal("Tu as reçu un Pokédex !").withStyle(ChatFormatting.GREEN));
    }

    private static List<DialoguePage> menu(ServerPlayer player) {
        DialoguePage menu = verticalOptions(page("menu", "Ah, " + player.getGameProfile().getName() + " ! Que puis-je faire pour toi ?"),
                option("Quelle belle journée !", d -> d.setPage(1)),
                option("Payer mes impôts", d -> nextTick(player, () -> TaxOffice.openPayment(player))),
                option("Récupérer les impôts", Mayor::isMayor, d -> nextTick(player, () -> TaxOffice.collect(player))),
                option("Choisir mon starter", p -> !StarterLockHandler.hasStarter(p),
                        d -> nextTick(player, () -> StarterLockHandler.unlockAndOpenStarterScreen(player))),
                option("Au revoir", d -> { }));

        DialoguePage weather = page("weather", "Oui, une journée parfaite pour observer les Pokémon sauvages !",
                "Repasse me voir plus tard : j'aurai peut-être du travail pour toi.");
        return List.of(menu, weather);
    }

    private static Dialogue dialogue(NPCEntity npc, List<DialoguePage> pages) {
        DialogueSpeaker chen = new DialogueSpeaker(text(ProfessorChen.NAME), new ReferenceDialogueFaceProvider(npc.getId(), true), null);
        FunctionDialogueAction close = new FunctionDialogueAction((d, value) -> {
            d.close();
            return Unit.INSTANCE;
        });
        FunctionDialogueAction noop = new FunctionDialogueAction((d, value) -> Unit.INSTANCE);
        return new Dialogue(pages, new Dialogue().getBackground(), close, Map.of(SPEAKER, chen), noop);
    }

    private static DialoguePage page(String id, String... lines) {
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

    private static DialoguePage options(DialoguePage page, DialogueOption... options) {
        page.setInput(new DialogueOptionSetInput(List.of(options), null, false));
        return page;
    }

    private static DialoguePage verticalOptions(DialoguePage page, DialogueOption... options) {
        page.setInput(new DialogueOptionSetInput(List.of(options), null, true));
        return page;
    }

    private static DialogueOption option(String label, Consumer<ActiveDialogue> action) {
        return option(label, p -> true, action);
    }

    /**
     * Options close the dialogue unless their action moved it to another page, so actions that
     * open a screen don't have to remember to.
     */
    private static DialogueOption option(String label, Predicate<ServerPlayer> visible, Consumer<ActiveDialogue> action) {
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

    private static DialogueText text(String line) {
        return new WrappedDialogueText(Component.literal(line));
    }

    /** Lets the dialogue screen close on the client before another screen replaces it. */
    private static void nextTick(ServerPlayer player, Runnable task) {
        player.server.tell(new TickTask(player.server.getTickCount() + 1, task));
    }
}
