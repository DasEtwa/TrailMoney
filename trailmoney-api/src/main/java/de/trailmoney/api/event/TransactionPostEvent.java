package de.trailmoney.api.event;

import de.trailmoney.api.transaction.TransactionResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class TransactionPostEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final TransactionResult result;

    public TransactionPostEvent(TransactionResult result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public TransactionResult result() {
        return result;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
