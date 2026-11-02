package com.rudze.treefelling.util;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Minimal server-tick-based delayed task queue. Avoids blocking the main thread with
 * long-running loops (e.g. progressive tree felling) by spreading work across ticks.
 * Not thread-safe: only ever touched from the server tick thread.
 */
public final class TickScheduler {

    public static final TickScheduler INSTANCE = new TickScheduler();

    private record ScheduledTask(long dueTick, Runnable task) {
    }

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>(java.util.Comparator.comparingLong(ScheduledTask::dueTick));
    private long currentTick = 0;

    private TickScheduler() {
    }

    public void schedule(int delayTicks, Runnable task) {
        queue.add(new ScheduledTask(currentTick + Math.max(0, delayTicks), task));
    }

    public void onServerTick(ServerTickEvent.Post event) {
        currentTick++;
        if (queue.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().dueTick() <= currentTick) {
            due.add(queue.poll().task());
        }
        for (Runnable task : due) {
            task.run();
        }
    }
}
