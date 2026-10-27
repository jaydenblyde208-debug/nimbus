package dev.nimbus.core.event;

import dev.nimbus.core.util.SafeRunner;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Small exact-type event bus with priorities. A throwing listener never stops the others. */
public final class EventBus {
    public interface Subscription { void cancel(); }

    private record Entry<E extends Event>(int priority, Consumer<E> handler) {}

    private static final Comparator<Entry<?>> ORDER =
        Comparator.comparingInt((Entry<?> e) -> e.priority()).reversed();

    private final Map<Class<? extends Event>, CopyOnWriteArrayList<Entry<?>>> listeners = new ConcurrentHashMap<>();

    public <E extends Event> Subscription subscribe(Class<E> type, Consumer<E> handler) {
        return subscribe(type, Priority.NORMAL, handler);
    }

    public <E extends Event> Subscription subscribe(Class<E> type, int priority, Consumer<E> handler) {
        Entry<E> entry = new Entry<>(priority, handler);
        CopyOnWriteArrayList<Entry<?>> list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.add(entry);
        list.sort(ORDER);
        return () -> list.remove(entry);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> E post(E event) {
        List<Entry<?>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return event;
        for (Entry<?> raw : list) {
            try {
                ((Entry<E>) raw).handler().accept(event);
            } catch (Throwable t) {
                SafeRunner.report("listener for " + event.getClass().getSimpleName(), t);
            }
        }
        return event;
    }
}
