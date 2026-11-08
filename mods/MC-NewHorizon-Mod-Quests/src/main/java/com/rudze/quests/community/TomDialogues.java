package com.rudze.quests.community;

import com.cobblemon.mod.common.api.dialogue.DialoguePage;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.rudze.quests.community.CommunityQuests.Quest;
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.npc.Dialogues;
import com.rudze.quests.npc.TownNpc;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static com.rudze.quests.npc.Dialogues.option;
import static com.rudze.quests.npc.Dialogues.options;
import static com.rudze.quests.npc.Dialogues.page;
import static com.rudze.quests.npc.Dialogues.verticalOptions;

/**
 * Tom, the deputy mayor, runs the community quests. The first conversation introduces him and the
 * system, then offers to tell the story of the current quest; it replays until the player reaches
 * its last page. After that, Tom offers a menu.
 */
public final class TomDialogues {

    private TomDialogues() {
    }

    public static void open(ServerPlayer player, NPCEntity npc) {
        List<DialoguePage> pages = CommunityQuests.load()
                .map(quests -> PlayerProgress.tomIntroDone(player) ? menu(player, quests) : intro(player, quests))
                .orElseGet(() -> List.of(page("lost", "Hmm... Impossible de remettre la main sur mes notes.",
                        "Repasse un peu plus tard, le temps que je range tout ça !")));
        Dialogues.start(player, npc, TownNpc.TOM.name(), pages);
    }

    private static List<DialoguePage> intro(ServerPlayer player, List<Quest> quests) {
        Quest current = CommunityQuests.current(quests);
        List<DialoguePage> pages = new ArrayList<>(List.of(
                page("hello", "Oh ! Bonjour ! Tu dois être " + player.getGameProfile().getName() + ".",
                        "Ça fait plaisir de voir une nouvelle tête par ici !"),
                page("who", "Je m'appelle Tom, adjoint au maire de Paradisia.",
                        "Pendant que Rudzee dirige l'île, moi je veille à ce que la ville avance.")));
        pages.addAll(rules());

        if (current == null) {
            pages.add(options(page("idle", "Pour l'instant, aucun projet n'est en cours : la mairie prépare le prochain.",
                            "Repasse me voir bientôt, j'aurai sûrement besoin de toi !"),
                    option("À bientôt, Tom !", d -> PlayerProgress.setTomIntroDone(player))));
            return pages;
        }

        List<DialoguePage> questPages = questPages(current);
        pages.add(options(page("teaser", "Et justement, un grand projet vient de commencer...",
                        "Tu veux que je t'en parle ?"),
                option("Raconte-moi !", d -> {
                    PlayerProgress.setTomIntroDone(player);
                    d.setPage(questPages.getFirst());
                }),
                option("Une autre fois", d -> PlayerProgress.setTomIntroDone(player))));
        pages.addAll(questPages);
        return pages;
    }

    /** How community quests work, kept short since it is also replayed from the menu. */
    private static List<DialoguePage> rules() {
        return List.of(
                page("growing", "Paradisia grandit vite, tu sais ! Chaque semaine, de nouveaux dresseurs s'installent.",
                        "Mais une ville, ça ne se construit pas toute seule..."),
                page("community", "C'est pour ça que j'ai lancé les Objectifs Communs !",
                        "De grands projets pour toute l'île, que tous les habitants réalisent ensemble."),
                page("together", "Un seul projet est ouvert à la fois, et tout le monde peut y participer.",
                        "Récolter, construire, explorer... chaque coup de main compte, même le plus petit !"),
                page("validation", "Quand l'objectif est atteint, la mairie le valide et un nouveau projet commence.",
                        "Projet après projet, c'est toute la ville qui prend vie grâce à vous !"));
    }

    private static List<DialoguePage> questPages(Quest quest) {
        List<DialoguePage> pages = new ArrayList<>();
        pages.add(page(quest.id + "_title", "Objectif commun : " + quest.title + " !"));
        for (int i = 0; i < quest.story.size(); i++) {
            pages.add(page(quest.id + "_story_" + i, quest.story.get(i)));
        }
        for (int i = 0; i < quest.objective.size(); i++) {
            pages.add(page(quest.id + "_objective_" + i, quest.objective.get(i)));
        }
        DialoguePage last = pages.getLast();
        options(last, option("Compte sur moi !", d -> { }));
        return pages;
    }

    private static List<DialoguePage> menu(ServerPlayer player, List<Quest> quests) {
        Quest current = CommunityQuests.current(quests);
        List<Quest> completed = CommunityQuests.completed(quests);
        List<DialoguePage> questPages = current != null ? questPages(current)
                : List.of(options(page("idle", "Aucun projet en cours pour le moment : la mairie prépare le prochain.",
                                "Repasse me voir bientôt !"),
                        option("D'accord !", d -> { })));
        List<DialoguePage> donePages = donePages(completed);
        List<DialoguePage> rulePages = new ArrayList<>(rules());
        options(rulePages.getLast(), option("Merci, Tom !", d -> { }));

        DialoguePage menu = verticalOptions(page("menu", "Salut, " + player.getGameProfile().getName() + " ! Qu'est-ce qui t'amène ?"),
                option("Le projet en cours", d -> d.setPage(questPages.getFirst())),
                option("Les projets réalisés", p -> !completed.isEmpty(), d -> d.setPage(donePages.getFirst())),
                option("Rappelle-moi le principe", d -> d.setPage(rulePages.getFirst())),
                option("Au revoir", d -> { }));

        List<DialoguePage> pages = new ArrayList<>();
        pages.add(menu);
        pages.addAll(questPages);
        pages.addAll(donePages);
        pages.addAll(rulePages);
        return pages;
    }

    /** Two achievements per page, so long lists don't overflow the dialogue box. */
    private static List<DialoguePage> donePages(List<Quest> completed) {
        if (completed.isEmpty()) {
            return List.of();
        }
        List<DialoguePage> pages = new ArrayList<>();
        pages.add(page("done", "Regarde tout ce qu'on a déjà accompli ensemble :"));
        for (int i = 0; i < completed.size(); i += 2) {
            List<String> lines = new ArrayList<>();
            for (Quest quest : completed.subList(i, Math.min(i + 2, completed.size()))) {
                lines.add("✔ " + (quest.completedText != null ? quest.completedText : quest.title));
            }
            pages.add(page("done_" + i, lines));
        }
        options(pages.getLast(), option("Bravo à tous !", d -> { }));
        return pages;
    }
}
