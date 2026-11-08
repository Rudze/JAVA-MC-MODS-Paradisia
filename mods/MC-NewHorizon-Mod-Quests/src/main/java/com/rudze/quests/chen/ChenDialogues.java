package com.rudze.quests.chen;

import com.cobblemon.mod.common.api.dialogue.DialoguePage;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.npc.Dialogues;
import com.rudze.quests.npc.TownNpc;
import com.rudze.quests.starter.StarterLockHandler;
import com.rudze.quests.tax.TaxOffice;
import com.rudze.worldbuild.Mayor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.rudze.quests.npc.Dialogues.nextTick;
import static com.rudze.quests.npc.Dialogues.option;
import static com.rudze.quests.npc.Dialogues.options;
import static com.rudze.quests.npc.Dialogues.page;
import static com.rudze.quests.npc.Dialogues.verticalOptions;

/**
 * Professor Chen's conversations. The first one is the welcome speech, ending with the starter
 * choice and the Pokédex; it replays until the player reaches its last page. After that, Chen
 * offers a menu.
 */
public final class ChenDialogues {

    private static final ResourceLocation POKEDEX = ResourceLocation.fromNamespaceAndPath("cobblemon", "pokedex_red");

    private ChenDialogues() {
    }

    public static void open(ServerPlayer player, NPCEntity npc) {
        List<DialoguePage> pages = PlayerProgress.chenIntroDone(player) ? menu(player) : intro(player);
        Dialogues.start(player, npc, TownNpc.CHEN.name(), pages);
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
}
