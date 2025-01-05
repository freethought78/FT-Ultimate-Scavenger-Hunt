package FTUltimateScavengerHunt;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TaskScheduler {

    private static final Queue<ScheduledTask> tasks = new ArrayDeque<>();

    public static void scheduleTask(int delay, Runnable task) {
        tasks.add(new ScheduledTask(delay, task));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // Process tasks
        Queue<ScheduledTask> remainingTasks = new ArrayDeque<>();
        while (!tasks.isEmpty()) {
            ScheduledTask scheduledTask = tasks.poll();
            if (scheduledTask.delay > 0) {
                scheduledTask.delay--;
                remainingTasks.add(scheduledTask);
            } else {
                scheduledTask.task.run();
            }
        }
        tasks.addAll(remainingTasks);
    }

    private static class ScheduledTask {
        int delay;
        Runnable task;

        ScheduledTask(int delay, Runnable task) {
            this.delay = delay;
            this.task = task;
        }
    }
}
