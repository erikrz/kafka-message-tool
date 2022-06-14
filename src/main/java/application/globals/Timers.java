package application.globals;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

public class Timers {

    private static final Set<Timer> TIMERS = new HashSet<>();

    public static Timer newTimer(String name) {
        final Timer timer = new Timer(name);
        TIMERS.add(timer);
        return timer;
    }

    public static void stop() {
        TIMERS.forEach(Timer::cancel);
    }
}
