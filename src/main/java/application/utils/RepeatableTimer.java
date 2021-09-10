package application.utils;

import java.util.Timer;
import java.util.TimerTask;

import application.globals.Timers;
import application.root.Executable;

public final class RepeatableTimer {
    private static final int EXECUTION_DELAY_MS = 0;
    private Timer timer = Timers.newTimer(this.getClass().getName());

    public void startExecutingRepeatedly(Executable executableTask, long repeatRateMs) {
        TimerTask timerTask = getTimerTask(executableTask);
        timer = new Timer(this.getClass().getName());
        timer.scheduleAtFixedRate(timerTask, EXECUTION_DELAY_MS, repeatRateMs);
    }

    private TimerTask getTimerTask(Executable executableTask) {
        return new TimerTask() {
            @Override
            public void run() {
                executableTask.execute();
            }
        };
    }

    public void cancel() {
        try {
            timer.cancel();
        } catch (java.lang.IllegalStateException ignored) {
            // don't complain about cancelling already cancelled
        }
    }
}